package com.example.ordermanager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NumberToWordsUtilTest {

  /** Covers Indian lakh/crore denominations, higher crore multiples, and decimal paise. */
  @Test
  void convertsIndianDenominationsAndPaise() {
    assertEquals("Rupees One Lakh Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("100000")));
    assertEquals("Rupees Ten Lakh Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("1000000")));
    assertEquals("Rupees One Crore Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("10000000")));
    assertEquals("Rupees Ten Crore Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("100000000")));
    assertEquals("Rupees One Hundred Crore Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("1000000000")));
    assertEquals("Rupees One Lakh and Fifty Paisa Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("100000.50")));
    assertEquals("Minus Rupees One and Twenty Five Paisa Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("-1.25")));
    assertEquals("Rupees Zero Only", NumberToWordsUtil.convertToWords(null));
  }
}
