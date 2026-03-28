package com.example.ordermanager.client.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClientFormTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "clients", "form.html");

  @Test
  void clientForm_marksMandatoryFieldsAndUsesDisabledSubmitUntilComplete() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("th:action=\"@{/clients(returnTo=${returnTo})}\"");
    assertThat(template).contains("th:field=\"*{name}\" required");
    assertThat(template).contains("th:field=\"*{contactPerson}\" required");
    assertThat(template).contains("th:field=\"*{email}\" required");
    assertThat(template).contains("id=\"clientPhoneLocal\"");
    assertThat(template).contains("id=\"clientSubmitBtn\"");
    assertThat(template).contains("function areMandatoryClientFieldsFilled()");
    assertThat(template).contains("function updateClientSubmitState()");
  }
}

