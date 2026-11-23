package com.example.ordermanager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IndianQuantityFormatterTest {

  /** Groups whole quantities using Indian digit groups and omits database-scale zeros. */
  @Test
  void format_groupsWholeQuantityWithoutFractionalZeros() {
    assertEquals("20,00,00,000", IndianQuantityFormatter.format(new BigDecimal("200000000.000")));
  }

  /** Keeps meaningful fractional digits while grouping the integer part. */
  @Test
  void format_preservesMeaningfulFraction() {
    assertEquals("12,34,567.5", IndianQuantityFormatter.format(new BigDecimal("1234567.500")));
  }

  /** Renders a missing quantity as the same visible placeholder used in PDF cells. */
  @Test
  void format_nullUsesDash() {
    assertEquals("-", IndianQuantityFormatter.format(null));
  }
}
