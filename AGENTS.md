# Mansart engineering contract

Mansart implements Jakarta specifications for JDK 25. It favors build-time
metadata, CDI Build Compatible Extensions, JPMS, and virtual-thread-friendly
blocking code. The root reactor contains independent specification modules;
Jakarta Persistence belongs under `mansart-jakarta-persistence`, never directly
at the repository root.

## Work unit

Work on one card at a time. Before editing, read its file under
`tasks/<SPEC>/<CARD>.md`, the cited normative sections under
`docs/spec-src/<SPEC>/`, and the nearest module POM and tests. Do not ingest the
whole specification when a cited chapter is sufficient.

Use this sequence:

1. Ask `architect` for a bounded plan and affected invariants.
2. Ask `implementer` for one test-driven change.
3. Run the narrow test, then the affected module build.
4. Ask `reviewer` to inspect the diff and verification evidence.
5. Fix findings and re-run verification.

Stop rather than inventing an API, Maven coordinate, TCK result, or requirement.
Normative claims must cite a local specification section. A green build with no
relevant test executed is not evidence.

## Design constraints

- Target JDK 25 and strict JPMS descriptors.
- Prefer generated immutable metadata over runtime classpath scanning,
  reflection, proxies, or bytecode enhancement.
- Use CDI Build Compatible Extensions for discovery and build-time integration.
- Keep core persistence independent from Quarkus; integration belongs in a
  separate adapter/deployment module.
- Treat `DataSource`, Jakarta Transactions, and Jakarta Data as neighboring
  contracts, not code to duplicate inside Persistence.
- Do not introduce platform threads, common-pool blocking, or assumptions that
  break virtual-thread execution.
- Do not modify delivered sibling modules unless the card explicitly describes
  a cross-module contract.

## Verification and source control

Use `./scripts/build.sh`, not raw Maven, so exit codes and logs are preserved.
Never edit generated `TASKS-*.md` files or official TCK sources. Never commit,
push, reset, restore, clean, or switch branches: the human/orchestrator owns Git.
Report the exact commands run, relevant test counts, remaining risks, and the
files changed.
