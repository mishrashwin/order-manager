package com.example.ordermanager.product.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductFormTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "products", "form.html");

  @Test
  void productForm_includesReturnToHiddenFieldAndAddVendorLinkHook() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("th:action=\"@{/products(returnTo=${returnTo})}\"");
    assertThat(template).contains("name=\"returnTo\"");
    assertThat(template).contains("id=\"addVendorLink\"");
    assertThat(template).contains("th:field=\"*{name}\" required");
    assertThat(template).contains("th:field=\"*{brand}\"");
    assertThat(template).contains("th:field=\"*{category}\"");
    assertThat(template).contains("th:field=\"*{vendor.id}\" required");
    assertThat(template).contains("id=\"productSubmitBtn\"");
    assertThat(template).contains("function areMandatoryProductFieldsFilled()");
    assertThat(template).contains("function updateProductSubmitState()");
  }
}


