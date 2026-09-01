# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: JP-32 (lock — deferred to M8).
- M2 (EntityManager CRUD) — 14 / 28 cards (JP-21, JP-22, JP-23, JP-24a, JP-24b, JP-24c, JP-24d, JP-25, JP-26, JP-27, JP-28, JP-29, JP-30, JP-34 done).
  Full suite: 253 pass, 0 fail, 1 skip (core module); 161 pass (data module).
- Trap: stub provider still not wired (TCK baseline unchanged).

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | 991 run, 989 errors, 2 skipped (same baseline — stub provider not wired) | 2026-08-28 |
| unit tests | 253 pass / 254 total (1 skipped, core module); 161 pass (data module) | 2026-09-01 |
| build | 34/34 (compile, `./mvnw -ntp clean compile -pl mansart-jakarta-persistence/mansart-persistence-core -am -DskipTests`) | 2026-09-01 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 12 / 12 cards (JP-09…JP-20).
M2 (EntityManager CRUD) — 13 / 28 cards (JP-21, JP-22, JP-23, JP-24a, JP-24b, JP-24c, JP-24d, JP-25, JP-26, JP-27, JP-28, JP-29, JP-30 done).

## Session log

2026-09-01 | JP-34 | `refresh()`: re-reads managed entity state from DB via SELECT + rebinds all attribute values. Null → `IllegalArgumentException`, unmanaged → `IllegalArgumentException`, closed EM → `IllegalStateException`. 3/3 unit tests (RefreshTest). Full suite: 253 pass, 0 fail, 1 skipped.
2026-09-01 | JP-30 | `clear()`: delegates to `persistenceContext.clear()` which clears `managedEntities` and `registeredById`. `contains()` already implemented (delegates to `persistenceContext.contains()`). 6/6 unit tests (ClearContainsTest). Full suite: 250 pass, 0 fail, 1 skipped.
2026-09-01 | JP-29 | `flush()`: iterates managed entities, calls existing `flushManagedEntity()` for each. `getFlushMode()` returns `FlushModeType.AUTO` (default). `setFlushMode()` sets the field. PersistenceContext gains `entities()` and `managedEntities()` accessors. 8/8 unit tests (FlushTest). Full suite: 244 pass, 0 fail, 1 skipped.
2026-09-01 | JP-28 | `merge()`: null → `IllegalArgumentException`, closed EM → `IllegalStateException`, managed entity → returns self, detached entity → `find()` + state copy + UPDATE to DB, new entity (no ID) → `persist()`. 4/4 unit tests (MergeTest). Full suite: 236 pass, 0 fail, 1 skipped.
2026-08-31 | JP-26 | `remove()`: managed entity (DELETE + unregister), detached entity (find by ID from DB + DELETE + unregister), new entity (no-op per spec), already-removed (no-op), post-close (IllegalStateException), null (IllegalArgumentException). 6/6 unit tests (RemoveBasicTest). Full suite: 230 pass, 0 fail, 1 skipped.
2026-08-31 | JP-27 | `remove()` relationship-specific cleanup: clears FK columns on inverse-side entities (scalar FK values via backwards-compatible ReferenceAttribute constructor). Matching by FK column name pattern `<TABLE_UPPER>_<ID_COLUMN_UPPER>`. Handles managed + detached (database scan). 2/2 unit tests (RemoveRelationshipTest: Author/Book bidirectional @OneToOne). Full suite: 232 pass, 0 fail, 1 skipped.
2026-08-31 | JP-25 | `find()` by ID: checks persistence context identity map first, queries DB via dialect's `select` with `Where.eq(id)`, maps ResultSet to entity, registers in persistence context. 4/4 unit tests (FindByIdTest). Full suite: 224 pass, 0 fail, 1 skipped.
2026-08-31 | JP-24d | `persist()` one-to-one: binds FK on owning side during INSERT (same as @ManyToOne), skips inverse side FK binding during INSERT, scans all managed entities for `ReferenceAttribute` pointing to the persisted entity and updates their FK columns via UPDATE. 5/5 unit tests (PersistOneToOneTest). Full suite: 220 pass, 0 fail, 1 skipped.
2026-08-31 | JP-24c | `persist()` one-to-many inverse side: maintains inverse collection by updating target entity's FK column via UPDATE statement. 4/4 unit tests (PersistOneToManyTest). Full suite: 215 pass, 0 fail, 1 skip (core module).
2026-08-31 | JP-24a | `persist()` many-to-many (owning + inverse side): ~14 methods, plural attribute generation, ManyToManyAttribute/ManyToManyInverseAttribute dialect SPI, join table INSERTs via batched PreparedStatement. TCK 991 run, 989 errors, 2 skipped (same baseline). Unit 207 pass / 208 total (1 skipped, core module); 161 pass (data module).
2026-08-31 | JP-24c | `persist()` one-to-many inverse side: maintains inverse collection by updating target entity's FK column via UPDATE statement. 4/4 unit tests (PersistOneToManyTest). Full suite: 215 pass, 0 fail, 1 skip (core module).
2026-08-31 | JP-24b | `persist()` many-to-one: resolves `ReferenceAttribute` FK values — writes target entity's ID into the owning entity's FK column (nullable and non-null cases). 4/4 unit tests (PersistManyToOneTest). Full suite: 211 pass, 0 fail, 1 skip (core module).
