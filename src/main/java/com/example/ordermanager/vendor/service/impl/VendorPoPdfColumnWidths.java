package com.example.ordermanager.vendor.service.impl;

import com.example.ordermanager.vendor.entity.VendorPoItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calculates compact Vendor PO PDF item columns from their longest rendered values. */
final class VendorPoPdfColumnWidths {

  private static final String[] KEYS =
      {"hsn", "quantity", "unit", "rate", "gst", "gstAmount", "amount"};
  private static final String[] HEADERS =
      {"HSN", "Qty", "Unit", "Rate", "GST%", "GST Amt", "Amount"};
  private static final int[] MINIMUMS = {7, 7, 6, 8, 6, 13, 13};
  private static final int INDEX_WIDTH = 7;
  private static final int PRODUCT_MINIMUM = 16;

  /** Prevents instantiation of this static width calculator. */
  private VendorPoPdfColumnWidths() {}

  /** Returns integer percentage widths totaling 100, leaving a readable minimum Product column. */
  static Map<String, Integer> calculate(List<VendorPoItem> items) {
    int[] idealWidths = MINIMUMS.clone();
    for (int column = 0; column < KEYS.length; column++) {
      int maxLength = HEADERS[column].length();
      if (items != null) {
        for (VendorPoItem item : items) {
          maxLength = Math.max(maxLength, displayValue(item, column).length());
        }
      }
      idealWidths[column] = Math.max(MINIMUMS[column], 2 + (maxLength * 82 + 99) / 100);
    }

    int minimumSum = 0;
    int idealExtra = 0;
    for (int column = 0; column < KEYS.length; column++) {
      minimumSum += MINIMUMS[column];
      idealExtra += idealWidths[column] - MINIMUMS[column];
    }
    int extraBudget = 100 - INDEX_WIDTH - PRODUCT_MINIMUM - minimumSum;
    int[] widths = allocateWidths(idealWidths, idealExtra, extraBudget);

    Map<String, Integer> result = new LinkedHashMap<>();
    result.put("index", INDEX_WIDTH);
    int allocated = INDEX_WIDTH;
    for (int column = 0; column < KEYS.length; column++) {
      result.put(KEYS[column], widths[column]);
      allocated += widths[column];
    }
    result.put("product", 100 - allocated);
    return result;
  }

  /** Distributes available width above per-column minimums in proportion to content lengths. */
  private static int[] allocateWidths(int[] idealWidths, int idealExtra, int extraBudget) {
    int[] widths = MINIMUMS.clone();
    if (idealExtra <= extraBudget) {
      return idealWidths;
    }
    int remaining = extraBudget;
    for (int column = 0; column < widths.length; column++) {
      int extra = idealWidths[column] - MINIMUMS[column];
      int share = idealExtra == 0 ? 0 : extra * extraBudget / idealExtra;
      widths[column] += share;
      remaining -= share;
    }
    for (int column = 0; remaining > 0; column = (column + 1) % widths.length) {
      if (widths[column] < idealWidths[column]) {
        widths[column]++;
        remaining--;
      }
    }
    return widths;
  }

  /** Returns the value rendered for an item column when determining its required width. */
  private static String displayValue(VendorPoItem item, int column) {
    if (item == null) {
      return "";
    }
    return switch (column) {
      case 0 -> valueOrDash(item.getHsnCode());
      case 1 -> valueOrDash(item.getFormattedQuantity());
      case 2 -> valueOrDash(item.getUnit());
      case 3 -> valueOrDash(item.getFormattedTaxableRate());
      case 4 -> item.getGstPercentage() == null ? "0%"
          : Integer.toString(item.getGstPercentage().intValue()) + "%";
      case 5 -> valueOrDash(item.getFormattedGstAmount());
      case 6 -> valueOrDash(item.getFormattedLineTotal());
      default -> "";
    };
  }

  /** Uses the PDF's visible placeholder when an item field has no value. */
  private static String valueOrDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
