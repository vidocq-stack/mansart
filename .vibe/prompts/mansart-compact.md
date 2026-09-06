CRITICAL: Respond with text only. Do NOT call any tools. Any tool call will be rejected.

You are compacting a Jakarta Persistence 3.2 implementation session (project:
mansart, Java 25, Maven, strict Java modules, zero external runtime
dependencies). Produce a handoff another agent can resume from without
re-reading anything.

Wrap the ENTIRE summary in <summary></summary> tags and output nothing outside
them. Fill every section; write "none" rather than omitting one.

<summary>
CARD: <the single card id from TASKS.md being worked, and its one-line goal>

CONTRACT DECIDED: <the interface/behaviour that was settled this session, and
the spec clause or docs/spec-notes/ file it came from. If nothing was settled,
say so — that is important.>

TCK BASELINE: <last measured PASS/total, and the exact command that produced
it. If not measured this session, write "not measured this session" and give
the last known figure. NEVER state a number that was not measured.>

FILES: <one line per file touched: path — created|modified — one-clause state.
No diffs. Include a snippet only if the next agent cannot proceed without the
exact text.>

TESTS: <failing test FQCN#method — expected vs actual — suspected cause, one
line each. Green suites get a single line: "N/N green in <module>".>

NEXT STEP: <one concrete action, small enough to verify in a single turn>

BLOCKED ON: <what is genuinely unresolved, or "nothing">
</summary>

Rules:
- Keep spec reasoning OUT. It belongs in docs/spec-notes/*.md, which survives
  compaction on disk; restating it here just pays for it twice.
- Do not carry over file contents, build logs, grep output or stack traces.
- Do not carry over anything already written to PLAN.md, TASKS.md, STATUS.md
  or docs/spec-notes/ — name the file instead.
- Never invent or round a measured number.
