package com.example.ordermanager.vendor.migration;

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

/** Verifies additive Vendor PO migrations against a disposable PostgreSQL schema. */
class VendorPoMigrationPostgresTest {

  /** Applies V34–V36 to a legacy-shaped schema and verifies snapshots, backfill, and decimals. */
  @Test
  @EnabledIfSystemProperty(named = "vendorPoMigrationTest", matches = "true")
  void v34ThroughV36BackfillLegacyPoAndAddDecimalSnapshots() throws Exception {
    String schema = "vendor_po_migration_" + UUID.randomUUID().toString().replace("-", "");
    String url =
        System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/order_manager_db");
    String username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
    String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
    try (Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
      statement.execute("SET search_path TO " + schema);
      statement.execute(
          "CREATE TABLE companies (id BIGINT PRIMARY KEY, name VARCHAR(255), gstn VARCHAR(50), "
              + "logo_data BYTEA, logo_content_type VARCHAR(100), signature_data BYTEA, signature_content_type VARCHAR(100), "
              + "vendor_po_terms_conditions TEXT, vendor_po_terms_conditions_enabled BOOLEAN)");
      statement.execute(
          "CREATE TABLE company_addresses (id BIGINT PRIMARY KEY, company_id BIGINT REFERENCES companies(id), "
              + "nickname VARCHAR(120), address_line1 TEXT, address_line2 TEXT, city VARCHAR(120), state VARCHAR(100), "
              + "postal_code VARCHAR(20), country VARCHAR(100), gstin VARCHAR(50), drug_license_number VARCHAR(100), "
              + "active BOOLEAN, default_bill_to BOOLEAN, default_ship_to BOOLEAN)");
      statement.execute(
          "CREATE TABLE vendors (id BIGINT PRIMARY KEY, company_name VARCHAR(255), contact_person VARCHAR(255), "
              + "address TEXT, email VARCHAR(255), phone VARCHAR(50), gstn VARCHAR(50), state VARCHAR(100))");
      statement.execute(
          "CREATE TABLE vendor_purchase_order (id BIGINT PRIMARY KEY, company_id BIGINT, vendor_id BIGINT, "
              + "po_number VARCHAR(100), created_at TIMESTAMPTZ, total_amount DOUBLE PRECISION)");
      statement.execute(
          "CREATE UNIQUE INDEX ux_vendor_po_company_po_number ON vendor_purchase_order(company_id, po_number)");
      statement.execute(
          "CREATE TABLE vendor_purchase_order_item (id BIGINT PRIMARY KEY, vendor_po_id BIGINT, quantity DOUBLE PRECISION, "
              + "unit_price DOUBLE PRECISION, line_total DOUBLE PRECISION, pre_gst_unit_price NUMERIC(12,2), gst_amount NUMERIC(12,2), gst_percentage NUMERIC(5,2))");
      statement.execute(
          "INSERT INTO companies VALUES (1,'Legacy Buyer','27ABCDE1234F1Z5',NULL,NULL,NULL,NULL,'[]',TRUE)");
      statement.execute(
          "INSERT INTO company_addresses VALUES (1,1,'Main','Frozen Rd',NULL,'Mumbai','MAHARASHTRA','400001','India',NULL,'DL-9',TRUE,TRUE,TRUE)");
      statement.execute(
          "INSERT INTO vendors VALUES (1,'Legacy Vendor','Buyer Contact','Vendor Rd','vendor@example.com','9999999999','27ABCDE1234F1Z5','MAHARASHTRA')");
      statement.execute("INSERT INTO vendor_purchase_order VALUES (1,1,1,'PO-001',now(),118)");
      statement.execute("INSERT INTO vendor_purchase_order_item VALUES (1,1,2,59,118,50,18,18)");
      for (String migration : new String[] {
          "db/migration/V34__vendor_po_snapshots_commercial_and_revisions.sql",
          "db/migration/V35__vendor_po_decimal_financial_snapshots.sql",
          "db/migration/V36__persist_vendor_po_taxable_item_rate.sql"}) {
        ScriptUtils.executeSqlScript(connection,
            new EncodedResource(new ClassPathResource(migration)));
      }
      try (ResultSet row = statement
          .executeQuery("SELECT snapshot_origin, buyer_company_name, bill_to_drug_license_number, "
              + "revision_number, total_amount FROM vendor_purchase_order WHERE id=1")) {
        row.next();
        assertThat(row.getString(1)).isEqualTo("LEGACY_BACKFILL");
        assertThat(row.getString(2)).isEqualTo("Legacy Buyer");
        assertThat(row.getString(3)).isEqualTo("DL-9");
        assertThat(row.getInt(4)).isZero();
        assertThat(row.getBigDecimal(5)).isEqualByComparingTo("118.00");
      }
      try (ResultSet row = statement.executeQuery(
          "SELECT taxable_rate, taxable_amount, line_total, pg_typeof(quantity)::text "
              + "FROM vendor_purchase_order_item WHERE id=1")) {
        row.next();
        assertThat(row.getBigDecimal(1)).isEqualByComparingTo("50.0000");
        assertThat(row.getBigDecimal(2)).isEqualByComparingTo("100.00");
        assertThat(row.getBigDecimal(3)).isEqualByComparingTo("118.00");
        assertThat(row.getString(4)).isEqualTo("numeric");
      }
    } finally {
      try (Connection cleanup = DriverManager.getConnection(url, username, password);
          Statement statement = cleanup.createStatement()) {
        statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
      }
    }
  }
}
