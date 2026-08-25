---
description: Append a performance measurement to mansart/BENCH.md (date, hardware/JVM, exact command, raw results, delta vs previous run).
agent: tracker
---
Append an entry to `BENCH.md` at the mansart root for: $ARGUMENTS

Required fields: absolute date, hardware and JVM, the exact command that was run,
the raw numbers, and the delta versus the previous run of the same benchmark.

Never record a figure that was not produced by a run in this session. The
workspace rule is that no performance number may appear in a README or a commit
message without a matching `BENCH.md` entry — so if there is no measurement,
refuse and say what to run.
