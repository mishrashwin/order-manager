package com.example.ordermanager.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminPaymentsTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "admin", "payments.html");

  @Test
  void paymentForm_postsToAdminAddEndpointWithCsrfToken() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains(
        "<form th:action=\"@{/admin/payments/add(_csrf=${_csrf.token})}\" method=\"post\"");
    assertThat(template).contains(
        "<input type=\"hidden\" th:name=\"${_csrf.parameterName}\" th:value=\"${_csrf.token}\"/>");
  }
}


