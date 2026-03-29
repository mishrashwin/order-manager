package com.example.ordermanager.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.ordermanager.user.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

  @Mock
  private EmailService emailService;

  private SupportController controller;

  private static final String OWNER_EMAIL = "owner@example.com";

  @BeforeEach
  void setUp() {
    controller = new SupportController(emailService, OWNER_EMAIL);
  }

  @Test
  void showSupportForm_returnsAuthSupportView() {
    String view = controller.showSupportForm(null);

    assertThat(view).isEqualTo("auth/support");
  }

  @Test
  void submitSupportForm_happyPath_sendEmailAndRedirectWithSubmitted() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    doNothing().when(emailService).sendSupportEmail(any(), any(), any(), any(), any(), any(), any(),
        any(), any());

    String view = controller.submitSupportForm("Jane Doe", "jane@example.com", "9999999999",
        "Login issue", "Cannot log in to my account.", null, redirect);

    assertThat(view).isEqualTo("redirect:/support?submitted");
    assertThat(redirect.getFlashAttributes().get("message")).asString().contains("submitted");
    verify(emailService).sendSupportEmail(eq(OWNER_EMAIL), eq("Jane Doe"), eq("jane@example.com"),
        eq("9999999999"), eq("Login issue"), eq("Cannot log in to my account."), isNull(), isNull(),
        isNull());
  }

  @Test
  void submitSupportForm_withAttachment_passesFileBytesToEmailService() throws Exception {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    byte[] imageBytes = new byte[] {1, 2, 3};
    MockMultipartFile file =
        new MockMultipartFile("screenshot", "screen.png", "image/png", imageBytes);

    String view = controller.submitSupportForm("Jane Doe", "jane@example.com", "9999999999",
        "Bug report", "App crashes on submit.", file, redirect);

    assertThat(view).isEqualTo("redirect:/support?submitted");
    verify(emailService).sendSupportEmail(eq(OWNER_EMAIL), eq("Jane Doe"), eq("jane@example.com"),
        eq("9999999999"), eq("Bug report"), eq("App crashes on submit."), eq(imageBytes),
        eq("screen.png"), eq("image/png"));
  }

  @Test
  void submitSupportForm_blankField_redirectsWithErrorAndSkipsEmail() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = controller.submitSupportForm("Jane Doe", "", "9999999999", "Subject",
        "Description", null, redirect);

    assertThat(view).isEqualTo("redirect:/support");
    assertThat(redirect.getFlashAttributes().get("error")).asString().contains("required");
  }

  @Test
  void submitSupportForm_emailServiceThrows_redirectsWithError() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    doThrow(new RuntimeException("Brevo down")).when(emailService).sendSupportEmail(any(), any(),
        any(), any(), any(), any(), any(), any(), any());

    String view = controller.submitSupportForm("Jane Doe", "jane@example.com", "9999999999",
        "Login issue", "Cannot log in.", null, redirect);

    assertThat(view).isEqualTo("redirect:/support?submitted");
    assertThat(redirect.getFlashAttributes().get("error")).asString().contains("Failed to send");
  }
}
