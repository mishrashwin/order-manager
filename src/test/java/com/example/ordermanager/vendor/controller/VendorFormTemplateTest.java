package com.example.ordermanager.vendor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VendorFormTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "vendors", "form.html");

  @Test
  void vendorForm_includesReturnToHiddenField() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("name=\"returnTo\"");
    assertThat(template).contains("${returnTo != null ? returnTo : '/vendors'}");
  }
}

