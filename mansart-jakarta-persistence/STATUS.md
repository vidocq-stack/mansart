# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: JP-27 — remove(): relationship-specific (one-to-many, one-to-one)  [TODO]
- M2 (EntityManager CRUD) — 9 / 28 cards (JP-21, JP-22, JP-23, JP-24a, JP-24b, JP-24c, JP-24d, JP-25, JP-26 done).
  Full suite: 230 pass, 0 fail, 1 skip (core module); 161 pass (data module).
- Trap: stub provider still not wired (TCK baseline unchanged).

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | 991 run, 989 errors, 2 skipped (same baseline — stub provider not wired) | 2026-08-28 |
| unit tests | 230 pass / 231 total (1 skipped, core module); 161 pass (data module) | 2026-08-31 |
| build | 34/34 (compile, `./mvnw -ntp clean compile -pl mansart-jakarta-persistence/mansart-persistence-core -am -DskipTests`) | 2026-08-31 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 12 / 12 cards (JP-09…JP-20).
M2 (EntityManager CRUD) — 9 / 28 cards (JP-21, JP-22, JP-23, JP-24a, JP-24b, JP-24c, JP-24d, JP-25, JP-26 done).

## Session log

2026-08-31 | JP-26 | `remove()`: managed entity (DELETE + unregister), detached entity (find by ID from DB + DELETE + unregister), new entity (no-op per spec), already-removed (no-op), post-close (IllegalStateException), null (IllegalArgumentException). 6/6 unit tests (RemoveBasicTest). Full suite: 230 pass, 0 fail, 1 skipped.
2026-08-31 | JP-25 | `find()` by ID: checks persistence context identity map first, queries DB via dialect's `select` with `Where.eq(id)`, maps ResultSet to entity, registers in persistence context. 4/4 unit tests (FindByIdTest). Full suite: 224 pass, 0 fail, 1 skipped.
2026-08-31 | JP-24d | `persist()` one-to-one: binds FK on owning side during INSERT (same as @ManyToOne), skips inverse side FK binding during INSERT, scans all managed entities for `ReferenceAttribute` pointing to the persisted entity and updates their FK columns via UPDATE. 5/5 unit tests (PersistOneToOneTest). Full suite: 220 pass, 0 fail, 1 skipped.
2026-08-31 | JP-24c | `persist()` one-to-many inverse side: maintains inverse collection by updating target entity's FK column via UPDATE statement. 4/4 unit tests (PersistOneToManyTest). Full suite: 215 pass, 0 fail, 1 skip (core module).
2026-08-31 | JP-24a | `persist()` many-to-many (owning + inverse side): ~14 methods, plural attribute generation, ManyToManyAttribute/ManyToManyInverseAttribute dialect SPI, join table INSERTs via batched PreparedStatement. TCK 991 run, 989 errors, 2 skipped (same baseline). Unit 207 pass / 208 total (1 skipped, core module); 161 pass (data module).
2026-08-31 | JP-24c | `persist()` one-to-many inverse side: maintains inverse collection by updating target entity's FK column via UPDATE statement. 4/4 unit tests (PersistOneToManyTest). Full suite: 215 pass, 0 fail, 1 skip (core module).
2026-08-31 | JP-24b | `persist()` many-to-one: resolves `ReferenceAttribute` FK values — writes target entity's ID into the owning entity's FK column (nullable and non-null cases). 4/4 unit tests (PersistManyToOneTest). Full suite: 211 pass, 0 fail, 1 skip (core module).
