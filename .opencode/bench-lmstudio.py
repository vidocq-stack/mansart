"""Quality battery (B/C tool fidelity, E API correctness, F multi-turn chain)
run against LM Studio on BigMontreuil, same probes as bench-models.py so the
scores are directly comparable to the oMLX MTPLX campaign of 2026-08-26.

Model: unsloth/qwen3.6-35b-a3b (IQ4_NL), served at 192.168.2.2:1234.
"""
import json, glob, time, urllib.request, os

KEY = "lm-studio"
URL = "http://192.168.2.2:1234/v1/chat/completions"
# override with BENCH_TAG / BENCH_MODEL env vars to bench any loaded LM Studio model
MODELS = [(os.environ.get("BENCH_TAG", "IQ4_NL@BigMontreuil"),
           os.environ.get("BENCH_MODEL", "unsloth/qwen3.6-35b-a3b"))]
SAMP = dict(temperature=0.6, top_p=0.95)

src = []
for f in sorted(glob.glob("/Users/yblazart/projects/perso/vidocq/mansart/**/src/main/java/**/*.java",
                          recursive=True)):
    try:
        src.append("// ==== %s ====\n%s" % (f, open(f, encoding="utf-8").read()))
    except Exception:
        pass
BLOB = "\n".join(src)

FIND_TOOL = {"type": "function", "function": {
    "name": "record_finding", "description": "Record exactly one code-review finding.",
    "parameters": {"type": "object", "properties": {
        "file": {"type": "string"}, "line": {"type": "integer"},
        "severity": {"type": "string", "enum": ["low", "medium", "high"]},
        "summary": {"type": "string"}},
        "required": ["file", "line", "severity", "summary"]}}}


