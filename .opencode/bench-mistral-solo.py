"""Mistral-119B alone (user restarted oMLX and loaded only it). Only ever calls the
119B — never the A3B/Coder, to avoid re-creating co-residence. Decode uses a UNIQUE
prompt prefix per context so the prefill is a genuine cold read and oMLX cannot serve a
cached generation (that polluted the v2 run). Watches memory at each step; 100k may OOM
with only ~200 MB free — errors are caught, not fatal.
"""
import json, glob, time, urllib.request
KEY = "sk-omlx-UvGztq7V1ZU1TuRVbuDk75ep"
URL = "http://127.0.0.1:8000/v1/chat/completions"
M = "Mistral-Small-4-119B-2603-4bit"
src = []
for f in sorted(glob.glob("/Users/yblazart/projects/perso/vidocq/mansart/**/src/main/java/**/*.java", recursive=True)):
    try: src.append(open(f, encoding="utf-8").read())
    except Exception: pass
BLOB = "\n".join(src)

def call(msgs, tools=None, gen=2048):
    body = dict(model=M, messages=msgs, max_tokens=gen, stream=False, temperature=0.6, top_p=0.95)
    if tools: body["tools"] = tools
    t = time.time()
    try:
        r = urllib.request.Request(URL, data=json.dumps(body).encode(),
                                   headers={"Authorization": "Bearer " + KEY, "Content-Type": "application/json"})
        return json.load(urllib.request.urlopen(r, timeout=1800)), time.time() - t, None
    except Exception as e:
        return None, time.time() - t, repr(e)[:90]

def mem():
    try:
        r = urllib.request.Request("http://127.0.0.1:8000/health"); r.add_header("Authorization", "Bearer " + KEY)
        p = json.load(urllib.request.urlopen(r, timeout=6))["engine_pool"]
        import subprocess
        pm = subprocess.run(["top", "-l", "1", "-n", "0"], capture_output=True, text=True).stdout
        line = [l for l in pm.splitlines() if "PhysMem" in l]
        return "loaded=%d modelmem=%.0fGB | %s" % (p["loaded_count"], p["current_model_memory"]/1e9, (line[0] if line else "").strip())
    except Exception:
        return "?"

print("MODEL Mistral-119B alone  [%s]" % mem(), flush=True)
print("\nA. prefill (cold, unique prompt) + decode (two warm calls, unique prefix)", flush=True)
print("  %8s %10s %13s %13s" % ("ctx", "prompt_tok", "prefill tok/s", "decode tok/s"), flush=True)
for ctx in (20000, 50000, 100000):
    uniq = "[bench run ctx=%d stamp=%d]\n\n" % (ctx, int(time.time() * 1000))
    prompt = uniq + BLOB[: int(ctx * 4.1)] + "\n\nName the first file only."
    m = [{"role": "user", "content": prompt}]
    d0, w0, e0 = call(m, gen=1)
    if e0 or not d0 or "usage" not in d0:
        print("  %8d  ---  %s   [%s]" % (ctx, e0 or "OOM/no-usage", mem()), flush=True); continue
    pt = d0["usage"]["prompt_tokens"]
    da, wa, ea = call(m, gen=8)
    db, wb, eb = call(m, gen=208)
    if ea or eb or not db or "usage" not in db:
        print("  %8d %10d %13.0f  decode-err %s   [%s]" % (ctx, pt, pt/max(w0,.01), ea or eb, mem()), flush=True); continue
    dec = 200 / (wb - wa) if wb > wa else float("nan")
    print("  %8d %10d %13.0f %13.1f   [%s]" % (ctx, pt, pt/max(w0,.01), dec, mem()), flush=True)

