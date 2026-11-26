package com.example.ordermanager.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordermanager.vendor.service.impl.VendorPoNumberSequence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies PostgreSQL sequence backfill, tenant isolation, and concurrent allocations. */
class VendorPoNumberSequencePostgresTest {

  /** Exercises gaps, prefix isolation, deleted allocations, and simultaneous counter updates. */
  @Test
  @EnabledIfSystemProperty(named = "vendorPoSequenceTest", matches = "true")
  void allocatesGapSafeCompanyScopedNumbersConcurrently() throws Exception {
    String schema = "vendor_po_sequence_" + UUID.randomUUID().toString().replace("-", "");
    String baseUrl =
        System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/order_manager_db");
    String username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
    String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
    try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
      statement.execute("SET search_path TO " + schema);
      statement.execute("CREATE TABLE companies (id BIGINT PRIMARY KEY)");
      statement.execute("INSERT INTO companies VALUES (2),(4),(6)");
      statement
          .execute("CREATE TABLE vendor_purchase_order (company_id BIGINT, po_number VARCHAR(100), "
              + "revision_number INTEGER NOT NULL DEFAULT 0)");
      statement.execute("INSERT INTO vendor_purchase_order VALUES "
          + "(2,'APL001',0),(6,'LIPL001',0),(6,'LIPL002',0),(6,'LIPL003',0),"
          + "(6,'LIPL004',0),(6,'LIPL005',0)");
      statement.execute("INSERT INTO vendor_purchase_order VALUES "
          + "(4,'LE001'),(4,'LE002'),(4,'LE003'),(4,'LE004'),(4,'LE005'),(4,'LE006'),"
          + "(4,'LE007'),(4,'LE008'),(4,'LE009'),(4,'LE010'),(4,'LE011'),(4,'LE013')");
      statement.execute("INSERT INTO vendor_purchase_order VALUES (4,'LE013',1)");
      ScriptUtils.executeSqlScript(connection, new EncodedResource(
          new ClassPathResource("db/migration/V39__vendor_po_number_sequences.sql")));

      SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
      dataSource.setDriver(new org.postgresql.Driver());
      dataSource.setUrl(baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema);
      dataSource.setUsername(username);
      dataSource.setPassword(password);
      VendorPoNumberSequence sequence = new VendorPoNumberSequence(new JdbcTemplate(dataSource));

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      assertThat(jdbcTemplate.queryForObject("SELECT last_value FROM vendor_po_number_sequence "
          + "WHERE company_id=2 AND prefix='APL'", Long.class)).isEqualTo(1L);
      assertThat(jdbcTemplate.queryForObject("SELECT last_value FROM vendor_po_number_sequence "
          + "WHERE company_id=4 AND prefix='LE'", Long.class)).isEqualTo(13L);
      assertThat(jdbcTemplate.queryForObject("SELECT last_value FROM vendor_po_number_sequence "
          + "WHERE company_id=6 AND prefix='LIPL'", Long.class)).isEqualTo(5L);
      assertThat(sequence.nextNumber(2L, "APL")).isEqualTo(2L);
      assertThat(sequence.nextNumber(4L, "LE")).isEqualTo(14L);
      assertThat(sequence.nextNumber(6L, "LIPL")).isEqualTo(6L);

      statement.execute("INSERT INTO vendor_purchase_order VALUES "
          + "(4,'A.P001',0),(4,'AXP099',0),(4,'A.P007x',0)");
      assertThat(sequence.nextNumber(4L, "A.P")).isEqualTo(2L);

      jdbcTemplate.update("UPDATE vendor_po_number_sequence SET last_value=40 "
          + "WHERE company_id=2 AND prefix='APL'");
      assertThat(sequence.nextNumber(2L, "APL")).isEqualTo(41L);
      jdbcTemplate.update(
          "INSERT INTO vendor_purchase_order (company_id, po_number) " + "VALUES (2, 'APL041')");
      jdbcTemplate.update(
          "DELETE FROM vendor_purchase_order WHERE company_id=2 " + "AND po_number='APL041'");

      TransactionTemplate transaction =
          new TransactionTemplate(new DataSourceTransactionManager(dataSource));
      transaction.execute(status -> {
        assertThat(sequence.nextNumber(2L, "APL")).isEqualTo(42L);
        status.setRollbackOnly();
        return null;
      });
      assertThat(sequence.nextNumber(2L, "APL")).isEqualTo(42L);

      var executor = Executors.newFixedThreadPool(12);
      try {
        List<Callable<Long>> allocations = new ArrayList<>();
        for (int index = 0; index < 48; index++) {
          allocations.add(() -> sequence.nextNumber(4L, "LE"));
        }
        Set<Long> allocated = executor.invokeAll(allocations).stream().map(future -> {
          try {
            return future.get();
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        }).collect(Collectors.toSet());
        Set<Long> expected = LongStream.rangeClosed(15L, 62L).boxed().collect(Collectors.toSet());
        assertThat(allocated).isEqualTo(expected);
      } finally {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    } finally {
      try (Connection cleanup = DriverManager.getConnection(baseUrl, username, password);
          Statement statement = cleanup.createStatement()) {
        statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
      }
    }
  }
}
