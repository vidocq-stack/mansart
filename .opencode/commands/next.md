---
description: Start a work session — read STATUS.md, pick the next TODO card from TASKS.md, and announce the plan before touching anything.
agent: jpa-dev
---
Start a new work session on mansart-jakarta-persistence.

**Session warm-up (do this first, once, before anything else):**

- **Wake jdtls by READING a Java file — not by calling `lsp`.** OpenCode starts a
  language server only when a file of that language is opened: "LSPs will activate
  as files are read". A bare `lsp` call hits a server that does not exist yet and
  comes back empty. So, in order:
  1. `read` one small existing Java file to trigger the spawn — e.g.
     `mansart-jakarta-data/mansart-data-core/src/main/java/module-info.java` (a
     dozen lines; this is the one justified whole-file `read` of the session).
  2. **Index the TCK** (below) — this takes a second and gives jdtls a moment to
     begin indexing the 30-module reactor.
  3. Now call `lsp` `workspaceSymbol` for `EntityManager`. If it is still empty,
     jdtls is mid-index — wait ~15 s and retry once. If it answers, jdtls is warm
     for the whole session. If it still fails, say so and continue on `read` — but
     that is the degraded mode, not the normal one.
- **Index the TCK for search.** Call `ctx_ctx_index` with
  `path: ".tck-ref/tck-index.md"`, `source: "JPA32-TCK"`. Nothing enters context.
  This makes `@spec-reader` able to `ctx_search` the TCK + spec instead of
  unzipping jars. If `.tck-ref/tck-index.md` is missing, run
  `python3 .opencode/build-tck-index.py` first (the TCK skill documents the
  one-time source extraction it needs).

Then:

1. Read `mansart-jakarta-persistence/STATUS.md` — the `## Current focus` block and
   the last 3 lines of `## Session log` only. Do not read the whole file if it has
   grown.
2. Read `mansart-jakarta-persistence/TASKS.md` and pick the first card whose
   status is `TODO` and whose dependencies are all `DONE`. If $ARGUMENTS names a
   card id, use that one instead and say why it is safe to take out of order.
3. Load the `mansart-jpa` skill, and `mansart-jpa-tck` if the card names a TCK
   client.
4. Before writing any code, state in at most 12 lines:
   - the card id and its goal;
   - the failing test you will write FIRST, and its exact path;
   - the files you will touch (max 4);
   - the command that will prove the card is done.

Then stop and wait for nothing — proceed with step 4's plan, starting with the
failing test. Do not open a second card in this session.
