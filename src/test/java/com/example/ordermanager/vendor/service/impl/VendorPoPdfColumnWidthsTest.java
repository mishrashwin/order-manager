package com.example.ordermanager.vendor.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.ordermanager.vendor.entity.VendorPoItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VendorPoPdfColumnWidthsTest {

  /** Gives long HSN values room while letting a short Rate column contract. */
  @Test
  void calculate_widensLongHsnAndKeepsAllColumnsSeparated() {
    VendorPoItem item = new VendorPoItem();
    item.setHsnCode("8414511020000000");
    item.setFormattedQuantity("1");
    item.setUnit("1");
    item.setFormattedTaxableRate("100.00");
    item.setGstPercentage(new BigDecimal("18.00"));
    item.setFormattedGstAmount("3,60,00,00,000.00");
    item.setFormattedLineTotal("23,60,00,00,000.00");

    Map<String, Integer> widths = VendorPoPdfColumnWidths.calculate(List.of(item));

    assertTrue(widths.get("hsn") > widths.get("rate"));
    assertTrue(widths.get("product") >= 16);
    assertEquals(100, widths.values().stream().mapToInt(Integer::intValue).sum());
  }

  /** Gives a large grouped Rate more space than a short HSN value. */
  @Test
  void calculateWidenRateWhenItsRenderedValueIsLong() {
    VendorPoItem item = new VendorPoItem();
    item.setHsnCode("1234");
    item.setFormattedQuantity("1");
    item.setUnit("EA");
    item.setFormattedTaxableRate("9,99,99,99,999.99");
    item.setGstPercentage(new BigDecimal("18.00"));
    item.setFormattedGstAmount("1,79,99,99,999.82");
    item.setFormattedLineTotal("11,79,99,99,999.81");

    Map<String, Integer> widths = VendorPoPdfColumnWidths.calculate(List.of(item));

    assertTrue(widths.get("rate") > widths.get("hsn"));
    assertEquals(100, widths.values().stream().mapToInt(Integer::intValue).sum());
  }
}
