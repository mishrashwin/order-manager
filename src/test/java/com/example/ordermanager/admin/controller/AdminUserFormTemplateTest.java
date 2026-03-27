package com.example.ordermanager.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminUserFormTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "admin", "users", "form.html");

  @Test
  void addUserForm_placesUsernameAndPasswordBeforeMobileAndEmail() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    int usernameIndex = template.indexOf("th:field=\"*{username}\"");
    int passwordIndex = template.indexOf("th:field=\"*{password}\"");
    int mobileIndex = template.indexOf("th:field=\"*{mobileNumber}\"");
    int emailIndex = template.indexOf("th:field=\"*{email}\"");

    assertThat(usernameIndex).isGreaterThanOrEqualTo(0);
    assertThat(passwordIndex).isGreaterThan(usernameIndex);
    assertThat(mobileIndex).isGreaterThan(passwordIndex);
    assertThat(emailIndex).isGreaterThan(mobileIndex);
  }

  @Test
  void addUserForm_keepsPhoneWidgetBindingsIntact() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("id=\"mobileNumber\"");
    assertThat(template).contains("id=\"mobileNumberCountry\"");
    assertThat(template).contains("id=\"mobileNumberLocal\"");
    assertThat(template)
        .contains("initPhoneInput('mobileNumber', 'mobileNumberCountry', 'mobileNumberLocal');");
  }
}

