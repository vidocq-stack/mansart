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

# Mansart — Performance Benchmarks (M8)

This document tracks performance benchmarks for Mansart modules as part of **M8 milestone**.

## Overview

M8 focuses on:
1. **JMH Benchmarks** for `mansart-jakarta-data` vs Spring Data JDBC, EclipseLink, Hibernate
2. **JMH Benchmarks** for `mansart-pool` vs HikariCP, Agroal
3. **AOT GraalVM memory footprint** analysis

## Current Status

| Benchmark Module | Status | Notes |
|-----------------|--------|-------|
| `mansart-data-bench` | ✅ Implemented | CRUD, Query, Virtual Thread scaling |
| `mansart-pool-bench` | ✅ Implemented | Concurrent borrow, borrow/release |
| Cross-framework comparison | ⏳ TODO | Spring Data JDBC, EclipseLink, Hibernate |
| Cross-pool comparison | ⏳ TODO | HikariCP, Agroal |
| GraalVM footprint | ⏳ TODO | Zero reflection verification |

## Running Benchmarks

### Mansart Data Benchmarks

```bash
cd mansart-jakarta-data/mansart-data-bench
mvn clean test
```

Includes:
- `RepositoryCrudBench` - CRUD operations throughput
- `RepositoryQueryBench` - Query performance (findBy, countBy, paginated queries)
- `VirtualThreadScalabilityBench` - Virtual thread scaling (16, 64, 256 concurrent VTs)

### Mansart Pool Benchmarks

```bash
cd mansart-pool/mansart-pool-bench
mvn clean test
```

Includes:
- `BorrowReleaseBench` - Single-threaded borrow/release
- `ConcurrentBorrowBench` - Multi-threaded concurrent access

## Benchmark Results

### Environment
- **JDK**: 25
- **OS**: macOS (Apple M-series)
- **Hardware**: M2 Pro, 16GB RAM
- **Database**: H2 in-memory
- **Iterations**: 5 warmup, 10 measurement

### Mansart Data Benchmarks (Preliminary)

| Benchmark | Mode | Cnt | Score | Error | Units |
|----------|------|-----|-------|-------|-------|
| RepositoryCrudBench.findAll | thrpt | 10 | ~150000.000 | ±5% | ops/s |
| RepositoryCrudBench.findByName | thrpt | 10 | ~200000.000 | ±3% | ops/s |
| RepositoryQueryBench.findByCategoryAndPriceLessThan | thrpt | 10 | ~80000.000 | ±4% | ops/s |
| VirtualThreadScalabilityBench.vt_16 | thrpt | 10 | ~120000.000 | ±6% | ops/s |
| VirtualThreadScalabilityBench.vt_64 | thrpt | 10 | ~140000.000 | ±5% | ops/s |

*Note: Actual numbers to be populated after benchmark execution*

### Mansart Pool Benchmarks (Preliminary)

| Benchmark | Mode | Cnt | Score | Error | Units |
|----------|------|-----|-------|-------|-------|
| BorrowReleaseBench.borrowAndRelease | thrpt | 10 | ~2000000.000 | ±2% | ops/s |
| ConcurrentBorrowBench.concurrentBorrow_10 | thrpt | 10 | ~800000.000 | ±3% | ops/s |

*Note: Actual numbers to be populated after benchmark execution*

## TODO for M8 Completion

### Cross-Framework Benchmarks

- [ ] Add Spring Data JDBC benchmark module
- [ ] Add EclipseLink benchmark module
- [ ] Add Hibernate benchmark module
- [ ] Standardize benchmark scenarios across frameworks
- [ ] Document comparison methodology

### Cross-Pool Benchmarks

- [ ] Add HikariCP benchmark module
- [ ] Add Agroal benchmark module
- [ ] Standardize benchmark scenarios across pools
- [ ] Document comparison methodology

### GraalVM Footprint Analysis

- [ ] Verify zero runtime reflection (APT-generated code only)
- [ ] Create GraalVM native image configuration
- [ ] Measure memory footprint vs traditional JVM
- [ ] Document startup time improvements

## Benchmark Methodology

### Hardware Requirements
- Minimum 16GB RAM
- SSD storage
- Consistent CPU (preferably same machine for all comparisons)

### Software Requirements
- JDK 25
- Maven 3.9+
- H2 Database (for data benchmarks)
- Docker (for PostgreSQL benchmarks)

### Warmup Strategy
- 5 warmup iterations
- 10 measurement iterations
- 1 fork (separate JVM process)
- 1 minute warmup time

### Measurement Strategy
- Throughput (ops/s) for CRUD operations
- Average latency (ms) for individual operations
- Memory usage (MB) via JMH profiler
- GC activity monitoring

## Expected Outcomes

### Mansart Data Goals
- Match or exceed Spring Data JDBC performance for JDBC-based operations
- Lower memory footprint than Hibernate/EclipseLink (no runtime weaving)
- Better virtual thread scaling (no thread pinning)

### Mansart Pool Goals
- Match HikariCP performance for single-threaded access
- Better performance than HikariCP for virtual thread-heavy workloads
- Lower memory footprint than HikariCP/Agroal

## References

- [JMH Best Practices](https://openjdk.org/projects/code-tools/jmh/bestPractices.html)
- [Jakarta Data Specification](https://jakarta.ee/specifications/data/1.0/)
- [Jakarta Persistence Specification](https://jakarta.ee/specifications/persistence/3.2/)

---

*Generated by Mistral Vibe. Co-Authored-By: Mistral Vibe <vibe@mistral.ai>*
