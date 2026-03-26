package com.example.ordermanager.order.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OrderFormTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "orders", "form.html");

  @Test
  void orderForm_postsToCreateForNewOrdersAndUpdateForExistingOrders() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains(
        "th:action=\"${order.id == null} ? @{/orders} : @{/orders/update/{id}(id=${order.id})}\"");
  }
}

