"""Revised bench: 119B now configured (131072 ctx). Logs the ACTUAL tool-call
arguments (got vs want) to settle the "4/8 exact is strange" doubt, measures the
119B throughput at 20k/50k/100k, and EJECTS the big model at the end (a final A3B
call plus an idle wait so oMLX drops the 119B from the pool).
"""
import json, glob, time, urllib.request
KEY = "sk-omlx-UvGztq7V1ZU1TuRVbuDk75ep"
URL = "http://127.0.0.1:8000/v1/chat/completions"
HEALTH = "http://127.0.0.1:8000/health"
MODELS = [
    ("Qwen3-Coder-Next", "Qwen3-Coder-Next-mxfp4-mlx",            [20000, 50000, 100000]),
    ("Qwen3.6-A3B",      "Qwen3.6-35B-A3B-MTPLX-Optimized-Speed", [20000, 50000, 100000]),
    ("Mistral-119B",     "Mistral-Small-4-119B-2603-4bit",        [20000, 50000, 100000]),
]
src = []
for f in sorted(glob.glob("/Users/yblazart/projects/perso/vidocq/mansart/**/src/main/java/**/*.java", recursive=True)):
    try: src.append(open(f, encoding="utf-8").read())
    except Exception: pass
BLOB = "\n".join(src)

FIND_TOOL = {"type": "function", "function": {
    "name": "record_finding", "description": "Record exactly one code-review finding.",
    "parameters": {"type": "object", "properties": {
        "file": {"type": "string"}, "line": {"type": "integer"},
        "severity": {"type": "string", "enum": ["low", "medium", "high"]},
        "summary": {"type": "string"}},
        "required": ["file", "line", "severity", "summary"]}}}

def call(model, msgs, tools=None, max_tokens=2048):
    body = dict(model=model, messages=msgs, max_tokens=max_tokens, stream=False, temperature=0.6, top_p=0.95)
    if tools: body["tools"] = tools
    t = time.time()
    try:
        r = urllib.request.Request(URL, data=json.dumps(body).encode(),
                                   headers={"Authorization": "Bearer " + KEY, "Content-Type": "application/json"})
        return json.load(urllib.request.urlopen(r, timeout=1200)), time.time() - t, None
    except Exception as e:
        return None, time.time() - t, repr(e)[:90]

def mem():
    try:
        r = urllib.request.Request(HEALTH); r.add_header("Authorization", "Bearer " + KEY)
        p = json.load(urllib.request.urlopen(r, timeout=6))["engine_pool"]
        return "loaded=%d mem=%.0fGB" % (p["loaded_count"], p["current_model_memory"] / 1e9)
    except Exception:
        return "?"

def throughput(tag, model, ctxs):
    print("\n[%s] throughput (prefill cold, decode from 2 warm calls)" % tag, flush=True)
    print("  %8s %10s %13s %13s" % ("ctx", "prompt_tok", "prefill tok/s", "decode tok/s"), flush=True)
    for ctx in ctxs:
        prompt = BLOB[: int(ctx * 4.1)] + "\n\nName the first file only."
        m = [{"role": "user", "content": prompt}]
        d0, w0, e0 = call(model, m, max_tokens=1)
        if e0 or not d0 or "usage" not in d0:
            print("  %8d  ---  %s" % (ctx, e0 or "no usage (OOM/err)"), flush=True); continue
        pt = d0["usage"]["prompt_tokens"]
        da, wa, ea = call(model, m, max_tokens=8)
        db, wb, eb = call(model, m, max_tokens=208)
        if ea or eb or not db or "usage" not in db:
            print("  %8d %10d %13.0f  decode err (%s)" % (ctx, pt, pt/max(w0,.01), ea or eb or "no usage"), flush=True); continue
        dec = 200 / (wb - wa) if wb > wa else float("nan")
        print("  %8d %10d %13.0f %13.1f   [%s]" % (ctx, pt, pt/max(w0,.01), dec, mem()), flush=True)

PROBES = [
    ("plain",    "Call record_finding once for file 'A.java', line 1, severity low, summary 'ok'."),
    ("nested",   "Call record_finding for the file 'io/vidocq/B.java' at line 42, severity high, summary 'null deref'."),
    ("infer",    "The file src/main/java/C.java has a synchronized block wrapping a JDBC call on line 88. Record it as a high severity finding."),
    ("enum",     "Record a finding: file 'D.java', line 7, the severity must be the middle one of the allowed values, summary 'medium issue'."),
    ("quotes",   "Record a finding for file 'E\"F.java' line 3 severity low summary 'quote in \"name\"'."),
    ("int",      "Record a finding on file 'G.java' at the line numbered one thousand and twenty four, severity low, summary 'deep'."),
    ("terse",    "record_finding H.java 9 high 'leak'"),
    ("distract", "Explain briefly what a persistence context is, then record a finding for 'I.java' line 5 severity medium summary 'see above'."),
]
EXPECT = {"plain": ("A.java", 1, "low"), "nested": ("io/vidocq/B.java", 42, "high"),
          "infer": (None, 88, "high"), "enum": ("D.java", 7, "medium"),
          "quotes": (None, 3, "low"), "int": ("G.java", 1024, "low"),
          "terse": ("H.java", 9, "high"), "distract": ("I.java", 5, "medium")}

