package com.example.ordermanager.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DashboardUrgentNotificationSanitizationTest {

  @Test
  void createFlashNotification_usesTextRenderingAndAvoidsHtmlInjectionSinks() throws IOException {
    String dashboardHtml = new ClassPathResource("templates/dashboard.html")
        .getContentAsString(StandardCharsets.UTF_8);

    String functionBody = extractCreateFlashNotificationFunction(dashboardHtml);

    assertTrue(functionBody.contains("customerStrong.textContent = customerName;"),
        "Customer name should be rendered via textContent");
    assertTrue(functionBody.contains("createTextNode(` - ${productName} (Qty: ${quantityText})`)"),
        "Product and quantity should be rendered as text node content");
    assertTrue(functionBody.contains("createTextNode(` Delivery: ${daysText}`)"),
        "Delivery metadata should be rendered as text node content");
    assertTrue(
        functionBody.contains("const deliveryDate = parseLocalIsoDate(safeOrder.deliveryDate);"),
        "Delivery date should be parsed through local-date helper");
    assertFalse(functionBody.contains("new Date(safeOrder.deliveryDate)"),
        "Direct Date parsing of YYYY-MM-DD can shift dates across timezones");

    assertFalse(functionBody.contains("innerHTML"),
        "createFlashNotification must not use innerHTML with untrusted data");
    assertFalse(functionBody.contains("insertAdjacentHTML"),
        "createFlashNotification must not use insertAdjacentHTML with untrusted data");
  }

  @Test
  void dashboardScript_containsLocalIsoDateParser() throws IOException {
    String dashboardHtml = new ClassPathResource("templates/dashboard.html")
        .getContentAsString(StandardCharsets.UTF_8);

    assertTrue(dashboardHtml.contains("function parseLocalIsoDate(value)"),
        "Template should define parseLocalIsoDate helper");
    assertTrue(dashboardHtml.contains("new Date(year, month - 1, day);"),
        "Local parsing should construct Date with year/month/day parts");
  }

  private String extractCreateFlashNotificationFunction(String html) {
    String functionStartMarker = "function createFlashNotification(order)";
    String nextBlockMarker = "document.addEventListener(\"DOMContentLoaded\"";

    int start = html.indexOf(functionStartMarker);
    int end = html.indexOf(nextBlockMarker, start);

    assertTrue(start >= 0, "Could not find createFlashNotification function in dashboard template");
    assertTrue(end > start,
        "Could not determine end of createFlashNotification function in dashboard template");

    return html.substring(start, end);
  }
}


