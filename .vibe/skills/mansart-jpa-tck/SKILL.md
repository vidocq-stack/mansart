---
name: mansart-jpa-tck
description: Jakarta Persistence 3.2 TCK scale, out-of-reactor runner, how to read surefire instead of the console, and the official-DDL schema trap. Load before touching the TCK runner or interpreting a TCK number.
user-invocable: false
---

# The Jakarta Persistence 3.2 TCK

`jakarta.tck:persistence-tck-spec-tests:3.2.1` — pin this exact version; the
3.2.2 and 4.0.0 SNAPSHOTs move. Already installed in the local M2, alongside
`persistence-tck-dist`, `persistence-tck-common`, `dbprocedures` and
`sigtest-maven-plugin`.

**269 client classes, ~1 745 test methods.** That scale is deliberate context
for decomposition: `tck-runner` on one named client class is roughly one
card's worth of work.

## Layout

`mansart-jakarta-persistence/mansart-persistence-tck` is **out of the
reactor** — standalone POM, `modelVersion 4.0.0`, no `<parent>` — exactly
like `mansart-data-tck` and `mansart-transactions-tck`. Runner script
`run-official-tck-persistence-3.2.sh`, profiles `tck-run`, `tck-pg`,
`tck-sig`, mirroring the Data runner. Never run it from the mansart root
reactor — always from its own directory or via the script.

## Two rules that decide whether a number means anything

- **The schema comes from the TCK's own DDL**
  (`persistence-tck/sql/<db>/…`), not from anything mansart generates.
  `setup*Data failed` is the single largest error source and it belongs to
  the harness, not to the provider.
- **Only PASS counts.** ERROR → FAIL is not progress. Stubs that quieten a
  test are forbidden and `auditor` rejects them. An honest 0/1745 baseline
  is worth more than an invented 40. (The previous local-model attempt on
  `ybl/jpa-opencode` reached 83% of its planned cards while the official TCK
  provider was never actually wired — 991 run / 989 errors, unchanged from
  baseline. Wiring the real harness is not optional busywork; it is the only
  thing that makes any later number trustworthy.)

## Reading results without burning the window

Never read the scanner/surefire console output raw — grep the XML reports
under `target/surefire-reports/` for the summary line, or `wc -l` the
`<failure>`/`<error>` tags. A full TCK console run is tens of thousands of
lines; the number you need is a handful of integers.

`tck-runner`'s job is exactly this: run the script, extract
`pass/fail/error/skipped` from surefire, and report those four integers —
nothing else re-enters the caller's context.
