# Spec note 10 — Mapping annotations: @Column, @Table

Jakarta Persistence 3.2, chapter 10/11. Condensed from the spec.
Focus: `@Column` and `@Table` annotation elements, defaults, and how
Mansart captures them.

## `@Column` (spec §11.1.9 / §2.2.3)

```java
@Column(name = "cust_name", nullable = false, unique = true,
        length = 128, precision = 10, scale = 2,
        insertable = true, updatable = true,
        columnDefinition = "VARCHAR(128)")
```

| Element           | Default      | Meaning |
|-------------------|--------------|---------|
| `name`            | `""` (field name → snake_case) | Column name |
| `unique`          | `false`      | Unique constraint |
| `nullable`        | `true`       | Allows NULL (false → NOT NULL) |
| `insertable`      | `true`       | Include in INSERT |
| `updatable`       | `true`       | Include in UPDATE |
| `columnDefinition`| `""`         | Native SQL column DDL override |
| `length`          | `255`        | String column length |
| `precision`       | `0`          | Numeric precision (decimal digits) |
| `scale`           | `0`          | Numeric scale (decimal digits after point) |

Defaults per spec: `length = 255`, `precision = 0`, `scale = 0`.
Mansart convention: default `length = 255` for String types, `precision = -1`
and `scale = -1` meaning "not specified" (the dialect treats -1 as
unspecified, matching the existing `Attribute` defaults).

### `unique` vs `@UniqueConstraint`

`@Column(unique = true)` is a shorthand for a single-column unique constraint.
It does NOT generate a named constraint — it's provider-resolved.  The DDL
generation is out of scope for M5-JP-31 (that's M18).  M5-JP-31 captures the
metadata; the EntityMapper and adapter use it.

## `@Table` (spec §11.1.56 / §2.2.1)

```java
@Table(name = "customers", schema = "app", catalog = "mydb")
```

| Element  | Default | Meaning |
|----------|---------|---------|
| `name`   | entity name → snake_case plural | Table name |
| `schema` | `""`    | Schema |
| `catalog`| `""`    | Catalog |

Already captured by APT and runtime. No changes needed for M5-JP-32.

## Mansart implementation contract (M5-JP-31)

### What already exists

- `Attribute.getLength()`, `getPrecision()`, `getScale()`,
  `getColumnDefinition()`, `isInsertable()`, `isUpdatable()` — all default
  methods returning -1 / "" / true.
- `Attribute.getColumnName()`, `isNullable()`, `isUnique()` — already
  populated by APT and runtime.
- `EntityModel.getSchema()`, `getCatalog()` — already populated by APT.

### What's missing (the card)

The APT processor (`MansartPersistenceProcessor`) and the runtime builder
(`RuntimeEntityModelBuilder`) only read `name`, `nullable`, `unique` from
`@Column`. They do NOT read `length`, `precision`, `scale`, `insertable`,
`updatable`, `columnDefinition`.

### Changes needed

1. **APT `FieldMetadata` record**: add `length`, `precision`, `scale`,
   `insertable`, `updatable`, `columnDefinition` fields.
2. **APT `scanField`**: read these from `@Column` annotation, with spec
   defaults (`length = 255`, `precision = 0`, `scale = 0`, `insertable =
   true`, `updatable = true`, `columnDefinition = ""`).
3. **APT `generateInnerAttr`**: pass these through the constructor and emit
   `@Override` methods for `getLength()`, `getPrecision()`, `getScale()`,
   `getColumnDefinition()`, `isInsertable()`, `isUpdatable()`.
4. **`RuntimeAttribute`**: add fields + constructor params + override
   methods.
5. **`RuntimeIdAttribute`**: add fields + constructor params + override
   methods (ids can also have `@Column`).
6. **`RuntimeEntityModelBuilder.parseField`**: read these from `@Column`
   annotation via Class-File API.
7. **`DialectEntityModelAdapter`**: already calls `getLength()`,
   `getPrecision()`, `getScale()` — no change needed once the SPI returns
   real values.
8. **EntityMapper**: must respect `isInsertable()` in `insert()` and
   `isUpdatable()` in `update()` — skip columns that are not insertable
   or not updatable.

### Tests (TDD)

- `ColumnMappingTest`: entity with `@Column(name="...", nullable=...,
  unique=..., length=..., precision=..., scale=..., insertable=...,
  updatable=...)` — assert all values via APT-generated model.
- `RuntimeEntityModelBuilderTest`: extend with `@Column(length=..., 
  precision=..., scale=..., insertable=false, updatable=false)` — assert
  via runtime model.
- `EntityMapperTest`: entity with `@Column(insertable=false)` → INSERT
  should skip that column. `@Column(updatable=false)` → UPDATE should skip.
