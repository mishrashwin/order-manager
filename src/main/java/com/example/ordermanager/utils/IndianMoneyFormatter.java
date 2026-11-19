package com.example.ordermanager.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formats monetary values with Indian digit grouping and exactly two decimal places. */
public final class IndianMoneyFormatter {

  /** Prevents instances because this formatter exposes only a stateless utility operation. */
  private IndianMoneyFormatter() {}

  /**
   * Returns a display string without changing the source amount; null follows PDF zero defaults.
   */
  public static String format(BigDecimal amount) {
    BigDecimal rounded =
        amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    String plain = rounded.abs().toPlainString();
    int decimalPoint = plain.indexOf('.');
    String integer = decimalPoint < 0 ? plain : plain.substring(0, decimalPoint);
    String fraction = decimalPoint < 0 ? "00" : plain.substring(decimalPoint + 1);
    String grouped;
    if (integer.length() <= 3) {
      grouped = integer;
    } else {
      int lastGroup = integer.length() - 3;
      StringBuilder result = new StringBuilder(integer.substring(lastGroup));
      for (int end = lastGroup; end > 0;) {
        int start = Math.max(0, end - 2);
        result.insert(0, integer.substring(start, end) + ",");
        end = start;
      }
      grouped = result.toString();
    }
    return (rounded.signum() < 0 ? "-" : "") + grouped + "." + fraction;
  }
}
