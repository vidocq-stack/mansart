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

package io.vidocq.mansart.data.bench;

import io.vidocq.mansart.data.core.MansartData;
import io.vidocq.mansart.data.core.MansartDataException;

import jakarta.data.repository.BasicRepository;

import org.h2.jdbcx.JdbcDataSource;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * JMH benchmarks for virtual thread scalability in Mansart Data.
 * Tests performance with 1, 4, 16, 64, and 256 virtual threads.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Fork(1)
public class VirtualThreadScalabilityBench {

    private DataSource dataSource;
    private MansartData mansartData;

    private record TestEntity(Long id, String name, int value, String category) {}

    private interface TestEntityRepository extends BasicRepository<TestEntity, Long> {
        List<TestEntity> findByCategory(String category);
        List<TestEntity> findByValueGreaterThan(int value);
    }

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:bench;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        // Use a larger connection pool for virtual thread tests
        // H2 can handle concurrent connections well
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_entity_vt (" +
                        "id BIGINT PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "value INT, " +
                        "category VARCHAR(100))");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_test_entity_vt_category ON test_entity_vt(category)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_test_entity_vt_value ON test_entity_vt(value)");

            // Insert 100,000 entities for scalability testing
            for (long i = 1; i <= 100000; i++) {
                String category = i % 10 == 0 ? "Category_A" : 
                                i % 10 == 1 ? "Category_B" : 
                                i % 10 == 2 ? "Category_C" : 
                                i % 10 == 3 ? "Category_D" : "Category_E";
                stmt.execute(String.format(
                    "INSERT INTO test_entity_vt (id, name, value, category) VALUES (%d, 'entity_%d', %d, '%s')",
                    i, i, ThreadLocalRandom.current().nextInt(1, 1000), category));
            }
        }

        mansartData = MansartData.builder()
                .dataSource(dataSource)
                .build();
    }

    @TearDown(Level.Trial)
    public void teardown() throws SQLException {
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_entity_vt");
            }
        }
    }

    // ==================== Single Thread Baseline ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("single_thread")
    @GroupThreads(1)
    public TestEntity singleThreadFindById() {
        return executeFindById();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("single_thread")
    @GroupThreads(1)
    public List<TestEntity> singleThreadFindAll() {
        return executeFindAll();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("single_thread")
    @GroupThreads(1)
    public List<TestEntity> singleThreadFindByCategory() {
        return executeFindByCategory();
    }

    // ==================== 4 Virtual Threads ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_4")
    @GroupThreads(4)
    public TestEntity vt4FindById() {
        return executeFindById();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_4")
    @GroupThreads(4)
    public List<TestEntity> vt4FindAll() {
        return executeFindAll();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_4")
    @GroupThreads(4)
    public List<TestEntity> vt4FindByCategory() {
        return executeFindByCategory();
    }

    // ==================== 16 Virtual Threads ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_16")
    @GroupThreads(16)
    public TestEntity vt16FindById() {
        return executeFindById();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_16")
    @GroupThreads(16)
    public List<TestEntity> vt16FindAll() {
        return executeFindAll();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_16")
    @GroupThreads(16)
    public List<TestEntity> vt16FindByCategory() {
        return executeFindByCategory();
    }

    // ==================== 64 Virtual Threads ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_64")
    @GroupThreads(64)
    public TestEntity vt64FindById() {
        return executeFindById();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_64")
    @GroupThreads(64)
    public List<TestEntity> vt64FindAll() {
        return executeFindAll();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_64")
    @GroupThreads(64)
    public List<TestEntity> vt64FindByCategory() {
        return executeFindByCategory();
    }

    // ==================== 256 Virtual Threads ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_256")
    @GroupThreads(256)
    public TestEntity vt256FindById() {
        return executeFindById();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_256")
    @GroupThreads(256)
    public List<TestEntity> vt256FindAll() {
        return executeFindAll();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Group("vt_256")
    @GroupThreads(256)
    public List<TestEntity> vt256FindByCategory() {
        return executeFindByCategory();
    }

    // ==================== Concurrent Virtual Thread Execution ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public long concurrentVirtualThreadExecution() throws Exception {
        int numThreads = 64;
        AtomicLong result = new AtomicLong(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = java.util.stream.IntStream.range(0, numThreads)
                    .mapToObj(i -> (Runnable) () -> {
                        try {
                            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
                            long id = ThreadLocalRandom.current().nextLong(1, 100000);
                            if (repo.findById(id).isPresent()) {
                                result.incrementAndGet();
                            }
                        } catch (MansartDataException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            for (Runnable task : tasks) {
                executor.submit(task);
            }
        }

        return result.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public long concurrentVirtualThreadQueries() throws Exception {
        int numThreads = 64;
        AtomicLong result = new AtomicLong(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = java.util.stream.IntStream.range(0, numThreads)
                    .mapToObj(i -> (Runnable) () -> {
                        try {
                            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
                            String category = "Category_A";
                            result.addAndGet(repo.findByCategory(category).size());
                        } catch (MansartDataException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            for (Runnable task : tasks) {
                executor.submit(task);
            }
        }

        return result.get();
    }

    // ==================== Helper Methods ====================

    private TestEntity executeFindById() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            long id = ThreadLocalRandom.current().nextLong(1, 100000);
            return repo.findById(id).orElse(null);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TestEntity> executeFindAll() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            return repo.findAll().limit(100).toList();
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TestEntity> executeFindByCategory() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            String category = ThreadLocalRandom.current().nextInt(0, 5) == 0 ? "Category_A" :
                            ThreadLocalRandom.current().nextInt(0, 4) == 0 ? "Category_B" :
                            ThreadLocalRandom.current().nextInt(0, 3) == 0 ? "Category_C" :
                            ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "Category_D" : "Category_E";
            return repo.findByCategory(category);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }
}
