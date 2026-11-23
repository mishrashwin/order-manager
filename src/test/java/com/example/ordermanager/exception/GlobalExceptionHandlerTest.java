package com.example.ordermanager.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class GlobalExceptionHandlerTest {

  /** Redirects PO update persistence failures to the form with safe user-facing feedback. */
  @Test
  void dataIntegrityFailureForPoUpdateRedirectsToEditForm() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServletPath("/vendor/pos/update/4");
    RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

    Object result = handler.handleDataIntegrityViolation(
        new DataIntegrityViolationException("private SQL constraint detail"), request, attributes);

    assertThat(result).isEqualTo("redirect:/vendor/pos/4/edit");
    assertThat(attributes.getFlashAttributes()).containsKey("poError");
    assertThat(attributes.getFlashAttributes().get("poError").toString())
        .doesNotContain("private SQL constraint detail");
  }

  /** Uses a fixed generic response for unexpected errors instead of reflecting exception text. */
  @Test
  void genericFailureDoesNotExposeExceptionOrRequestPath() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    ResponseEntity<Object> response =
        handler.handleGeneric(new IllegalStateException("private SQL and schema details"), null);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().toString()).doesNotContain("private SQL", "schema details",
        "path");
  }
}
