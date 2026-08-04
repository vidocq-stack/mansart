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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

# M8: Performance & Footprint

## Objectives

M8 focuses on proving Mansart's performance advantages and minimal footprint:

1. **Benchmark against competing implementations** (Spring Data JDBC, EclipseLink, Hibernate)
2. **Virtual thread performance** - demonstrate non-blocking advantages
3. **Memory footprint** - AOT GraalVM compatibility
4. **Pool comparison** - mansart-pool vs HikariCP vs Agroal

## Priority 1: mansart-jakarta-data JMH Benchmarks (High)

### Current State
- `mansart-data-bench` module created
- `RepositoryCrudBench.java` with basic CRUD operations
- JMH 1.37 configured
- Comparison dependencies added (Spring Data JDBC, EclipseLink, Hibernate)

### Target Implementation

#### 1.1 CRUD Benchmarks
- [ ] findById - single entity lookup
- [ ] findAll - full table scan
- [ ] findBy<Field> - indexed lookup
- [ ] save - single entity persistence
- [ ] saveAll - batch persistence
- [ ] deleteById - single entity deletion
- [ ] deleteAll - batch deletion

#### 1.2 Query Benchmarks
- [ ] findAll with pagination
- [ ] findAll with sorting
- [ ] findBy with multiple conditions
- [ ] findBy with complex predicates
- [ ] Aggregation queries (COUNT, MAX, MIN, AVG, SUM)
- [ ] GROUP BY queries
- [ ] JOIN queries

#### 1.3 Virtual Thread Benchmarks
- [ ] Single-thread baseline
- [ ] 4 virtual threads
- [ ] 16 virtual threads
- [ ] 64 virtual threads
- [ ] 256 virtual threads
- [ ] Measure pinning with `-Djdk.tracePinnedThreads=full`

#### 1.4 Comparison Matrix
| Operation | mansart-data | Spring Data JDBC | EclipseLink | Hibernate |
|-----------|--------------|-------------------|-------------|-----------|
| findById | | | | |
| findAll | | | | |
| save | | | | |
| saveAll | | | | |
| Complex Query | | | | |

### Files to Create/Modify
- `RepositoryCrudBench.java` ✅ (created)
- `RepositoryQueryBench.java` (new)
- `VirtualThreadScalabilityBench.java` (new)
- `ComparisonBenchBase.java` (abstract base class for comparison)
- `SpringDataComparisonBench.java` (new)
- `EclipseLinkComparisonBench.java` (new)
- `HibernateComparisonBench.java` (new)

## Priority 2: mansart-pool Benchmarks (High)

### Target Implementation
Extend existing `mansart-pool-bench` module:

#### 2.1 Core Pool Operations
- [ ] borrow connection
- [ ] return connection
- [ ] concurrent borrow/return
- [ ] idle eviction
- [ ] leak detection

#### 2.2 Comparison Benchmarks
- [ ] mansart-pool vs HikariCP
- [ ] mansart-pool vs Agroal
- [ ] Virtual thread scalability (1, 4, 16, 64, 256 VTs)
- [ ] Traditional thread scalability (1, 4, 16 threads)

#### 2.3 Pinning Analysis
- [ ] Run with `-Djdk.tracePinnedThreads=full`
- [ ] Measure pinning count under load
- [ ] Compare with HikariCP (monitors on Connection.close())
- [ ] Verify zero pinning with mansart-pool

### Files to Modify
- `BorrowReleaseBench.java` (extend with comparison)
- `ConcurrentBorrowBench.java` (extend with comparison)
- Add `PoolComparisonBench.java` (new)

## Priority 3: GraalVM AOT Footprint (Medium)

### Current State
- mansart-data-processor uses Class-File API (AOT-friendly)
- No runtime reflection in critical paths
- ServiceLoader used for DialectFactory (can be configured for AOT)

### Target Implementation

#### 3.1 Native Image Configuration
- [ ] Create `META-INF/native-image` configuration files
- [ ] Configure reflection for ServiceLoader
- [ ] Configure resource access
- [ ] Test native compilation

#### 3.2 Footprint Measurement
- [ ] Measure heap usage at startup
- [ ] Measure heap usage after loading entities
- [ ] Measure heap usage after queries
- [ ] Compare with Spring Data JDBC native image

#### 3.3 AOT Validation
- [ ] Verify all reflection is declared in config
- [ ] Verify no Class.forName calls in hot paths
- [ ] Verify no Proxy usage
- [ ] Test with GraalVM native-image

### Files to Create
- `src/main/resources/META-INF/native-image/io.vidocq.mansart.data.core/reflect-config.json`
- `src/main/resources/META-INF/native-image/io.vidocq.mansart.data.core/resource-config.json`
- `src/test/java/io/vidocq/mansart/data/bench/NativeImageTest.java`
- `pom.xml` profile for native-image build

