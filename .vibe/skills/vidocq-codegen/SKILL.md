---
name: vidocq-codegen
description: The three-tier static code generation doctrine used across the Vidocq ecosystem (APT, Class-File API Maven plugin, runtime Class-File API fallback). Load before writing anything that would otherwise use reflection.
user-invocable: false
---

# Code generation: the three tiers

The rule the whole architecture rests on: **everything a framework would
normally discover by reflection is produced as code before it runs.**

1. **APT** — `mansart-persistence-processor`, `SourceVersion.RELEASE_25`.
   Sees the entities in the sources being compiled, emits Java sources:
   `_Entity` static metamodel, `EntityDescriptor` (mapping metadata as
   constants), and accessors doing direct `getfield`/`putfield`. Covers the
   overwhelming majority of cases.
2. **Maven plugin** — `mansart-persistence-maven-plugin`, bound to
   `process-classes`, using the **Class-File API** (`java.lang.classfile`,
   JEP 484). An `@Entity` inside a dependency jar was compiled elsewhere and
   APT never sees it; the plugin scans the dependency classpath and emits the
   same shapes as `.class` files. Lazy-association proxies for external
   entities are generated here too — real subclasses, never
   `java.lang.reflect.Proxy`.
3. **Runtime Class-File API** — `ClassFile.of().build(...)` plus
   `MethodHandles.Lookup.defineHiddenClass` at `EntityManagerFactory`
   bootstrap, for an entity neither tier reached. An escape hatch, not a
   design: it logs a warning naming the class and pointing at the plugin.

Tiers 1 and 2 must be behaviourally identical, enforced by
`mansart-persistence-external-it` running the same assertions against a
tier-1 entity and a tier-2 entity loaded from
`mansart-persistence-external-lib`.

**This is not speculative.** All three shapes already ship in this
repository for Jakarta Data: `mansart-data-processor`,
`mansart-data-maven-plugin` (`GenerateExternalRepositoriesMojo`),
`mansart-data-external-lib`, `mansart-data-external-it`. The persistence
work mirrors a proven pattern rather than inventing one.

## Banned

`java.lang.reflect.Proxy`, ASM, Byte Buddy, cglib, `Field.setAccessible`,
`Class.forName` on a user entity, `MethodHandles` resolution against a user
class on a hot path.

The only acceptable placeholder for something genuinely not implemented yet
is `throw new UnsupportedOperationException("not implemented: <what>")` —
never a fake `null`/`0`/`false`/empty-collection return to quiet a test.
`recon` checks for exactly this pattern.
