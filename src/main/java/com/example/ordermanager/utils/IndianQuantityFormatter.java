package com.example.ordermanager.utils;

import java.math.BigDecimal;

/** Formats quantities with Indian digit grouping and without insignificant fractional zeros. */
public final class IndianQuantityFormatter {

  /** Prevents instances because quantity formatting is a stateless utility operation. */
  private IndianQuantityFormatter() {}

  /** Formats a quantity with Indian grouping while retaining meaningful fractional digits. */
  public static String format(BigDecimal quantity) {
    if (quantity == null) {
      return "-";
    }
    BigDecimal normalized = quantity.stripTrailingZeros();
    String plain = normalized.abs().toPlainString();
    int decimalPoint = plain.indexOf('.');
    String integer = decimalPoint < 0 ? plain : plain.substring(0, decimalPoint);
    String fraction = decimalPoint < 0 ? "" : plain.substring(decimalPoint);
    String sign = normalized.signum() < 0 ? "-" : "";
    return sign + groupInteger(integer) + fraction;
  }

  /** Applies Indian grouping with a three-digit final group and two-digit preceding groups. */
  private static String groupInteger(String integer) {
    if (integer.length() <= 3) {
      return integer;
    }
    int lastGroup = integer.length() - 3;
    StringBuilder result = new StringBuilder(integer.substring(lastGroup));
    for (int end = lastGroup; end > 0;) {
      int start = Math.max(0, end - 2);
      result.insert(0, integer.substring(start, end) + ",");
      end = start;
    }
    return result.toString();
  }
}
