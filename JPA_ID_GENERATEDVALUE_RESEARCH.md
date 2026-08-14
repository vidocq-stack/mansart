# Jakarta Persistence 3.2 @Id and @GeneratedValue Research Summary

## 1. @Id Annotation

**Purpose**: Identifies the primary key field or property of an entity class.

**Key Requirements**:
- Every entity must have a primary key
- Must be declared on the entity class that is the root of the entity hierarchy, or on a mapped superclass
- Can be applied to FIELD or METHOD
- Runtime retention

**Valid Types** (from spec):
- Java primitive types or wrappers
- String
- UUID
- LocalDate
- Date (java.util.Date)
- java.sql.Date
- BigDecimal
- BigInteger

**Example**:
```java
@Id
@GeneratedValue
UUID uuid;

// Or with property access
@Id
public String getIsbn() {
    return isbn;
}
```

**Related Annotations**: @Column, @GeneratedValue, @EmbeddedId, @IdClass

---

## 2. @GeneratedValue Annotation

**Purpose**: Specifies a generation strategy for generated primary keys.

**Key Requirements**:
- Can be applied to primary key property/field of entity or mapped superclass
- Only required for simple primary keys (not derived primary keys)
- Works in conjunction with @Id

**Annotation Members**:
- `strategy()`: GenerationType enum, default = AUTO
- `generator()`: String, default = "" (entity name)

**Example**:
```java
@Id
@GeneratedValue(strategy = SEQUENCE, generator = "CUST_SEQ")
@Column(name = "CUST_ID")
public Long getId() { return id; }

@Id
@GeneratedValue(strategy = TABLE, generator = "CUST_GEN")
@Column(name = "CUST_ID")
Long id;
```

**Related Annotations**: @Id, @SequenceGenerator, @TableGenerator

---

## 3. GenerationType Enum

**Strategies**:

1. **TABLE**: Uses database table to ensure uniqueness
   - Supports: Long, Integer, long, int
   - Requires @TableGenerator

2. **SEQUENCE**: Uses database sequence
   - Supports: Long, Integer, long, int
   - Requires @SequenceGenerator

3. **IDENTITY**: Uses database identity column
   - Supports: Long, Integer, long, int

4. **UUID**: Generates RFC 4122 UUID
   - Supports: UUID, String

5. **AUTO**: Provider picks appropriate strategy
   - For UUID/String: equivalent to UUID
   - For Long/Integer: selects TABLE, SEQUENCE, or IDENTITY

---

## 4. APT (Annotation Processing Tool) Requirements

**StaticMetamodel Generation**:
- Generated class: `<Entity>_<id>`
- Must include `@Generated("jakarta.persistence.StaticMetamodel")`
- Since 3.2: adds constants for managed types, named queries, named graphs, named result set mappings

**APT Processing for @Id/@GeneratedValue**:
- Parse @Id to identify primary key fields/properties
- Parse @GeneratedValue to determine generation strategy
- For SEQUENCE strategy: process @SequenceGenerator configuration
- For TABLE strategy: process @TableGenerator configuration
- Generate entity metadata classes with primary key information
- Generate support classes for entity state management

**Output Requirements**:
- Entity metadata with PK field details
- Strategy-specific generator configuration
- Proper type information for primary key

---

## 5. Related Annotations

### @Column
- Specifies column mapped by persistent field/property
- Members: name, unique, nullable, insertable, updatable, columnDefinition, options, table, length, precision, scale, secondPrecision, check, comment
- If not specified, defaults apply (name = property/field name)

### @Temporal (DEPRECATED in 3.2)
- For Date/Calendar types
- Values: DATE, TIME, TIMESTAMP
- Should use java.time types in new code

### @Enumerated
- For enum types
- Values: ORDINAL (default), STRING
- Alternative: @EnumeratedValue annotation for custom mapping

### Additional Related Annotations:
- @EmbeddedId: For composite keys (embeddable type)
- @IdClass: For composite keys (separate ID class)
- @AttributeOverride: Override column mappings in embeddable
- @MapsId: For derived primary keys

---

## 6. Code Examples from JPA Spec

### Example 1: Auto-generated UUID
```java
@Id 
@GeneratedValue
UUID uuid;
```

### Example 2: Sequence with explicit generator
```java
@Id
@GeneratedValue(strategy = SEQUENCE, generator = "CUST_SEQ")
@Column(name = "CUST_ID")
public Long getId() { return id; }

@SequenceGenerator(name = "CUST_SEQ", sequenceName = "CUSTOMER_SEQ")
```

### Example 3: Table-based generation
```java
@Id
@GeneratedValue(strategy = TABLE, generator = "CUST_GEN")
@Column(name = "CUST_ID")
Long id;

@TableGenerator(
    name = "CUST_GEN",
    table = "ID_GEN",
    pkColumnName = "GEN_KEY",
    valueColumnName = "GEN_VALUE",
    pkColumnValue = "CUST_ID"
)
```

### Example 4: Combined with @Column
```java
@Column(name = "DESC", nullable = false, length = 512)
public String getDescription() { return description; }

@Column(name = "ORDER_COST", updatable = false, precision = 12, scale = 2)
public BigDecimal getCost() { return cost; }
```

### Example 5: Enumerated field
```java
@Entity
public class Employee {
    @Enumerated(STRING)
    public SalaryRate getPayScale() { ... }
}

public enum SalaryRate {JUNIOR, SENIOR, MANAGER, EXECUTIVE}
```

---

## 7. Implementation Notes for mansart-persistence

**APT Processor Requirements**:
1. Parse @Id annotation → identify primary key field/property
2. Parse @GeneratedValue → determine GenerationType strategy
3. For SEQUENCE/TABLE strategies → collect generator configuration
4. Generate EntityMetadata with PK information
5. Generate StaticMetamodel with primary key attribute
6. Updatemansart-persistence-processor in M7-10 to handle these annotations
7. Support all GenerationType strategies in metadata generation
8. Integrate with existing entity mappings (M7-12)

**Primary Key Type Handling**:
- UUID: Use GenerationType.UUID or AUTO
- Long/Integer: Use AUTO, TABLE, SEQUENCE, or IDENTITY
- String: Use Generat
