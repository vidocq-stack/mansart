---
description: Run the official Jakarta Persistence 3.2 TCK. Pass a client class name to run just that one, or "all" for the full suite.
agent: tck-runner
subtask: true
---
Run the Jakarta Persistence 3.2 TCK for: $ARGUMENTS

If $ARGUMENTS is empty, run the client class named in the `## Current focus`
block of `mansart-jakarta-persistence/STATUS.md`.
If $ARGUMENTS is `all`, run the full suite — say up front that this takes a long
time, and report the totals plus the top distinct root causes.

Load the `mansart-jpa-tck` skill for the exact commands. Route the invocation
through the `ctx` tools and read the surefire reports; never let the raw Maven
log into your context. Report the real integers.
