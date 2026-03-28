package com.example.ordermanager.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SupportTemplateTest {

  private static final Path SUPPORT_TEMPLATE =
      Path.of("src", "main", "resources", "templates", "auth", "support.html");

  private static final Path LOGIN_TEMPLATE =
      Path.of("src", "main", "resources", "templates", "auth", "login.html");

  @Test
  void supportTemplate_containsRequiredFields() throws IOException {
    String template = Files.readString(SUPPORT_TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("name=\"name\"");
    assertThat(template).contains("name=\"email\"");
    assertThat(template).contains("name=\"mobile\"");
    assertThat(template).contains("name=\"subject\"");
    assertThat(template).contains("name=\"description\"");
    assertThat(template).contains("name=\"screenshot\"");
  }

  @Test
  void supportTemplate_hasCsrfToken() throws IOException {
    String template = Files.readString(SUPPORT_TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("${_csrf.parameterName}");
    assertThat(template).contains("${_csrf.token}");
  }

  @Test
  void supportTemplate_actionPostsToSupportEndpoint() throws IOException {
    String template = Files.readString(SUPPORT_TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("th:action=\"@{/support}\"");
    assertThat(template).contains("method=\"post\"");
  }

  @Test
  void supportTemplate_hasBackToLoginLink() throws IOException {
    String template = Files.readString(SUPPORT_TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("@{/login}");
  }

  @Test
  void loginTemplate_hasSupportLink() throws IOException {
    String template = Files.readString(LOGIN_TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("@{/support}");
  }
}