def tools_battery(tag, model, ctx_tokens=20000):
    print("\n[%s] B/C. tool fidelity — LOGGING actual args (got vs want)" % tag, flush=True)
    chunk = BLOB[: int(ctx_tokens * 4.1)]
    ok = exact = 0
    for name, ask in PROBES:
        msgs = [{"role": "system", "content": "You are a Java reviewer. Use the record_finding tool."},
                {"role": "user", "content": "Codebase:\n\n" + chunk + "\n\n" + ask}]
        d, el, err = call(model, msgs, tools=[FIND_TOOL], max_tokens=2048)
        if err or not d:
            print("  %-9s ERROR %s" % (name, err), flush=True); continue
        ch = d["choices"][0]; tc = ch["message"].get("tool_calls") or []
        ef, eln, esev = EXPECT[name]
        if len(tc) == 1:
            try:
                a = json.loads(tc[0]["function"]["arguments"])
            except Exception:
                a = {}
            valid = ({"file", "line", "severity", "summary"} <= set(a) and isinstance(a.get("line"), int)
                     and a.get("severity") in ("low", "medium", "high"))
            right = valid and a.get("line") == eln and a.get("severity") == esev and (ef is None or a.get("file") == ef)
            ok += valid; exact += right
            print("  %-9s valid=%s exact=%s | got file=%r line=%r sev=%r | want file=%r line=%r sev=%r"
                  % (name, valid, right, a.get("file"), a.get("line"), a.get("severity"), ef, eln, esev), flush=True)
        else:
            print("  %-9s NO/MULTI tool_call (%d) finish=%s out=%d" % (name, len(tc), ch["finish_reason"], d["usage"]["completion_tokens"]), flush=True)
    print("  => fidelity %d/8 | exact args %d/8" % (ok, exact), flush=True)

API_Q = [
    ("find-nonentity", "Which exception does EntityManager.find throw if the first argument is not an entity type? Simple name only.", ["IllegalArgumentException"]),
    ("getReference",   "Which exception does the proxy from EntityManager.getReference throw when the row does not exist and state is first accessed? Simple name only.", ["EntityNotFoundException"]),
    ("flush-no-tx",    "Which exception does EntityManager.flush throw when there is no transaction? Simple name only.", ["TransactionRequiredException"]),
    ("single-none",    "Which exception does Query.getSingleResult throw when there is no result? Simple name only.", ["NoResultException"]),
    ("single-many",    "Which exception does Query.getSingleResult throw when there is more than one result? Simple name only.", ["NonUniqueResultException"]),
    ("cascade",        "List every constant of jakarta.persistence.CascadeType. Names only, comma separated.", ["PERSIST", "MERGE", "REMOVE", "REFRESH", "DETACH", "ALL"]),
    ("fetch",          "List every constant of jakarta.persistence.FetchType. Names only.", ["LAZY", "EAGER"]),
    ("lock",           "Name the LockModeType constant that forces a version increment with optimistic locking. Constant name only.", ["OPTIMISTIC_FORCE_INCREMENT"]),
]

def api_battery(tag, model):
    print("\n[%s] E. jakarta.persistence API correctness" % tag, flush=True)
    score = 0
    for name, q, expected in API_Q:
        d, el, err = call(model, [{"role": "system", "content": "Answer as briefly as possible."},
                                  {"role": "user", "content": q}], max_tokens=1024)
        if err or not d:
            print("  %-14s ERROR %s" % (name, err), flush=True); continue
        txt = (d["choices"][0]["message"].get("content") or "").upper()
        hit = all(e.upper() in txt for e in expected)
        score += hit
        print("  %-14s %s" % (name, "OK  " if hit else "MISS"), flush=True)
    print("  => API correctness %d/8" % score, flush=True)

for tag, model, ctxs in MODELS:
    print("\n" + "=" * 74, flush=True)
    print("MODEL %s  (%s)" % (tag, model), flush=True)
    d, el, err = call(model, [{"role": "user", "content": "hi"}], max_tokens=8)
    if err:
        print("  UNAVAILABLE: %s" % err, flush=True); continue
    print("  cold load + first call: %.1fs  [%s]" % (el, mem()), flush=True)
    throughput(tag, model, ctxs)
    tools_battery(tag, model)
    api_battery(tag, model)

# ---- EJECT: warm the small default model, then idle so oMLX drops the 119B ----
print("\n=== EJECT big model: pinging A3B (default) then waiting for pool to drop ===", flush=True)
call("Qwen3.6-35B-A3B-MTPLX-Optimized-Speed", [{"role": "user", "content": "ok"}], max_tokens=4)
print("  after A3B ping: [%s]" % mem(), flush=True)
for _ in range(6):
    time.sleep(20)
    print("  idle... [%s]" % mem(), flush=True)
print("\nBENCH-V2 DONE", flush=True)
