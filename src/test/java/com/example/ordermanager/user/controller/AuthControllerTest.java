package com.example.ordermanager.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanager.exception.TooManyAttemptsException;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.PasswordResetService;
import com.example.ordermanager.user.service.RegistrationService;
import com.example.ordermanager.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private UserService userService;
  @Mock
  private RegistrationService registrationService;
  @Mock
  private PasswordResetService passwordResetService;

  private AuthController authController;

  @BeforeEach
  void setUp() {
    authController = new AuthController(userService, registrationService, passwordResetService);
  }

  @Test
  void signupRedirect_returnsLoginWithAdminManagedMessage() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = authController.signupRedirect(redirect);

    assertThat(view).isEqualTo("redirect:/login");
    assertThat(redirect.getFlashAttributes().get("message")).isEqualTo(
        "User registration is now managed by company administrators. Please contact your admin.");
  }

  @Test
  void verifyEmail_whenValidToken_setsSuccessFlash() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    when(registrationService.verifyToken("token-1")).thenReturn(true);

    String view = authController.verifyEmail("token-1", redirect);

    assertThat(view).isEqualTo("redirect:/login");
    assertThat(redirect.getFlashAttributes().get("message"))
        .isEqualTo("Your email has been verified successfully! You can now log in.");
  }

  @Test
  void verifyEmail_whenInvalidToken_setsErrorFlash() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    when(registrationService.verifyToken("bad-token")).thenReturn(false);

    String view = authController.verifyEmail("bad-token", redirect);

    assertThat(view).isEqualTo("redirect:/login");
    assertThat(redirect.getFlashAttributes().get("error"))
        .isEqualTo("Invalid or expired verification link. Please request a new one.");
  }

  @Test
  void requestPasswordReset_whenUserMissing_redirectsBackWithError() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    when(userService.findByEmail("missing@acme.com")).thenReturn(null);

    String view = authController.requestPasswordReset("missing@acme.com", redirect);

    assertThat(view).isEqualTo("redirect:/forgot-password");
    assertThat(redirect.getFlashAttributes().get("error"))
        .isEqualTo("Please enter a valid email address.");
  }

  @Test
  void verifyResetCode_whenTooManyAttempts_redirectsToForgotPassword() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    MockHttpSession session = new MockHttpSession();

    when(passwordResetService.verifyPasswordResetCode("123456", "user@acme.com")).thenThrow(
        new TooManyAttemptsException("Too many failed attempts. Please request a new reset code."));

    String view = authController.verifyResetCode("123456", "user@acme.com", redirect, session);

    assertThat(view).isEqualTo("redirect:/forgot-password");
    assertThat(redirect.getFlashAttributes().get("error"))
        .isEqualTo("Too many failed attempts. Please request a new reset code.");
  }

  @Test
  void verifyResetCode_whenValid_storesSessionAndRedirectsToResetPage() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    MockHttpSession session = new MockHttpSession();

    User user = new User();
    user.setEmail("user@acme.com");

    when(passwordResetService.verifyPasswordResetCode("123456", "user@acme.com")).thenReturn(user);

    String view = authController.verifyResetCode("123456", "user@acme.com", redirect, session);

    assertThat(view).isEqualTo("redirect:/reset-password?email=user%40acme.com");
    assertThat(session.getAttribute("RESET_VERIFIED_EMAIL")).isEqualTo("user@acme.com");
  }

  @Test
  void resetPassword_withoutVerifiedSession_redirectsToForgotPassword() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    MockHttpSession session = new MockHttpSession();

    String view =
        authController.resetPassword("user@acme.com", "NewPass1", "NewPass1", redirect, session);

    assertThat(view).isEqualTo("redirect:/forgot-password");
    assertThat(redirect.getFlashAttributes().get("error"))
        .isEqualTo("Reset session expired. Please verify your code again.");
  }

  @Test
  void resetPassword_success_clearsSessionAndRedirectsToLogin() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("RESET_VERIFIED_EMAIL", "user@acme.com");

    when(passwordResetService.resetPasswordWithVerifiedEmail("user@acme.com", "NewPass1"))
        .thenReturn(true);

    String view =
        authController.resetPassword("user@acme.com", "NewPass1", "NewPass1", redirect, session);

    assertThat(view).isEqualTo("redirect:/login");
    assertThat(session.getAttribute("RESET_VERIFIED_EMAIL")).isNull();
    assertThat(redirect.getFlashAttributes().get("message"))
        .isEqualTo("Password reset successfully! You can now log in with your new password.");
  }
}


