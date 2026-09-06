# Spec notes — index

**Load exactly one note.** This index exists so an agent can pick the right
file without opening several to find out which one it needed. Reading two
notes to answer one question defeats the purpose.

Budget: **200 lines per note**. A note that outgrows it splits into
`<nn>a-…` / `<nn>b-…`, and both rows are listed here.

Only the `spec` agent writes in this directory. Format and per-note template:
`README.md`.

| # | Chapter | Note | Status | Lines |
| --- | --- | --- | --- | --- |
| 02 | Entities: requirements, persistent fields, primary keys, embeddables | `02-entities.md` | empty | — |
| 03 | `EntityManager`, persistence context, lifecycle transitions, cascade, orphan removal | `03-entity-operations.md` | partial | 73 |
| 04 | JPQL: syntax, path expressions, joins, functions | `04-query-language.md` | empty | — |
| 05 | Metamodel API, static metamodel generation | `05-metamodel.md` | empty | — |
| 06 | Criteria API construction and typing | `06-criteria-api.md` | empty | — |
| 07 | `EntityManagerFactory`, container vs application-managed, transaction association | `07-entity-managers.md` | partial | 86 |
| 08 | `persistence.xml`, persistence units, class discovery | `08-entity-packaging.md` | empty | — |
| 09 | Entity listeners, callback methods, invocation order | `09-lifecycle-callbacks.md` | empty | — |
| 10 | Mapping annotations, defaults, overrides | `10-metadata-annotations.md` | empty | — |
| 11 | `orm.xml`, XML/annotation precedence | `11-metadata-xml.md` | empty | — |
| 12 | Optimistic and pessimistic locking, `@Version` | `12-locking-concurrency.md` | empty | — |
| 13 | Proxy semantics, fetch graphs, `LazyInitializationException` | `13-lazy-loading.md` | empty | — |

`Status`: `empty` → `partial` → `complete`. Update the row and the line count
in the same edit that changes a note — a stale index sends an agent to the
wrong file, which costs exactly what this directory is meant to save.

Chapter numbering follows the Jakarta Persistence 3.2 specification document;
confirm a number against the revision you actually consult before relying on
it. Add a row when a chapter turns out to matter and is missing. Delete none:
a chapter with no note is information too.
