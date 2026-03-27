package com.example.ordermanager.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminOrderStatisticsTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "admin", "order-statistics.html");

  @Test
  void orderStatisticsTemplate_displaysOverallAndDrillDownTotalValues() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("Total value:");
    assertThat(template)
        .contains("#numbers.formatDecimal(totalOrderValue, 0, 'COMMA', 2, 'POINT')");
    assertThat(template).contains("selectedClientTotalValue");
    assertThat(template)
        .contains("#numbers.formatDecimal(selectedClientTotalValue, 0, 'COMMA', 2, 'POINT')");
  }
}

