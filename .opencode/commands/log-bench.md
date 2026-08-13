---
description: Append a benchmark run entry to BENCH.md (Vidocq convention)
agent: build
---
Append a benchmark entry to `BENCH.md` at the mansart root (create the file with a
short header if missing). Benchmark context: $ARGUMENTS

Entry format: `## BENCH-YYYYMMDD-NN — <what was measured>` with bullet lines for
Date, Commit (`git rev-parse --short HEAD`), JVM (`java -version`), Hardware (CPU /
cores / RAM via `system_profiler SPHardwareDataType`), OS (`uname -a` condensed),
the exact reproducible command in a bash block, the raw results summary in a code
block, the delta vs the most recent comparable entry (percentage, or "premier run"),
and Notes. No perf number may appear in a README or commit message without a
corresponding BENCH.md entry. Do not commit; report the file path and the new id.