# B/C tool fidelity with arg logging, at 20k (smallest, safest)
PROBES = [
    ("plain",    "Call record_finding once for file 'A.java', line 1, severity low, summary 'ok'.", ("A.java",1,"low")),
    ("nested",   "Call record_finding for the file 'io/vidocq/B.java' at line 42, severity high, summary 'null deref'.", ("io/vidocq/B.java",42,"high")),
    ("infer",    "The file src/main/java/C.java has a synchronized block wrapping a JDBC call on line 88. Record it as a high severity finding.", (None,88,"high")),
    ("enum",     "Record a finding: file 'D.java', line 7, the severity must be the middle one of the allowed values, summary 'medium issue'.", ("D.java",7,"medium")),
    ("quotes",   "Record a finding for file 'E\"F.java' line 3 severity low summary 'quote in \"name\"'.", (None,3,"low")),
    ("int",      "Record a finding on file 'G.java' at the line numbered one thousand and twenty four, severity low, summary 'deep'.", ("G.java",1024,"low")),
    ("terse",    "record_finding H.java 9 high 'leak'", ("H.java",9,"high")),
    ("distract", "Explain briefly what a persistence context is, then record a finding for 'I.java' line 5 severity medium summary 'see above'.", ("I.java",5,"medium")),
]
FIND_TOOL = {"type":"function","function":{"name":"record_finding","description":"Record exactly one code-review finding.",
    "parameters":{"type":"object","properties":{"file":{"type":"string"},"line":{"type":"integer"},
    "severity":{"type":"string","enum":["low","medium","high"]},"summary":{"type":"string"}},
    "required":["file","line","severity","summary"]}}}
print("\nB/C. tool fidelity at ctx~20k — logging got vs want", flush=True)
chunk = BLOB[: int(20000 * 4.1)]
ok = exact = 0
for name, ask, (ef, eln, esev) in PROBES:
    d, el, err = call([{"role":"system","content":"You are a Java reviewer. Use the record_finding tool."},
                       {"role":"user","content":"Codebase:\n\n"+chunk+"\n\n"+ask}], tools=[FIND_TOOL], gen=2048)
    if err or not d:
        print("  %-9s ERROR %s" % (name, err), flush=True); continue
    ch = d["choices"][0]; tc = ch["message"].get("tool_calls") or []
    if len(tc) == 1:
        try: a = json.loads(tc[0]["function"]["arguments"])
        except Exception: a = {}
        valid = ({"file","line","severity","summary"} <= set(a) and isinstance(a.get("line"),int) and a.get("severity") in ("low","medium","high"))
        right = valid and a.get("line")==eln and a.get("severity")==esev and (ef is None or a.get("file")==ef)
        ok += valid; exact += right
        print("  %-9s valid=%s exact=%s | got file=%r line=%r sev=%r | want file=%r line=%r sev=%r"
              % (name, valid, right, a.get("file"), a.get("line"), a.get("severity"), ef, eln, esev), flush=True)
    else:
        print("  %-9s NO/MULTI call (%d) finish=%s" % (name, len(tc), ch["finish_reason"]), flush=True)
print("  => fidelity %d/8 | exact args %d/8" % (ok, exact), flush=True)

API_Q = [
    ("find-nonentity","Which exception does EntityManager.find throw if the first argument is not an entity type? Simple name only.",["IllegalArgumentException"]),
    ("getReference","Which exception does the proxy from EntityManager.getReference throw when the row does not exist and state is first accessed? Simple name only.",["EntityNotFoundException"]),
    ("flush-no-tx","Which exception does EntityManager.flush throw when there is no transaction? Simple name only.",["TransactionRequiredException"]),
    ("single-none","Which exception does Query.getSingleResult throw when there is no result? Simple name only.",["NoResultException"]),
    ("single-many","Which exception does Query.getSingleResult throw when there is more than one result? Simple name only.",["NonUniqueResultException"]),
    ("cascade","List every constant of jakarta.persistence.CascadeType. Names only, comma separated.",["PERSIST","MERGE","REMOVE","REFRESH","DETACH","ALL"]),
    ("fetch","List every constant of jakarta.persistence.FetchType. Names only.",["LAZY","EAGER"]),
    ("lock","Name the LockModeType constant that forces a version increment with optimistic locking. Constant name only.",["OPTIMISTIC_FORCE_INCREMENT"]),
]
print("\nE. jakarta.persistence API correctness", flush=True)
score = 0
for name, q, expected in API_Q:
    d, el, err = call([{"role":"system","content":"Answer as briefly as possible."},{"role":"user","content":q}], gen=1024)
    if err or not d: print("  %-14s ERROR %s" % (name, err), flush=True); continue
    txt = (d["choices"][0]["message"].get("content") or "").upper()
    hit = all(e.upper() in txt for e in expected); score += hit
    print("  %-14s %s" % (name, "OK  " if hit else "MISS"), flush=True)
print("  => API correctness %d/8" % score, flush=True)
print("\nMISTRAL-SOLO DONE  [%s]" % mem(), flush=True)
