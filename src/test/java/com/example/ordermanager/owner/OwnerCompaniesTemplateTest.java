package com.example.ordermanager.owner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OwnerCompaniesTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "owner", "companies.html");

  @Test
  void setFeeModal_includesCsrfTokenAndServerRenderedSetFeeActionHook() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains(
        "th:data-set-fee-url=\"@{/owner/companies/{id}/set-fee(id=${company.companyId})}\"");
    assertThat(template).contains("<form id=\"setFeeForm\" method=\"post\">");
    assertThat(template).contains(
        "<input type=\"hidden\" th:name=\"${_csrf.parameterName}\" th:value=\"${_csrf.token}\"/>");
    assertThat(template).contains("const setFeeUrl = btn.dataset.setFeeUrl;");
    assertThat(template).contains("document.getElementById('setFeeForm').action = setFeeUrl;");
    assertThat(template).doesNotContain("'/owner/companies/' + companyId + '/set-fee'");
  }
}

