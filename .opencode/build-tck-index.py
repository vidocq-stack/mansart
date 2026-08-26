#!/usr/bin/env python3
"""Turn the extracted TCK + spec + common sources into one heading-sectioned
markdown file, so `ctx_index` chunks it cleanly (one section per Java file) and
`ctx_search` can retrieve the few relevant files instead of unzipping jars.

Run once per checkout (regenerate if the TCK version changes):
    python3 .opencode/build-tck-index.py
Then, once per OpenCode session (nothing enters context — path form):
    ctx_index(path=".tck-ref/tck-index.md", source="JPA32-TCK")
"""
import glob, os, sys
ROOT = os.path.join(os.path.dirname(__file__), "..", ".tck-ref")
OUT = os.path.join(ROOT, "tck-index.md")
parts = ["# Jakarta Persistence 3.2 — TCK tests, spec API, TCK common\n"]
n = 0
for f in sorted(glob.glob(os.path.join(ROOT, "**", "*.java"), recursive=True)):
    rel = os.path.relpath(f, ROOT)
    try:
        body = open(f, encoding="utf-8").read()
    except Exception:
        continue
    parts.append(f"## {rel}\n\n```java\n{body}\n```\n")
    n += 1
md = "\n".join(parts)
open(OUT, "w", encoding="utf-8").write(md)
print(f"{n} files -> {OUT} ({len(md)//1024} KB, {md.count(chr(10)+'## ')} sections)")
