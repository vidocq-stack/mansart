---
description: TDD loop on ONE TCK test class until it PASSes for real — arg is the TCK Client class (FQN or simple name)
agent: persistence-dev
---
Make the TCK test class `$ARGUMENTS` PASS, properly:

1. Ask `@spec-reader` for: the test methods, the entities and `setup*Data` it uses, the
   exact assertions, and the API contract (Javadoc) of every Mansart method involved.
2. Ask `@tck-runner` to run ONLY this class
   (`./run-official-tck-persistence-3.2.sh -Dtest=$ARGUMENTS`) and classify the failure.
3. If the bucket is harness/schema (`setup*Data failed`, table/column not found): fix
   the runner (official TCK DDL), not the provider. Otherwise write or extend a unit test
   in `mansart-persistence-core` that reproduces the failing contract, watch it fail,
   implement the real behaviour (no stubs, no reflection, no TCK names in runtime),
   make it pass.
4. Re-run the TCK class until PASS. Then `./mvnw -ntp clean install` from the mansart
   root (validation gate, via ctx). Then ask `@gate-reviewer` for a verdict.
5. Report: what was wrong, what changed (files), unit tests added, TCK class result,
   full build result. Do not touch the tracker yourself — use `/session-end`.
