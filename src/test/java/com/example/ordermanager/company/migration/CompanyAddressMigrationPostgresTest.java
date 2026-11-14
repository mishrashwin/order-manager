package com.example.ordermanager.company.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** PostgreSQL verification for the Company Address legacy backfill migration. */
class CompanyAddressMigrationPostgresTest {

  /**
   * Applies V32 and V33 in isolation and verifies legacy defaults plus the optional license column.
   */
  @Test
  @EnabledIfSystemProperty(named = "companyAddressMigrationTest", matches = "true")
  void v32_backfillsLegacyAddressAndActiveDefaults() throws Exception {
    String schema = "company_address_test_" + UUID.randomUUID().toString().replace("-", "");
    String url =
        System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/order_manager_db");
    String username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
    String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

    try (Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
      statement.execute("SET search_path TO " + schema);
      statement.execute("CREATE TABLE companies (id BIGINT PRIMARY KEY, name VARCHAR(255), "
          + "address TEXT, gstn VARCHAR(50), state VARCHAR(100))");
      statement.execute("INSERT INTO companies (id, name, address, gstn, state) VALUES "
          + "(1, 'Legacy Co', '17 Old Road, Mumbai', '27ABCDE1234F1Z5', 'MAHARASHTRA'), "
          + "(2, 'Empty Legacy Co', NULL, NULL, NULL)");

      ScriptUtils.executeSqlScript(connection, new EncodedResource(
          new ClassPathResource("db/migration/V32__add_company_addresses.sql")));
      ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource(
          "db/migration/V33__add_drug_license_number_to_company_addresses.sql")));

      try (ResultSet rows = statement
          .executeQuery("SELECT count(*), count(*) FILTER (WHERE active AND default_bill_to), "
              + "count(*) FILTER (WHERE active AND default_ship_to) FROM company_addresses")) {
        rows.next();
        assertThat(rows.getInt(1)).isEqualTo(2);
        assertThat(rows.getInt(2)).isEqualTo(2);
        assertThat(rows.getInt(3)).isEqualTo(2);
      }
      try (ResultSet rows = statement.executeQuery(
          "SELECT count(*) FROM company_addresses a JOIN companies c ON c.id = a.company_id "
              + "WHERE a.nickname = 'Legacy Address' "
              + "AND (a.address_line1 IS DISTINCT FROM c.address "
              + "OR a.gstin IS DISTINCT FROM c.gstn OR a.state IS DISTINCT FROM c.state)")) {
        rows.next();
        assertThat(rows.getInt(1)).isZero();
      }
      try (ResultSet rows = statement
          .executeQuery("SELECT drug_license_number FROM company_addresses WHERE company_id = 1")) {
        rows.next();
        assertThat(rows.getString(1)).isNull();
      }
    } finally {
      try (Connection cleanup = DriverManager.getConnection(url, username, password);
          Statement statement = cleanup.createStatement()) {
        statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
      }
    }
  }
}
