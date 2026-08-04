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

package io.vidocq.mansart.data.bench;

import io.vidocq.mansart.data.core.MansartData;
import io.vidocq.mansart.data.core.MansartDataException;

import jakarta.data.repository.BasicRepository;

import org.h2.jdbcx.JdbcDataSource;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * JMH benchmarks for Mansart Data CRUD operations.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Fork(1)
public class RepositoryCrudBench {

    private DataSource dataSource;
    private MansartData mansartData;
    
    private record TestEntity(Long id, String name, int value) {}
    
    private interface TestEntityRepository extends BasicRepository<TestEntity, Long> {
        List<TestEntity> findByName(String name);
        List<TestEntity> findByValueGreaterThan(int value);
    }

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:bench;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;
        
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_entity (" +
                        "id BIGINT PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "value INT)");
            
            for (long i = 1; i <= 10000; i++) {
                stmt.execute(String.format(
                    "INSERT INTO test_entity (id, name, value) VALUES (%d, 'entity_%d', %d)",
                    i, i, ThreadLocalRandom.current().nextInt(1000)));
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
                stmt.execute("DROP TABLE IF EXISTS test_entity");
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public TestEntity findById() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            long id = ThreadLocalRandom.current().nextLong(1, 10000);
            return repo.findById(id).orElse(null);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<TestEntity> findAll() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            return repo.findAll().toList();
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<TestEntity> findByName() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            String name = "entity_" + ThreadLocalRandom.current().nextInt(1, 10000);
            return repo.findByName(name);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<TestEntity> findByValueGreaterThan() {
        try {
            TestEntityRepository repo = mansartData.repository(TestEntityRepository.class);
            int value = ThreadLocalRandom.current().nextInt(100, 500);
            return repo.findByValueGreaterThan(value);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }
}
