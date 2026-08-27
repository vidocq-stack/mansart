---
description: Run SonarQube on the local Docker instance. Pass a module name for one module, or "all" for the whole persistence sub-reactor.
agent: sonar-runner
subtask: true
---
Run SonarQube analysis for: $ARGUMENTS

If $ARGUMENTS is empty, analyse the module named in the `## Current focus` block of
`mansart-jakarta-persistence/STATUS.md`.
If $ARGUMENTS is `all`, analyse the whole `mansart-jakarta-persistence` sub-reactor
— say up front that this takes several minutes.

Start the `vidocq-sonar` container if it is not running and wait for
`/api/system/status` to report `UP` before scanning. Route the Maven invocation
through the `ctx` tools and read the outcome from the SonarQube API, never from the
scanner log. Report in your standard `SONAR:` format, issues on new code only.
