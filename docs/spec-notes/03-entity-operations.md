# 03 — Entity operations, persistence context, lifecycle transitions

Condensed from Jakarta Persistence 3.2 spec, chapter 3 (EntityManager &
persistence context). Card M4-JP-26 scope: the in-memory state machine and
identity map only — no database flush, no SQL, no transaction sync.

## Entity states (spec §3.2)

```
            persist()
  NEW ─────────────────→ MANAGED
                           │
        detach() / clear() │  remove()
              ┌────────────┼──────────→ REMOVED
              ↓            │
          DETACHED ←───────┘
              │                  merge()
              └────────────────→ MANAGED (returns managed copy)
```

- **NEW**: never persisted, no id assigned, not in any persistence context.
- **MANAGED**: tracked by a persistence context, has an id (assigned or
  generated at flush — but for M4-JP-26 the id is whatever the entity has).
- **DETACHED**: was MANAGED, now independent. Changes are not tracked.
- **REMOVED**: MANAGED but scheduled for deletion at flush. Still in the
  identity map until flush actually deletes it (or it's re-persisted).

## Operation semantics (in-memory only, no flush)

| Op | Pre-state | Post-state | Throws if |
|----|-----------|------------|-----------|
| `persist(e)` | NEW, DETACHED | MANAGED | already REMOVED? spec: persist on REMOVED → MANAGED again |
| `persist(e)` | MANAGED | MANAGED (no-op) | — |
| `persist(e)` | REMOVED | MANAGED | — |
| `merge(e)`  | any | returns MANAGED copy; input stays in its state | — |
| `remove(e)` | NEW | — | `IllegalArgumentException` (not managed) |
| `remove(e)` | MANAGED | REMOVED | — |
| `remove(e)` | DETACHED | — | `IllegalArgumentException` (not managed) |
| `remove(e)` | REMOVED | REMOVED (no-op) | — |
| `refresh(e)` | MANAGED | MANAGED | `IllegalArgumentException` if not managed |
| `detach(e)` | MANAGED, REMOVED | DETACHED | — |
| `detach(e)` | DETACHED, NEW | no-op | — |
| `clear()`   | any | all become DETACHED, context emptied | — |
| `contains(e)` | any | returns true iff MANAGED or REMOVED | — |
| `flush()`   | — | no-op for M4-JP-26 (no DB) | — |

## Identity map (first-level cache)

- Keyed by `(entityClass, primaryKey)`.
- At most one managed instance per identity within a persistence context.
- `find()` checks the identity map first (no DB in M4-JP-26, so find returns
  the cached instance or throws `EntityNotFoundException` / returns null).
- `contains()` returns true iff the entity instance is the one in the map.

## Id extraction

`EntityModel.getIdAttributes()` returns the id `Attribute`. For a single-id
entity (`isSingleId()`), `Attribute.get(entity)` reads the id value. This is
how the identity map is keyed.

## What M4-JP-26 does NOT do

- No database access, no SQL, no JDBC.
- No flush to the database (flush is a no-op).
- No `find()` from the database (returns from identity map or null/throws).
- No `getReference()` lazy proxy.
- No lock modes (lock/getLockMode throw UnsupportedOperationException).
- No transaction integration (M4-JP-27).
- No cascade (M9).
- No lifecycle callbacks (M8).

## Test strategy

Unit tests with a simple test entity (`@Entity` class with `@Id`). The
persistence context is tested directly (not through EM) for state transitions.
Then integration through EM for the delegation + ensureOpen contract.
