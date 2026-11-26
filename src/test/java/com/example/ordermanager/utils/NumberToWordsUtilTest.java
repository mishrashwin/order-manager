package com.example.ordermanager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NumberToWordsUtilTest {

  /** Covers Indian lakh/crore denominations, higher crore multiples, and decimal paise. */
  @Test
  void convertsIndianDenominationsAndPaise() {
    assertEquals("Indian Rupees One Lakh Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("100000")));
    assertEquals("Indian Rupees Ten Lakh Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("1000000")));
    assertEquals("Indian Rupees One Crore Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("10000000")));
    assertEquals("Indian Rupees Ten Crore Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("100000000")));
    assertEquals("Indian Rupees One Hundred Crore Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("1000000000")));
    assertEquals("Indian Rupees One Lakh and Fifty Paisa Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("100000.50")));
    assertEquals("Minus Indian Rupees One and Twenty Five Paisa Only",
        NumberToWordsUtil.convertToWords(new BigDecimal("-1.25")));
    assertEquals("Indian Rupees Zero Only", NumberToWordsUtil.convertToWords(null));
  }
}
