"""Corrected throughput: prefill (cold gen=1) + decode isolated from TWO WARM calls
(gen=8 then gen=208, both after a warm-up so the prompt prefix is cached and the
prefill cancels out). The earlier bench compared a cold call to a warm one and got
nan. Also probes the 119B's real context ceiling (it 400s past its default window).
"""
import json, glob, time, urllib.request
KEY = "sk-omlx-UvGztq7V1ZU1TuRVbuDk75ep"
URL = "http://127.0.0.1:8000/v1/chat/completions"
MODELS = [
    ("Qwen3.6-A3B",      "Qwen3.6-35B-A3B-MTPLX-Optimized-Speed", [20000, 50000, 100000]),
    ("Qwen3-Coder-Next", "Qwen3-Coder-Next-mxfp4-mlx",            [20000, 50000, 100000]),
    ("Mistral-119B",     "Mistral-Small-4-119B-2603-4bit",        [2000, 4000, 8000, 16000, 20000, 50000]),
]
src = []
for f in sorted(glob.glob("/Users/yblazart/projects/perso/vidocq/mansart/**/src/main/java/**/*.java", recursive=True)):
    try: src.append(open(f, encoding="utf-8").read())
    except Exception: pass
BLOB = "\n".join(src)

def call(model, prompt, gen):
    body = {"model": model, "messages": [{"role": "user", "content": prompt}],
            "max_tokens": gen, "temperature": 0.6, "top_p": 0.95, "stream": False}
    t = time.time()
    try:
        r = urllib.request.Request(URL, data=json.dumps(body).encode(),
                                   headers={"Authorization": "Bearer " + KEY, "Content-Type": "application/json"})
        d = json.load(urllib.request.urlopen(r, timeout=1200))
        return d, time.time() - t, None
    except Exception as e:
        return None, time.time() - t, repr(e)[:90]

for tag, model, ctxs in MODELS:
    print("\n=== %s (%s) ===" % (tag, model), flush=True)
    print("  %8s %10s %13s %13s" % ("ctx", "prompt_tok", "prefill tok/s", "decode tok/s"), flush=True)
    for ctx in ctxs:
        prompt = BLOB[: int(ctx * 4.1)] + "\n\nDescribe the first file in one sentence."
        d0, w0, e0 = call(model, prompt, 1)          # cold: prefill
        if e0:
            print("  %8d  ---  400/err: %s" % (ctx, e0), flush=True); continue
        pt = d0["usage"]["prompt_tokens"]
        da, wa, ea = call(model, prompt, 8)          # warm: prefill cached + 8 decode
        db, wb, eb = call(model, prompt, 208)        # warm: prefill cached + 208 decode
        if ea or eb:
            print("  %8d  prefill=%.0f  decode err" % (ctx, pt / max(w0, .01)), flush=True); continue
        dec = 200 / (wb - wa) if wb > wa else float("nan")
        print("  %8d %10d %13.0f %13.1f" % (ctx, pt, pt / max(w0, .01), dec), flush=True)
print("\nDECODEFIX DONE", flush=True)
