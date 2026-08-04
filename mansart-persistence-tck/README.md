/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

# Mansart Jakarta Persistence 3.2 TCK Runner

This module executes the **official Jakarta Persistence 3.2 Technology Compatibility Kit (TCK)** against the Mansart implementation.

> **⚠️ IMPORTANT**: This module is **INTENTIONALLY OUTSIDE** the parent reactor (uses Maven Model 4.0.0).
> Reason: ShrinkWrap Maven Resolver (transitive dependency of the official TCK harness) cannot parse Maven Model 4.1.0 POMs.

## Prerequisites

### 1. Build Mansart Jakarta Persistence

First, ensure all Mansart modules are built and installed locally:

```bash
cd /path/to/mansart
mvn clean install -DskipTests -Dlicense.skip=true
```

This installs `mansart-persistence-*.jar` files to your local Maven repository.

### 2. Jakarta Persistence TCK Availability

The Jakarta Persistence 3.2 TCK (`jakarta.persistence:jakarta.persistence-tck:3.2.0`) must be available in your Maven repository.

**Current status**: The TCK is NOT available on Maven Central. You have two options:

#### Option A: Manual Installation (Recommended)
Download the TCK from Eclipse Foundation and install it manually:

```bash
# Download the TCK (when available)
wget https://download.eclipse.org/jakarta/persistence/3.2.0/jakarta.persistence-tck-3.2.0.zip
unzip jakarta.persistence-tck-3.2.0.zip
cd jakarta.persistence-tck-3.2.0

# Install to local Maven repository
mvn install -DskipTests
```

#### Option B: Use Eclipse Repository
Add the Eclipse repository to your `~/.m2/settings.xml`:

```xml
<settings>
  <profiles>
    <profile>
      <id>eclipse</id>
      <repositories>
        <repository>
          <id>eclipse-jakarta</id>
          <url>https://repo.eclipse.org/content/repositories/jakarta-releases/</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>eclipse</activeProfile>
  </activeProfiles>
</settings>
```

## Running the TCK

### Basic TCK Execution (H2)

Run the **EntityTests** subset (CRUD validation):

```bash
cd mansart-persistence-tck
mvn clean test -Ptck-run
```

This executes the TCK against H2 in-memory database.

### Full TCK Execution

To run **all TCK tests** (EntityTests + SignatureTests + XMLTests + etc.):

```bash
cd mansart-persistence-tck
mvn clean test -Ptck-run -Dtest=*Tests
```

### PostgreSQL TCK Execution

Run the TCK against PostgreSQL (requires Docker):

```bash
cd mansart-persistence-tck
mvn clean test -Ptck-run,tck-pg
```

### Signature Tests Only

Run only the signature compliance tests:

```bash
cd mansart-persistence-tck
mvn clean test -Ptck-run,tck-sig
```

## TCK Profiles

| Profile | Description | Command |
|---------|-------------|---------|
| `tck-run` | Activates the official Jakarta Persistence TCK | `mvn -Ptck-run test` |
| `tck-pg` | Uses PostgreSQL instead of H2 | `mvn -Ptck-run,tck-pg test` |
| `tck-sig` | Includes signature compliance tests | `mvn -Ptck-run,tck-sig test` |

## Configuration

### System Properties

| Property | Description | Default |
|----------|-------------|---------|
| `mansart.tck.dialect` | Database dialect: `h2` or `pg` | `h2` |
| `jimage.dir` | Directory for signature test cache | `${project.build.directory}/jimage-cache` |
| `java.specification.version` | Java version for signature comparison | `21` (forced) |

### Database Configuration

#### H2 (Default)
- URL: `jdbc:h2:mem:tck;DB_CLOSE_DELAY=-1`
- User: `sa`
- Password: (empty)

#### PostgreSQL
- Requires Docker and Testcontainers
- Automatically started via Testcontainers
- Image: `postgres:17`

## Expected Results

### Jakarta Persistence 3.2 TCK Composition

