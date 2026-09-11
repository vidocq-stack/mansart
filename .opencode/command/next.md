---
description: Do ONE card end to end (TDD, build, sonar, commit) then STOP
agent: lead
---
ARGS: $ARGUMENTS   (<XXX>)

ONE card, then STOP. AGENTS.md §1-4 apply. YOU write no source, no STATUS, no
commit by hand: three scripts are the gates and the only writers.

0. WHERE CODE LIVES — read it, never guess it
   IMPL=$(python3 scripts/tck-module.py $ARGUMENTS --impl)      e.g. mansart-jakarta-persistence/mansart-jakarta-persistence-core
   PKG=$(python3 scripts/tck-module.py $ARGUMENTS --package)    e.g. io.vidocq.mansart.persistence
   All src/main and src/test go under $IMPL, package $PKG. NEVER the parent
   module (packaging pom: compiles nothing, exit 0), NEVER the -tck runner,
   NEVER package ee.jakarta.tck.* (the TCK's own). Give both to every subagent.

1. PICK   ./scripts/verify-m0.sh $ARGUMENTS must be 7 pass — else STOP: run /tck.
          First M1+ card in TASKS-$ARGUMENTS.md with no row in STATUS "Done
          cards". Say its id. Copy its title, sections and done-when verbatim.
2. LOCATE @recon: "card <id>: <title>. In $IMPL, which files exist for this? 3 lines."
          (Do NOT list jars or dump bytecode: the guard refuses, and refuses
          harder the third time. What the TCK needs is in docs/spec-notes/.)
3. RED    @tdd: "failing JUnit 5 test for card <id> in $IMPL/src/test/java/<PKG path>/…,
          package $PKG. Put the card id <id> in the test class name or a comment
          — card-done.sh looks for it. Run ./scripts/build.sh -pl $IMPL test and
          quote the failing assertion." No RED (compile error is not red)? STOP.
4. GREEN  @impl: "make card <id>'s test pass in $IMPL/src/main/java, package
          $PKG, Class-File API or APT — no runtime reflection; do not touch
          src/test". Failed twice -> @thinker, then ONE more @impl. Still red ->
          say BLOCKED with the error in your report, STOP (no STATUS row).
5. GATE   ./scripts/card-done.sh $ARGUMENTS <id>
          It runs the module's tests itself and writes the STATUS row only on
          exit 0 with run>0, failures=0, errors=0. exit 1/4 = not done: read its
          line, go back to step 4 (max 2 more rounds), else STOP as BLOCKED.
6. SONAR  @verify: ./scripts/sonar.sh $IMPL. New issues -> @impl fixes, tests
          READ-ONLY, max 2 tries, @verify re-scans. Not configured -> "skipped".
7. COMMIT ./scripts/card-commit.sh $ARGUMENTS <id> <feat|fix|test|refactor> <scope> "<what, imperative, lowercase>"
          It stages only $IMPL and STATUS, signs (-S -s), formats the message and
          the Co-Authored-By trailer. Signature fails -> not done, STOP.
8. STOP.  Report 3 lines: card, the card-done.sh numbers, the sha.
          "run /push $ARGUMENTS when you want it pushed".
