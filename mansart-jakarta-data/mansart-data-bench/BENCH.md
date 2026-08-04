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

# Mansart Data JMH Benchmarks

This module contains JMH (Java Microbenchmark Harness) benchmarks for Mansart Jakarta Data implementation.
The benchmarks measure performance of CRUD operations, complex queries, pagination, sorting, and virtual thread scalability.

## Quick Start

### Run all benchmarks

```bash
cd mansart-jakarta-data/mansart-data-bench
mvn clean compile exec:java -Pbench
```

### Run specific benchmarks

```bash
# CRUD operations
java -cp target/classes:... org.openjdk.jmh.Main RepositoryCrudBench

# Complex queries (pagination, sorting, aggregation)
java -cp target/classes:... org.openjdk.jmh.Main RepositoryQueryBench

# Virtual thread scalability
java -cp target/classes:... org.openjdk.jmh.Main VirtualThreadScalabilityBench
```

## Benchmark Modules

### 1. RepositoryCrudBench

Basic CRUD operation benchmarks:

- `findById()` - Single entity lookup by primary key
- `findAll()` - Full table scan returning all entities as Stream
- `findByName()` - Indexed lookup by name field
- `findByValueGreaterThan()` - Range query with condition

**Entity count**: 10,000 entities
**Database**: H2 in-memory
**Warmup**: 3 iterations x 1 second
**Measurement**: 5 iterations x 1 second
**Metric**: Throughput (operations/second)

### 2. RepositoryQueryBench

Complex query operation benchmarks:

#### Pagination
- `findAllWithPagination()` - Paginated results using `PageRequest`
- `findByCategoryWithPagination()` - Paginated results with filter

#### Sorting
- `findByCategoryWithSortingAsc()` - Sorted results ascending by price
- `findByCategoryWithSortingDesc()` - Sorted results descending by price

#### Multiple Conditions
- `findByCategoryAndPriceLessThan()` - Query with multiple predicates

#### Aggregation
- `countByCategory()` - COUNT aggregation

#### Stream Operations
- `findAllAsStream()` - Return results as Stream
- `findAllAsList()` - Return first 100 results as List

#### Join-like Operations
- `findPurchaseOrdersByProductId()` - Find orders by product (simulating join)
- `findPurchaseOrdersByStatus()` - Find orders by status

#### Transaction Operations
- `countTransactionsByType()` - COUNT aggregation on transactions
- `findTransactionsByType()` - Find transactions by type

**Entity counts**:
- Products: 10,000
- Purchase Orders: 50,000
- Transactions: 100,000

**Database**: H2 in-memory with appropriate indexes

### 3. VirtualThreadScalabilityBench

Virtual thread scalability benchmarks measuring performance under concurrent load:

#### Single Thread Baseline
- `singleThreadFindById()` - Baseline for findById
- `singleThreadFindAll()` - Baseline for findAll
- `singleThreadFindByCategory()` - Baseline for findByCategory

#### 4 Virtual Threads
- `vt4FindById()`, `vt4FindAll()`, `vt4FindByCategory()`

#### 16 Virtual Threads
- `vt16FindById()`, `vt16FindAll()`, `vt16FindByCategory()`

#### 64 Virtual Threads
- `vt64FindById()`, `vt64FindAll()`, `vt64FindByCategory()`

#### 256 Virtual Threads
- `vt256FindById()`, `vt256FindAll()`, `vt256FindByCategory()`

#### Concurrent Execution
- `concurrentVirtualThreadExecution()` - 64 VTs executing findById concurrently
- `concurrentVirtualThreadQueries()` - 64 VTs executing findByCategory concurrently

**Entity count**: 100,000 entities
**Database**: H2 in-memory
**Target**: < 15% degradation from 1 thread to 256 threads

## Methodology

### Benchmark Configuration

All benchmarks use the following standard configuration:

```java
@State(Scope.Benchmark)
@OutputTimeUnit(MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Fork(1)
```

### Warmup Strategy

- **3 warmup iterations** of 1 second each
- Allows JIT compilation to optimize hot paths
- Ensures connection pool is warmed up

### Measurement Strategy

- **5 measurement iterations** of 1 second each
- Provides statistically significant results
- Multiple forks for reproducibility (when enabled)

### Database Setup

- **H2 in-memory database** for consistency and reproducibility
- **Separate database per benchmark class** to avoid interference
- **Tables created and populated** before each trial
- **Indexes** created for common query patterns
- **Data cleaned up** after each trial

