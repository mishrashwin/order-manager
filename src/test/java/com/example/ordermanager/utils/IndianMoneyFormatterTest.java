package com.example.ordermanager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IndianMoneyFormatterTest {

  /** Checks requested Indian grouping from thousands through one hundred crore. */
  @Test
  void formatsIndianDigitGroups() {
    assertEquals("1,000.00", IndianMoneyFormatter.format(new BigDecimal("1000")));
    assertEquals("10,000.00", IndianMoneyFormatter.format(new BigDecimal("10000")));
    assertEquals("1,00,000.00", IndianMoneyFormatter.format(new BigDecimal("100000")));
    assertEquals("10,00,000.00", IndianMoneyFormatter.format(new BigDecimal("1000000")));
    assertEquals("1,00,00,000.00", IndianMoneyFormatter.format(new BigDecimal("10000000")));
    assertEquals("10,00,00,000.00", IndianMoneyFormatter.format(new BigDecimal("100000000")));
    assertEquals("1,00,00,00,000.00", IndianMoneyFormatter.format(new BigDecimal("1000000000")));
  }

  /** Keeps null, sign, decimals, and half-up rounding behavior consistent and display-only. */
  @Test
  void formatsDecimalNullNegativeAndRoundingValues() {
    assertEquals("0.00", IndianMoneyFormatter.format(null));
    assertEquals("0.00", IndianMoneyFormatter.format(BigDecimal.ZERO));
    assertEquals("-1,00,000.50", IndianMoneyFormatter.format(new BigDecimal("-100000.5")));
    assertEquals("1,23,456.79", IndianMoneyFormatter.format(new BigDecimal("123456.785")));
    assertEquals("99,99,99,999.99", IndianMoneyFormatter.format(new BigDecimal("999999999.99")));
  }
}
