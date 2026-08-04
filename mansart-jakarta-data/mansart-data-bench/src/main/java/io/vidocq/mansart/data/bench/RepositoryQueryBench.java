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

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
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
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * JMH benchmarks for complex query operations in Mansart Data.
 * Tests pagination, sorting, aggregation, and join operations.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Fork(1)
public class RepositoryQueryBench {

    private DataSource dataSource;
    private MansartData mansartData;

    // Entity for main queries
    private record Product(Long id, String name, String category, double price, int stock) {}

    // Entity for join queries
    private record PurchaseOrder(Long id, Long productId, int quantity, String status) {}

    // Entity for aggregation queries
    private record Transaction(Long id, double amount, String type, long timestamp) {}

    private interface ProductRepository extends BasicRepository<Product, Long> {
        List<Product> findByCategory(String category);
        List<Product> findByPriceGreaterThan(double price);
        List<Product> findByCategoryAndPriceLessThan(String category, double price);
        long countByCategory(String category);
        List<Product> findByCategoryOrderByPriceAsc(String category);
        List<Product> findByCategoryOrderByPriceDesc(String category);
        Page<Product> findByCategory(String category, PageRequest pageRequest, jakarta.data.Order<Product> order);
    }

    private interface PurchaseOrderRepository extends BasicRepository<PurchaseOrder, Long> {
        List<PurchaseOrder> findByProductId(Long productId);
        List<PurchaseOrder> findByStatus(String status);
    }

    private interface TransactionRepository extends BasicRepository<Transaction, Long> {
        List<Transaction> findByType(String type);
        long countByType(String type);
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
            // Create tables
            stmt.execute("CREATE TABLE IF NOT EXISTS product (" +
                        "id BIGINT PRIMARY KEY, " +
                        "name VARCHAR(255), " +
                        "category VARCHAR(100), " +
                        "price DOUBLE, " +
                        "stock INT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS purchase_order (" +
                        "id BIGINT PRIMARY KEY, " +
                        "product_id BIGINT, " +
                        "quantity INT, " +
                        "status VARCHAR(50), " +
                        "FOREIGN KEY (product_id) REFERENCES product(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS transaction (" +
                        "id BIGINT PRIMARY KEY, " +
                        "amount DOUBLE, " +
                        "type VARCHAR(50), " +
                        "timestamp BIGINT)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_product_category ON product(category)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_product_price ON product(price)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_purchase_order_product ON purchase_order(product_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_purchase_order_status ON purchase_order(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transaction_type ON transaction(type)");

            // Insert 10,000 products
            for (long i = 1; i <= 10000; i++) {
                String category = i % 3 == 0 ? "Electronics" : i % 3 == 1 ? "Clothing" : "Books";
                stmt.execute(String.format(
                    "INSERT INTO product (id, name, category, price, stock) VALUES (%d, 'Product_%d', '%s', %.2f, %d)",
                    i, i, category, ThreadLocalRandom.current().nextDouble(10, 1000), 
                    ThreadLocalRandom.current().nextInt(1, 100)));
            }

            // Insert 50,000 purchase orders (5 per product on average)
            for (long i = 1; i <= 50000; i++) {
                long productId = ThreadLocalRandom.current().nextLong(1, 10000);
                String status = i % 4 == 0 ? "COMPLETED" : i % 4 == 1 ? "PENDING" : i % 4 == 2 ? "SHIPPED" : "CANCELLED";
                stmt.execute(String.format(
                    "INSERT INTO purchase_order (id, product_id, quantity, status) VALUES (%d, %d, %d, '%s')",
                    i, productId, ThreadLocalRandom.current().nextInt(1, 10), status));
            }

            // Insert 100,000 transactions
            for (long i = 1; i <= 100000; i++) {
                String type = i % 3 == 0 ? "DEPOSIT" : i % 3 == 1 ? "WITHDRAWAL" : "TRANSFER";
                stmt.execute(String.format(
                    "INSERT INTO transaction (id, amount, type, timestamp) VALUES (%d, %.2f, '%s', %d)",
                    i, ThreadLocalRandom.current().nextDouble(1, 10000), type, System.currentTimeMillis()));
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
                stmt.execute("DROP TABLE IF EXISTS transaction");
                stmt.execute("DROP TABLE IF EXISTS purchase_order");
                stmt.execute("DROP TABLE IF EXISTS product");
            }
        }
    }

    // ==================== Pagination Benchmarks ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public Page<Product> findAllWithPagination() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            jakarta.data.Order<Product> order = jakarta.data.Order.by(Sort.asc("id"));
            return repo.findAll(PageRequest.ofPage(0, 10, true), order);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public Page<Product> findByCategoryWithPagination() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            String category = ThreadLocalRandom.current().nextInt(0, 3) == 0 ? "Electronics" : 
                            ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "Clothing" : "Books";
            jakarta.data.Order<Product> order = jakarta.data.Order.by(Sort.asc("id"));
            return repo.findByCategory(category, PageRequest.ofPage(0, 20, true), order);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Sorting Benchmarks ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<Product> findByCategoryWithSortingAsc() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            return repo.findByCategoryOrderByPriceAsc("Electronics");
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<Product> findByCategoryWithSortingDesc() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            return repo.findByCategoryOrderByPriceDesc("Electronics");
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Multiple Conditions Benchmarks ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<Product> findByCategoryAndPriceLessThan() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            String category = ThreadLocalRandom.current().nextInt(0, 3) == 0 ? "Electronics" : 
                            ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "Clothing" : "Books";
            double maxPrice = ThreadLocalRandom.current().nextDouble(100, 500);
            return repo.findByCategoryAndPriceLessThan(category, maxPrice);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Aggregation Benchmarks ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public long countByCategory() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            String category = ThreadLocalRandom.current().nextInt(0, 3) == 0 ? "Electronics" : 
                            ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "Clothing" : "Books";
            return repo.countByCategory(category);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Stream and FindAll Benchmarks ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public Stream<Product> findAllAsStream() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            return repo.findAll();
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<Product> findAllAsList() {
        try {
            ProductRepository repo = mansartData.repository(ProductRepository.class);
            return repo.findAll().limit(100).toList();
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Join-like Query Benchmarks (via separate lookups) ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<PurchaseOrder> findPurchaseOrdersByProductId() {
        try {
            PurchaseOrderRepository repo = mansartData.repository(PurchaseOrderRepository.class);
            long productId = ThreadLocalRandom.current().nextLong(1, 10000);
            return repo.findByProductId(productId);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<PurchaseOrder> findPurchaseOrdersByStatus() {
        try {
            PurchaseOrderRepository repo = mansartData.repository(PurchaseOrderRepository.class);
            String status = ThreadLocalRandom.current().nextInt(0, 4) == 0 ? "COMPLETED" : 
                          ThreadLocalRandom.current().nextInt(0, 3) == 0 ? "PENDING" : 
                          ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "SHIPPED" : "CANCELLED";
            return repo.findByStatus(status);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Transaction Aggregation Benchmarks ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public long countTransactionsByType() {
        try {
            TransactionRepository repo = mansartData.repository(TransactionRepository.class);
            String type = ThreadLocalRandom.current().nextInt(0, 3) == 0 ? "DEPOSIT" : 
                         ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "WITHDRAWAL" : "TRANSFER";
            return repo.countByType(type);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public List<Transaction> findTransactionsByType() {
        try {
            TransactionRepository repo = mansartData.repository(TransactionRepository.class);
            String type = "DEPOSIT";
            return repo.findByType(type);
        } catch (MansartDataException e) {
            throw new RuntimeException(e);
        }
    }
}
