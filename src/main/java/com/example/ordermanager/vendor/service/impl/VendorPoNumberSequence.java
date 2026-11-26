package com.example.ordermanager.vendor.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Allocates transactional PO suffixes per company and prefix using PostgreSQL. */
@Component
public class VendorPoNumberSequence {

  private final JdbcTemplate jdbcTemplate;

  /** Creates the allocator backed by the application's transaction-managed DataSource. */
  public VendorPoNumberSequence(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Reserves the next suffix; the JdbcTemplate update joins the active PO transaction. */
  public long nextNumber(Long companyId, String prefix) {
    String sql = "INSERT INTO vendor_po_number_sequence (company_id, prefix, last_value) "
        + "VALUES (?, ?, COALESCE((SELECT MAX(substring(po_number FROM length(?) + 1)::BIGINT) "
        + "FROM vendor_purchase_order WHERE company_id = ? " + "AND LEFT(po_number, length(?)) = ? "
        + "AND substring(po_number FROM length(?) + 1) ~ '^[0-9]+$'), 0) + 1) "
        + "ON CONFLICT (company_id, prefix) DO UPDATE SET last_value = "
        + "vendor_po_number_sequence.last_value + 1 RETURNING last_value";
    return jdbcTemplate.queryForObject(sql, Long.class, companyId, prefix, prefix, companyId,
        prefix, prefix, prefix);
  }
}
