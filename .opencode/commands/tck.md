---
description: Run the Persistence TCK via the out-of-reactor runner and triage — args are smoke | all | full | pg | -Dtest=<ClientClass>
agent: tck-runner
---
Run the Jakarta Persistence 3.2 TCK with the runner script in
`mansart-jakarta-persistence/mansart-persistence-tck` using this argument: `$ARGUMENTS`
(empty = `--smoke` then `--all`). Route all Maven output through the ctx tools.
Report PASS/Total (+ errors/failures/skipped), the failure-bucket table, the top 10
normalised error messages, and the single highest-leverage next action.
