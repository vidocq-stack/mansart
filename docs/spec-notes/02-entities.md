# Spec note 02 — Entities, persistent fields, primary keys, ID generation

Jakarta Persistence 3.2, chapter 2. Condensed from the spec.  Focus:
primary keys, `@GeneratedValue`, `@SequenceGenerator`, `@TableGenerator`.

## Primary key (`@Id`)

- Every entity **must** have a primary key (spec §2.4).
- `@Id` maps a field/property to the PK column.  Type must be a primitive
  wrapper or `String`, `java.util.Date`, `java.sql.Date`, `java.math.BigInteger`,
  or `java.util.UUID` (JPA 3.2 adds UUID as a valid simple PK type).
- Assigned ids: the application sets the value before `persist()`.

## `@GeneratedValue` (spec §2.4.3 / §11.1.22)

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mySeq")
```

| Element   | Default  | Meaning |
|-----------|----------|---------|
| `strategy` | `AUTO`   | ID generation strategy |
| `generator`| `""`     | Name of the `@SequenceGenerator` / `@TableGenerator` to use |

### `GenerationType` enum (spec §11.1.23)

| Value      | Behaviour |
|------------|-----------|
| `AUTO`     | Provider chooses.  Mansart: treat as `IDENTITY` on H2/PG (db auto-increment). |
| `IDENTITY` | Database identity column.  Id assigned by DB on INSERT.  Read back via `Statement.getGeneratedKeys()`. |
| `SEQUENCE` | DB sequence.  Allocate id **before** INSERT via `SELECT NEXT VALUE FOR <seq>`.  Requires `@SequenceGenerator`. |
| `TABLE`    | Application-managed generator table.  Allocate id **before** INSERT from a row in a generator table.  Requires `@TableGenerator`. |
| `UUID`     | JPA 3.2 addition.  Provider generates a UUID before INSERT.  Not in Mansart yet (out of scope for M5-JP-30). |

Key timing difference: `IDENTITY` reads the key **after** insert (post-insert);
`SEQUENCE` / `TABLE` allocate **before** insert (pre-insert).

## `@SequenceGenerator` (spec §11.1.50)

```java
@SequenceGenerator(name = "mySeq", sequenceName = "MY_SEQ",
                   initialValue = 1, allocationSize = 50, catalog = "", schema = "")
```

| Element        | Default | Meaning |
|----------------|---------|---------|
| `name`         | (req)   | Name referenced by `@GeneratedValue(generator=...)` |
| `sequenceName` | = name  | DB sequence name |
| `catalog`      | `""`    | Catalog (usually empty) |
| `schema`       | `""`    | Schema (usually empty) |
| `initialValue` | `1`     | First value (creates sequence if absent — not our job; DDL is) |
| `allocationSize` | `50`  | Pool size for allocation (pre-fetch).  Mansart: 1 for simplicity. |

If no `@SequenceGenerator` is named, default: sequenceName = generator name,
allocationSize = 50 (spec default).  Mansart: if the entity uses `SEQUENCE`
but no `@SequenceGenerator` is present, use the entity's table name as the
sequence name with a `_seq` suffix.

## `@TableGenerator` (spec §11.1.57)

```java
@TableGenerator(name = "myGen", table = "ID_GEN",
                pkColumnName = "GEN_NAME", valueColumnName = "GEN_VAL",
                pkColumnValue = "myGen", initialValue = 0, allocationSize = 50)
```

| Element          | Default     | Meaning |
|------------------|-------------|---------|
| `name`           | (req)       | Name referenced by `@GeneratedValue(generator=...)` |
| `table`          | `""`        | Generator table name (default `"sequence_generator"` if empty per spec) |
| `catalog`/`schema` | `""`      | Catalog/schema |
| `pkColumnName`   | `"SEQUENCE_NAME"` | PK column name in gen table |
| `valueColumnName`| `"SEQUENCE_NEXT_VAL"` | Value column name |
| `pkColumnValue`  | = name      | PK value identifying this generator row |
| `initialValue`   | `0`         | Starting value |
| `allocationSize` | `50`        | How many ids to grab per DB round-trip |

Table generator works by: `SELECT valueColumnName FROM table WHERE pkColumnName = pkColumnValue`,
then `UPDATE table SET valueColumnName = valueColumnName + 1 WHERE ...`.  Mansart:
allocationSize = 1 for simplicity (one DB round-trip per id, no in-memory pool).

## Mansart implementation contract (M5-JP-30)

### Strategy dispatch in `EntityMapper.insert()`

```
if id not generated → use assigned value, include in INSERT
if strategy == IDENTITY → skip id in INSERT, read getGeneratedKeys() after
if strategy == AUTO → treat as IDENTITY on H2
if strategy == SEQUENCE → allocate id before INSERT via sequence, include in INSERT
if strategy == TABLE → allocate id before INSERT via gen table, include in INSERT
```

### New classes (mansart-persistence-core)

- `IdGenerator` interface: `Object generate(Connection, Dialect)`
- `SequenceIdGenerator` — `SELECT NEXT VALUE FOR <seq>`, returns the id
- `TableIdGenerator` — selects+updates a generator table row
- `IdGeneratorRegistry` — maps `Class<? entity>` → `IdGenerator` (or null for IDENTITY/assigned)

### SPI additions

- `IdAttribute.getGenerationStrategy()` already exists (AUTO, IDENTITY, SEQUENCE, TABLE)
- `IdAttribute.getGenerator()` already exists (default `""`)
- `IdAttribute.getSequenceName()` already exists (default `""`)
- Need to capture `@SequenceGenerator` and `@TableGenerator` metadata at model-build time
- New: `IdAttribute.getTableGeneratorName()` / `getPkColumnValue()` etc. — or a single
  `GeneratorConfig` record on the `IdAttribute`.

### Runtime builder (RuntimeEntityModelBuilder)

Must parse `@SequenceGenerator` and `@TableGenerator` annotations from the
entity class (class-level annotations) and store them on the `RuntimeIdAttribute`.

### APT processor

Must pass `GenerationStrategy` + generator name + sequence name into
`FieldMetadata` and emit `getGenerationStrategy()` / `getGenerator()` /
`getSequenceName()` overrides in the generated `IdAttr` class.

### Tests (TDD)

Test entity classes needed:
- `SequenceIdEntity` — `@GeneratedValue(strategy=SEQUENCE)` + `@SequenceGenerator`
- `TableIdEntity` — `@GeneratedValue(strategy=TABLE)` + `@TableGenerator`
- `AutoIdEntity` — `@GeneratedValue(strategy=AUTO)` (behaves like IDENTITY on H2)

DDL for tests must create the sequence and generator table.