## Expected Results

### CRUD Operations

| Operation | Target Throughput | Notes |
|-----------|------------------|-------|
| findById | > 100,000 ops/s | Primary key lookup |
| findAll | > 10,000 ops/s | Full table scan, limited to 100 results |
| findByName | > 50,000 ops/s | Indexed field lookup |
| findByValueGreaterThan | > 20,000 ops/s | Range query |

### Query Operations

| Operation | Target Throughput | Notes |
|-----------|------------------|-------|
| findAllWithPagination | > 5,000 ops/s | Page size 10 |
| findByCategoryWithSorting | > 15,000 ops/s | Sorting by price |
| countByCategory | > 50,000 ops/s | Aggregation |
| findByCategoryAndPriceLessThan | > 10,000 ops/s | Multiple conditions |

### Virtual Thread Scalability

| Thread Count | Target Degradation | Notes |
|--------------|-------------------|-------|
| 1 (baseline) | - | Single-thread performance |
| 4 VTs | < 5% | Minimal overhead |
| 16 VTs | < 10% | Good scalability |
| 64 VTs | < 15% | Acceptable scalability |
| 256 VTs | < 20% | Maximum tested |

**Target**: Less than 15% degradation from 1 thread to 256 virtual threads

## Hardware Specifications (Reference)

- **CPU**: Apple M-series or Intel i7/i9
- **Memory**: 16GB+ RAM
- **JDK**: Temurin 25
- **OS**: macOS or Linux

## Comparison with Other Implementations

Future work includes comparison benchmarks against:

- **Spring Data JDBC** - Baseline for comparison
- **EclipseLink** - JPA implementation
- **Hibernate** - JPA implementation

These will be added as separate benchmark classes once dependency resolution is finalized.

## Running Benchmarks in CI

The benchmarks can be integrated into CI pipelines:

```yaml
- name: Run Benchmarks
  run: |
    cd mansart-jakarta-data/mansart-data-bench
    mvn clean compile exec:java -Pbench -Dlicense.skip=true
  env:
    JAVA_OPTS: -Xmx4g
```

Results are output in JSON format to `target/benchmarks.json`.

## Interpreting Results

### Key Metrics

1. **Throughput (ops/s)**: Higher is better. Measures operations per second.
2. **Average Time (ms/op)**: Lower is better. Measures average time per operation.
3. **p95/p99 Latency**: Measures tail latency.

### Comparison Strategy

When comparing Mansart Data with other implementations:

1. Run all benchmarks on the same hardware
2. Use the same database (H2 in-memory)
3. Use the same data volume (10,000 entities for CRUD, 100,000 for scalability)
4. Run multiple iterations and average results
5. Ensure no external interference during benchmark runs

## Troubleshooting

### Dependency Issues

If Spring Data JDBC, EclipseLink, or Hibernate dependencies cannot be resolved:
- Ensure Maven Central repository is accessible
- Run `mvn dependency:resolve` to diagnose issues
- Check dependency versions in pom.xml

### Compilation Errors

- Ensure JDK 25 is installed and configured
- Run `mvn clean compile -Dlicense.skip=true` to skip license checks during development
- Check that all required imports are present

### Benchmark Execution Issues

- Increase heap size: `-Xmx4g`
- Ensure H2 database can handle the connection load
- Check for virtual thread pinning with `-Djdk.tracePinnedThreads=full`

## Next Steps (M8 Roadmap)

1. [x] Complete RepositoryCrudBench with all CRUD operations
2. [x] Create RepositoryQueryBench for complex queries
3. [x] Create VirtualThreadScalabilityBench
4. [ ] Add Spring Data JDBC comparison benchmarks (dependency resolution needed)
5. [ ] Add EclipseLink comparison benchmarks (dependency resolution needed)
6. [ ] Add Hibernate comparison benchmarks (dependency resolution needed)
7. [ ] CI integration for benchmarks
8. [ ] Historical tracking and reporting
9. [ ] mansart-pool benchmarks (Priority 2)
10. [ ] GraalVM native-image footprint (Priority 3)

## References

- [JMH Documentation](https://openjdk.org/projects/code-tools/jmh/)
- [Jakarta Data 1.0 Specification](https://jakarta.ee/specifications/data/1.0/)
- [Mansart Data Documentation](../README.md)

---

*Generated by Mistral Vibe. Co-Authored-By: Mistral Vibe <vibe@mistral.ai>*
