# Spec notes — condensed Jakarta Persistence 3.2

**Why this directory exists.** Agents read the notes here, never the full spec
text. A spec chapter costs tens of thousands of input tokens every time it
enters a context; a note costs a few hundred and can be cached. This is the
largest single lever on session cost in this project, and it only works if the
notes are actually filled in and actually trusted.

**Who writes here.** The `spec` agent (GLM 5.2) — it is the only agent with
write access to this directory, and the only directory it can write to.
`impl` and `recon` read these files; they never edit them.

## How to fill a note

Do not paste spec prose. Extract, then decide. A note is finished when an
implementer can write the code from the note alone, and an auditor can tell
from the note whether the code is wrong.

Work chapter by chapter, on demand: fill a note the first time a card needs
that chapter, not speculatively. An empty note is honest; a note filled from
memory is worse than no note.

## Per-note template

Copy this into `<nn>-<chapter-slug>.md`.

```markdown
# <nn> — <Chapter title>

**Spec**: Jakarta Persistence 3.2, §<x.y>–<x.z>
**Status**: empty | partial | complete
**Last verified**: <YYYY-MM-DD> against <the spec revision consulted>

## Normative constraints

<!-- One numbered item per MUST/MUST NOT/SHOULD. Each carries its §. State the
     rule, not the paragraph it came from. If two chapters conflict, record
     both and resolve the conflict in "Decisions" below — never silently pick
     one. -->

1. **§x.y** — <constraint, imperative, one sentence>
2. **§x.y** — <constraint>

## Behaviour a test can check

<!-- The constraints above, restated as observable outcomes. This is what a
     failing test gets compared against. -->

| Situation | Required outcome | § |
| --- | --- | --- |
|  |  |  |

## Decisions taken for mansart

<!-- Where the spec permits latitude, or where two chapters disagree, record
     what this implementation does and why. This is the part the spec does not
     contain and cannot be re-derived. -->

- **<decision>** — <rationale>. Alternative rejected: <what, and the fact that
  kills it>.

## TCK anchors

<!-- Which conformance tests pin these rules. Names only — never copy test
     code into this repository's main sources or notes. -->

- `<FQCN>#<method>` — pins constraint <n>.

## Open questions

- <question> — blocks <card id>, or "not blocking".
```

## Chapter index

Fill `Status` as notes are written. Chapter numbering follows the Jakarta
Persistence 3.2 specification document; confirm each against the spec revision
you actually consult before relying on the number.

| Note file | Chapter | Status |
| --- | --- | --- |
| `02-entities.md` | Entities: requirements, persistent fields, primary keys, embeddables | empty |
| `03-entity-operations.md` | `EntityManager`, persistence context, lifecycle transitions, cascade, orphan removal | empty |
| `04-query-language.md` | JPQL: syntax, path expressions, joins, functions | empty |
| `05-metamodel.md` | Metamodel API, static metamodel generation | empty |
| `06-criteria-api.md` | Criteria API construction and typing | empty |
| `07-entity-managers.md` | `EntityManagerFactory`, container vs application-managed, transaction association | empty |
| `08-entity-packaging.md` | `persistence.xml`, persistence units, class discovery | empty |
| `09-lifecycle-callbacks.md` | Entity listeners, callback methods, invocation order | empty |
| `10-metadata-annotations.md` | Mapping annotations, defaults, overrides | empty |
| `11-metadata-xml.md` | `orm.xml`, XML/annotation precedence | empty |
| `12-locking-concurrency.md` | Optimistic and pessimistic locking, `@Version` | empty |
| `13-lazy-loading.md` | Proxy semantics, fetch graphs, `LazyInitializationException` | empty |

Add a row when a chapter turns out to matter and is missing. Delete none —
a chapter with no note is information too.

## What does not belong here

- Copies of spec text. Extract the rule; cite the section.
- Implementation code or diffs. Those live in the modules.
- Session narrative or progress. That is `STATUS.md`.
- Anything already in `AGENTS.md`.