def call(model, msgs, tools=None, max_tokens=2048):
    body = dict(model=model, messages=msgs, max_tokens=max_tokens, stream=False, **SAMP)
    if tools:
        body["tools"] = tools
    t = time.time()
    try:
        r = urllib.request.Request(URL, data=json.dumps(body).encode(),
                                   headers={"Authorization": "Bearer " + KEY,
                                            "Content-Type": "application/json"})
        return json.load(urllib.request.urlopen(r, timeout=2400)), time.time() - t, None
    except Exception as e:
        return None, time.time() - t, repr(e)[:140]


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
    print("\n[%s] B/C. tool fidelity + output economy (ctx~%d)" % (tag, ctx_tokens), flush=True)
    chunk = BLOB[: int(ctx_tokens * 4.1)]
    ok = exact = 0
    outs = []
    t0 = time.time()
    for name, ask in PROBES:
        msgs = [{"role": "system", "content": "You are a Java reviewer. Use the record_finding tool."},
                {"role": "user", "content": "Codebase:\n\n" + chunk + "\n\n" + ask}]
        d, el, err = call(model, msgs, tools=[FIND_TOOL], max_tokens=2048)
        if err:
            print("  %-9s ERROR %s" % (name, err), flush=True); continue
        ch = d["choices"][0]
        tc = ch["message"].get("tool_calls") or []
        valid = right = False
        if len(tc) == 1:
            try:
                a = json.loads(tc[0]["function"]["arguments"])
                valid = ({"file", "line", "severity", "summary"} <= set(a)
                         and isinstance(a.get("line"), int)
                         and a.get("severity") in ("low", "medium", "high"))
                ef, eln, esev = EXPECT[name]
                right = valid and a.get("line") == eln and a.get("severity") == esev \
                        and (ef is None or a.get("file") == ef)
            except Exception:
                pass
        ok += valid; exact += right
        co = d["usage"]["completion_tokens"]
        outs.append(co)
        print("  %-9s valid=%-5s exact=%-5s out=%4d wall=%5.1fs finish=%s"
              % (name, valid, right, co, el, ch["finish_reason"]), flush=True)
    n = len(PROBES)
    outs.sort()
    print("  => fidelity %d/%d | exact args %d/%d | median out %d tok | total out %d tok | wall %.1fs"
          % (ok, n, exact, n, outs[len(outs) // 2] if outs else 0, sum(outs), time.time() - t0), flush=True)


API_Q = [
    ("find-nonentity", "Which exception does EntityManager.find throw if the first argument is not an entity type? Answer with the exception simple name only.", ["IllegalArgumentException"]),
    ("getReference",   "Which exception does the proxy returned by EntityManager.getReference throw when the row does not exist and the state is first accessed? Simple name only.", ["EntityNotFoundException"]),
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
    t0 = time.time()
    for name, q, expected in API_Q:
        d, el, err = call(model, [{"role": "system", "content": "Answer as briefly as possible."},
                                  {"role": "user", "content": q}], max_tokens=1024)
        if err:
            print("  %-14s ERROR %s" % (name, err), flush=True); continue
        txt = (d["choices"][0]["message"].get("content") or "").upper()
        hit = all(e.upper() in txt for e in expected)
        score += hit
        print("  %-14s %s  out=%4d wall=%5.1fs" % (name, "OK " if hit else "MISS",
              d["usage"]["completion_tokens"], el), flush=True)
    print("  => API correctness %d/%d | wall %.1fs" % (score, len(API_Q), time.time() - t0), flush=True)


CHAIN_TOOLS = [
    {"type": "function", "function": {"name": "list_files",
        "description": "List the Java files of a module.",
        "parameters": {"type": "object", "properties": {"module": {"type": "string"}},
                       "required": ["module"]}}},
    {"type": "function", "function": {"name": "read_file",
        "description": "Read a file and return its numbered lines.",
        "parameters": {"type": "object", "properties": {"path": {"type": "string"}},
                       "required": ["path"]}}},
    FIND_TOOL,
]
FAKE_FILES = ["core/Flusher.java", "core/EntityKey.java", "core/Session.java"]
FAKE_BODY = ("1: package core;\n2: class Flusher {\n3:   void flush() {\n"
             "4:     synchronized (this) {\n5:       stmt.executeUpdate();\n"
             "6:     }\n7:   }\n8: }\n")


def chain_battery(tag, model):
    print("\n[%s] F. multi-turn tool chain" % tag, flush=True)
    msgs = [{"role": "system", "content":
             "You are a Java reviewer. Work step by step using the tools. "
             "Never describe a tool call in prose: emit it."},
            {"role": "user", "content":
             "In module 'core', find the file whose flush method pins a virtual thread. "
             "Record, as a high severity finding, the line number of the BLOCKING JDBC "
             "CALL itself (not the line of the synchronized keyword). "
             "Use list_files, then read_file, then record_finding."}]
    steps, t0 = [], time.time()
    for turn in range(6):
        d, el, err = call(model, msgs, tools=CHAIN_TOOLS, max_tokens=2048)
        if err:
            print("  turn %d ERROR %s" % (turn, err), flush=True); break
        m = d["choices"][0]["message"]
        tc = m.get("tool_calls") or []
        if not tc:
            steps.append("TEXT"); break
        msgs.append({"role": "assistant", "content": m.get("content") or "", "tool_calls": tc})
        for c in tc:
            fn = c["function"]["name"]
            try:
                args = json.loads(c["function"]["arguments"])
            except Exception:
                args = {}
            steps.append(fn)
            if fn == "list_files":
                out = json.dumps(FAKE_FILES)
            elif fn == "read_file":
                out = FAKE_BODY if "Flusher" in str(args.get("path", "")) else "1: // empty\n"
            else:
                out = "recorded"
                msgs.append({"role": "tool", "tool_call_id": c["id"], "content": out})
                good = (args.get("line") == 5 and args.get("severity") == "high")
                print("  chain=%s | final args line=%s sev=%s -> %s | %d turns, %.1fs"
                      % ("->".join(steps), args.get("line"), args.get("severity"),
                         "CORRECT" if good else "WRONG", turn + 1, time.time() - t0), flush=True)
                return
            msgs.append({"role": "tool", "tool_call_id": c["id"], "content": out})
    print("  chain=%s | INCOMPLETE after %.1fs" % ("->".join(steps) or "none", time.time() - t0), flush=True)


for tag, model in MODELS:
    print("\n" + "=" * 74, flush=True)
    print("MODEL %s  (%s)  BLOB=%d chars" % (tag, model, len(BLOB)), flush=True)
    d, el, err = call(model, [{"role": "user", "content": "hi"}], max_tokens=8)
    if err:
        print("  UNAVAILABLE: %s" % err, flush=True); continue
    print("  first call: %.1fs" % el, flush=True)
    tools_battery(tag, model)
    api_battery(tag, model)
    chain_battery(tag, model)

print("\nBENCH-LMSTUDIO DONE", flush=True)