## Priority 4: Benchmark Infrastructure (Medium)

### Current State
- JMH 1.37 configured
- Basic benchmarks created

### Target Implementation

#### 4.1 CI Integration
- [ ] Add benchmark execution to CI pipeline
- [ ] Store benchmark results as artifacts
- [ ] Track performance regression (fail if >5% degradation)
- [ ] Generate comparison reports

#### 4.2 Benchmark Reporting
- [ ] Generate JSON output
- [ ] Generate HTML reports
- [ ] Generate Markdown reports for PR comments
- [ ] Track historical trends

#### 4.3 Documentation
- [ ] BENCH.md with benchmark methodology
- [ ] How to run benchmarks locally
- [ ] How to interpret results
- [ ] Hardware specifications for reference

### Files to Create/Modify
- `.github/workflows/benchmarks.yml` (in vidocq-parent)
- `mansart-data-bench/pom.xml` (add reporting)
- `mansart/benchmarks/` (store historical results)
- `mansart/BENCH.md` (update with M8 results)

## Task Breakdown

### Week 1: mansart-jakarta-data Benchmarks
- [ ] Complete RepositoryCrudBench with all CRUD operations
- [ ] Create RepositoryQueryBench for complex queries
- [ ] Create VirtualThreadScalabilityBench
- [ ] Add Spring Data JDBC comparison benchmarks
- [ ] Verify benchmarks compile and run

### Week 2: mansart-pool Benchmarks
- [ ] Extend existing pool benchmarks with comparison
- [ ] Add HikariCP comparison
- [ ] Add Agroal comparison
- [ ] Add pinning analysis
- [ ] Document results

### Week 3: GraalVM AOT
- [ ] Create native-image configuration files
- [ ] Test native compilation
- [ ] Measure footprint
- [ ] Document AOT setup

### Week 4: Infrastructure & Documentation
- [ ] CI integration for benchmarks
- [ ] Historical tracking
- [ ] Generate reports
- [ ] Update BENCH.md

## Testing Strategy

### Local Testing
```bash
# Run mansart-data benchmarks
cd mansart-jakarta-data/mansart-data-bench
mvn clean compile
mvn -Pbench verify

# Run pool benchmarks
cd mansart-pool/mansart-pool-bench
mvn clean compile
mvn -Pbench verify
```

### Benchmark Methodology
1. **Warmup**: 3 iterations, 1 second each
2. **Measurement**: 5 iterations, 1 second each
3. **Forks**: 1 (for reproducibility)
4. **Threads**: 1, 4, 16, 64, 256 (virtual threads)
5. **Database**: H2 in-memory for consistency
6. **Data size**: 10,000 entities for CRUD, 100,000 for scalability

### Expected Results

| Metric | Target | Comparison |
|--------|--------|------------|
| findById latency | < 5 µs | vs Spring Data JDBC |
| findAll throughput | > 100k ops/s | vs Hibernate |
| VT scalability | < 15% degradation | 1 → 256 VTs |
| Pool pinning | 0 pinned threads | vs HikariCP |
| Native image size | < 50 MB | AOT compiled |

## Dependencies

### New Dependencies
- `org.openjdk.jmh:jmh-core:1.37` - JMH core
- `org.openjdk.jmh:jmh-generator-annprocess:1.37` - JMH annotation processor
- `org.springframework.data:spring-data-jdbc:3.3.0` - Comparison baseline
- `org.eclipse.persistence:eclipselink:4.0.2` - JPA comparison
- `org.hibernate:hibernate-core:6.5.2.Final` - JPA comparison

### Existing Dependencies (reused)
- `com.h2database:h2:2.3.232` - In-memory database
- `io.vidocq.mansart:mansart-data-core:0.3.0-SNAPSHOT`
- `io.vidocq.mansart:mansart-data-dialect-h2:0.3.0-SNAPSHOT`
- `io.vidocq.mansart:mansart-pool-core:0.3.0-SNAPSHOT`

## Success Criteria

- [ ] mansart-data JMH benchmarks run successfully
- [ ] Comparison with Spring Data JDBC implemented
- [ ] Comparison with EclipseLink implemented
- [ ] Comparison with Hibernate implemented
- [ ] mansart-pool vs HikariCP benchmarks
- [ ] mansart-pool vs Agroal benchmarks
- [ ] Virtual thread scalability validated (<15% degradation)
- [ ] GraalVM native-image compilation successful
- [ ] Footprint measurements documented
- [ ] All benchmarks pass in CI
- [ ] No performance regression >5%
- [ ] BENCH.md updated with M8 results

## Next Steps (Post-M8)

- M9: Additional dialect support (MySQL, Oracle)
- M10: DDL migrations integration
- M11: Observability integration (Humboldt)

---

*Generated by Mistral Vibe. Co-Authored-By: Mistral Vibe <vibe@mistral.ai>*
