# 07 — EntityManagerFactory, entity managers, transaction association

Condensed from Jakarta Persistence 3.2 spec, chapter 4 (EntityManagerFactory
& EntityManager) and chapter 5 (transaction association). Card M4-JP-27
scope: wire `getTransaction()`, `joinTransaction()`, `isJoinedToTransaction()`
to `mansart-transactions`.

## Transaction types (spec §4.3)

- `PersistenceUnitTransactionType.RESOURCE_LOCAL` — application-managed,
  resource-local transactions via `EntityTransaction` (`begin/commit/rollback`).
- `PersistenceUnitTransactionType.JTA` — container or JTA-managed; the
  `EntityManager` joins the JTA transaction via `joinTransaction()`.

## getTransaction() (spec §4.4.4)

```
getTransaction()
  ├── if RESOURCE_LOCAL → return EntityTransaction
  └── if JTA → throw IllegalStateException
```

- Exempt from the closed-state contract (per `EntityManager.close()` Javadoc,
  `getTransaction()` is one of the three methods that does NOT throw
  `IllegalStateException` when the EM is closed — alongside `isOpen()` and
  `getProperties()`).
- Returns a `jakarta.persistence.EntityTransaction` with `begin()`, `commit()`,
  `rollback()`, `setRollbackOnly()`, `getRollbackOnly()`, `isActive()`, `getRollbackOnly()`.

## joinTransaction() (spec §4.4.5)

```
joinTransaction()
  ├── if RESOURCE_LOCAL → TransactionRequiredException (spec: only for JTA)
  └── if JTA → associate EM with the current JTA transaction
```

- For RESOURCE_LOCAL: spec says calling `joinTransaction()` is unnecessary and
  the behaviour is that the EM is already in its own resource transaction. The
  TCK expects `joinTransaction()` to work or be a no-op for RESOURCE_LOCAL; the
  safe behaviour: for RESOURCE_LOCAL, `joinTransaction()` is a no-op (the EM is
  always "joined" to its own resource transaction when one is active). For JTA,
  it registers the EM as a `Synchronization` with the JTA transaction.

## isJoinedToTransaction() (spec §4.4.6)

```
isJoinedToTransaction()
  ├── RESOURCE_LOCAL → true iff an EntityTransaction is active
  └── JTA → true iff joined to a JTA transaction (Status.STATUS_ACTIVE)
```

## Binding to mansart-transactions

`mansart-transactions` (`io.vidocq.mansart.transactions.core.MansartTransactionManager`)
implements `jakarta.transaction.TransactionManager`:

- `begin()`, `commit()`, `rollback()`, `getStatus()`, `getTransaction()`
- Uses `ThreadLocal<MansartTransaction>` (not ScopedValue — that is a future
  migration; the current TM is ThreadLocal-based).
- `getTransaction()` returns the active `jakarta.transaction.Transaction` or
  `null` if none active.
- `MansartTransaction.getStatus()` returns `jakarta.transaction.Status.*`
  constants (`STATUS_ACTIVE`, `STATUS_NO_TRANSACTION`, etc.).

The EM does NOT own or create a `TransactionManager` instance. It receives a
`TransactionManager` reference (or null for RESOURCE_LOCAL-only EMs) from the
EMF at construction time. The EMF obtains it from the application/container.

## What M4-JP-27 does NOT do

- No `ScopedValue` migration (card title is aspirational; the real TM is
  ThreadLocal). Documented here so the next session does not "fix" it.
- No connection management (M4-JP-28).
- No flush-on-commit wiring (flush is still a no-op from M4-JP-26).
- No `runInTransaction` / `callInTransaction` on EMF (those remain
  `UnsupportedOperationException` — they are convenience methods that need
  both transaction and connection integration).

## Test strategy

Unit tests in `mansart-persistence-tests`:
- RESOURCE_LOCAL: `getTransaction()` returns an `EntityTransaction`;
  `begin()`/`commit()`/`rollback()`/`isActive()` cycle; `isJoinedToTransaction()`
  reflects active state; `joinTransaction()` is a no-op (no exception).
- JTA: `getTransaction()` throws `IllegalStateException`;
  `isJoinedToTransaction()` returns false when no JTA tx active, true after
  `joinTransaction()` with an active `MansartTransactionManager` tx.
- Closed EM: `getTransaction()` does NOT throw (exempt per Javadoc);
  `joinTransaction()` and `isJoinedToTransaction()` DO throw
  `IllegalStateException` when closed.
- The existing tests expecting `UnsupportedOperationException` for these
  methods are updated to reflect the implemented behaviour.
