---
description: Run the full validation gate — build, unit tests, drift audit — and report the real result. Required before marking any card DONE.
agent: jpa-dev
---
Run the validation gate. Report only what you observe in tool output.

1. Build, routed through the `ctx` tools so the log does not enter your context:
   `./mvnw -ntp clean install` from the mansart root.
   Report `BUILD SUCCESS` / `BUILD FAILURE` per module, and on failure the first
   real compilation error with `file:line`.
2. Unit tests: report `pass/total` read from the surefire reports, not from the
   console summary.
3. Delegate a drift audit to `@auditor` on `git diff main...HEAD` restricted to
   `mansart-jakarta-persistence/`. Any `blocker` finding fails the gate.
4. If the card names a TCK client, delegate the run to `@tck-runner` and report
   its numbers.
5. Delegate a SonarQube scan of the **module this card touched** to
   `@sonar-runner` — one module, not the whole reactor. A `BLOCKER` or `CRITICAL`
   on new code fails the gate. Pre-existing debt does not: you are accountable for
   the code this card wrote, not for the backlog.

Verdict, and nothing else:

```
GATE: PASS | FAIL
build: <...>   unit: <n>/<n>   audit: CLEAN|DRIFT(<n> blockers)
tck: <n>/<n> or n/a   sonar: PASS|FAIL|NOT RUN (<blocker>B/<critical>C on new code)
<if FAIL: the single next thing to fix>
```

A gate that was not run is a FAIL. Never write `GATE: PASS` from memory of an
earlier build.
