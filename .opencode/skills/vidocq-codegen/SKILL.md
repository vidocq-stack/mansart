---
name: vidocq-codegen
description: The three-tier code-generation doctrine for Vidocq — APT, Maven plugin with the Class-File API, and the runtime fallback. Load before writing any generator or whenever reflection looks like the answer.
---

# Generating code instead of reflecting

Vidocq's rule is that everything a framework would normally discover by
reflection at runtime is instead **produced as code before it runs**. That is
what makes the runtime AOT-friendly (GraalVM native image, Leyden CDS) and what
keeps startup flat.

## Tier 1 — APT, `mansart-persistence-processor`

`javax.annotation.processing.Processor`, `SourceVersion.RELEASE_25`. Sees every
`@Entity`, `@Embeddable`, `@MappedSuperclass` in the sources being compiled and
emits **Java sources** into `target/generated-sources/annotations/`:

- `_Entity` — the JPA static metamodel, typed attributes, `@Generated`.
- `EntityDescriptor` — all mapping metadata as `static final` constants: table,
  columns, identity, associations, fetch plans, callbacks.
- accessor classes — direct `getfield`/`putfield` on the entity's fields, no
  `Field.get`, no `MethodHandle`.

This is the default path and it covers the overwhelming majority of real
applications. Reference implementation in this repository:
`mansart-jakarta-data/mansart-data-processor`.

APT constraints worth remembering: you may not read a class that does not exist
yet, you get one shot per round, and you must never fail the build with an
exception — report through `Messager` with the offending `Element` so the error
lands on the right line.

## Tier 2 — Maven plugin, `java.lang.classfile`

An `@Entity` that arrives inside a **dependency jar** was compiled elsewhere; APT
never saw it. `mansart-persistence-maven-plugin`, bound to `process-classes`,
scans the dependency classpath, finds entities with no tier-1 descriptor, and
emits the *same shapes* as `.class` files into `target/generated-classes` using
the Class-File API (JEP 484).

This is also where lazy-association proxy classes for external entities are
produced — generated subclasses with real overridden methods, never a
`java.lang.reflect.Proxy`.

Reference implementation:
`mansart-jakarta-data/mansart-data-maven-plugin` →
`GenerateExternalRepositoriesMojo`, with `mansart-data-external-lib` as the
fixture jar and `mansart-data-external-it` as the integration test. Mirror all
three.

**Tier 1 and tier 2 must be behaviourally identical.** Every shape added to one
is added to the other, plus a test in `mansart-persistence-external-it` that runs
the same assertion against a tier-1 entity and a tier-2 entity.

## Tier 3 — runtime Class-File API, extreme fallback only

`ClassFile.of().build(...)` plus `MethodHandles.Lookup.defineHiddenClass`, at
`EntityManagerFactory` bootstrap, for an entity that neither previous tier
reached — a jar added at runtime, a container that skipped the plugin.

It works, and it must stay visibly exceptional: log a warning naming the class
and pointing at the Maven plugin. It is never the answer to "APT is awkward
here".

## Hard bans

`java.lang.reflect.Proxy` · ASM · Byte Buddy · cglib · `Field.setAccessible` ·
`Class.forName` on a user entity · `MethodHandles` resolution against a user
class on a hot path · runtime bytecode libraries of any kind.

`MethodHandles.Lookup.defineHiddenClass` in tier 3 is the single permitted use of
`MethodHandles`, and it defines a class — it does not reflect on one.

## The question to answer before reaching for reflection

Write down which of the three tiers fails, and why. If you cannot name one, the
answer is codegen. Escalate to `@codegen` rather than improvising, and to
`@thinker` if the trade-off is genuinely a design decision.
