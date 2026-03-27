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
    assertThat(template).contains("id=\"addProductLink\"");
    assertThat(template).contains(
        "@{/products/new(returnTo=${order.id == null ? '/orders/new' : '/orders/edit/' + order.id})}");
    assertThat(template).contains("const orderDraftStorageKey");
    assertThat(template).contains("id=\"orderQuantitySummary\"");
    assertThat(template).contains("id=\"orderTotalAmountSummary\"");
    assertThat(template).contains("productRows: collectProductRows()");
    assertThat(template).contains("th:classappend=\"${dateError} ? ' is-invalid' : ''\"");
    assertThat(template).contains("th:if=\"${dateError}\"");
    assertThat(template).contains("id=\"dateClientError\"");
    assertThat(template).contains("function validateOrderDatesOnClient()");
    assertThat(template).contains("Delivery date must be on or after order date.");
    assertThat(template).doesNotContain("th:field=\"*{quantity}\" min=\"1\" required");
    assertThat(template)
        .doesNotContain("th:field=\"*{totalAmount}\" step=\"0.01\" min=\"0\" required");
  }
}