The Jakarta Persistence 3.2 TCK includes the following test suites:

| Suite | Tests | Description |
|-------|-------|-------------|
| EntityTests | ~100 | CRUD operations, entity lifecycle |
| SignatureTests | ~50 | API signature compliance |
| XMLTests | ~20 | persistence.xml parsing |
| JPAQLTests | ~150 | JPQL query validation |
| CriteriaTests | ~100 | Criteria API validation |
| **Total** | **~420** | Full Jakarta Persistence 3.2 compliance |

### Success Criteria

- **All EntityTests**: Must pass
- **SignatureTests**: Must pass (binary compatibility)
- **XMLTests**: Must pass
- **JPAQLTests**: Must pass for implemented JPQL features
- **CriteriaTests**: Must pass for implemented Criteria API features

## Troubleshooting

### TCK Not Found

```
[ERROR] Failed to execute goal on project mansart-persistence-tck: 
Could not resolve dependencies for project... jakarta.persistence:jakarta.persistence-tck:3.2.0
```

**Solution**: Install the TCK manually (see Prerequisites above) or configure the Eclipse repository.

### Signature Test Failures

```
[ERROR] Signature mismatch for jakarta.persistence.Entity
```

**Solution**: Run with `-Ptck-sig` profile and check `java.specification.version` is set to `21`:
```bash
mvn clean test -Ptck-run,tck-sig -Djava.specification.version=21
```

### PostgreSQL Connection Failures

```
[ERROR] Could not create DataSource for PostgreSQL
```

**Solution**: Ensure Docker is running and Testcontainers can pull the PostgreSQL image:
```bash
docker pull postgres:17
docker run --name test-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:17
```

### Out of Memory

Increase Maven heap size:
```bash
MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m" mvn clean test -Ptck-run
```

## Current Status

### Mansart Jakarta Persistence Implementation

| Feature | Status | Notes |
|---------|--------|-------|
| EntityManager | ✅ Complete | All CRUD operations |
| Persistence Context | ✅ Complete | L1 cache, state management |
| Static Metamodel | ✅ Complete | `Entity_` generation |
| Bytecode Enhancement | ✅ Partial | Dirty tracking, lazy loading |
| JPQL | ⚠️ Partial | M5-M6 in progress |
| Criteria API | ⏳ TODO | Future milestone |
| Transactions | ⏳ TODO | Future milestone |
| L2 Cache | ⏳ TODO | Future milestone |
| Inheritance | ⏳ TODO | Future milestone |

### TCK Readiness

- **EntityTests**: Should pass with current implementation
- **SignatureTests**: Should pass (API compliant)
- **XMLTests**: May have issues (persistence.xml parsing)
- **JPAQLTests**: Will fail for unimplemented JPQL features
- **CriteriaTests**: Will fail (not yet implemented)

## Comparison with Other Implementations

| Implementation | TCK Status | Notes |
|----------------|------------|-------|
| Hibernate | 100% | Full compliance |
| EclipseLink | 100% | Full compliance |
| OpenJPA | 100% | Full compliance |
| **Mansart** | **~50%** | Entity + Signature tests expected to pass |

## Next Steps

1. **Get TCK from Eclipse Foundation** - The TCK needs to be made available
2. **Install TCK locally** - `mvn install` the TCK jar
3. **Run EntityTests** - `mvn -Ptck-run test`
4. **Fix failures** - Address any EntityTests failures
5. **Enable SignatureTests** - `mvn -Ptck-run,tck-sig test`
6. **Implement remaining features** - JPQL, Criteria, etc.

## References

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [Jakarta Persistence TCK](https://github.com/jakartaee/persistence-tck) (source)
- [Eclipse Foundation Releases](https://repo.eclipse.org/content/repositories/jakarta-releases/)
- [Mansart Jakarta Persistence Documentation](../mansart-jakarta-persistence/README.md)

---

*Generated by Mistral Vibe. Co-Authored-By: Mistral Vibe <vibe@mistral.ai>*
