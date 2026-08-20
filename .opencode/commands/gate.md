---
description: Validation gate — full mansart build through ctx, then an independent review of the change set
agent: persistence-dev
---
Run the validation gate for the current change set:

1. From the mansart root: `./mvnw -ntp clean install` — through `ctx_batch_execute`,
   extracting only `BUILD SUCCESS|FAILURE`, the reactor summary, and any `ERROR`/`Tests
   run:` lines with failures.
2. Ask `@gate-reviewer` to review `git diff` against the checklist.
3. Report verbatim: build verdict per module (or the first failing module + error),
   reviewer verdict, and whether the task may be ticked. Never soften a failure.
Context: $ARGUMENTS
