package com.example.ordermanager.user.controller;

import com.example.ordermanager.exception.EmailAlreadySentException;
import com.example.ordermanager.exception.TooManyAttemptsException;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.PasswordResetService;
import com.example.ordermanager.user.service.RegistrationService;
import com.example.ordermanager.user.service.UserService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Auth Controller - Handles authentication flows Updated: Public signup removed - users are now
 * added by admin only
 */
@Controller
public class AuthController {

  private final UserService userService;
  private final RegistrationService registrationService;
  private final PasswordResetService passwordResetService;

  public AuthController(UserService userService, RegistrationService registrationService,
      PasswordResetService passwordResetService) {
    this.userService = userService;
    this.registrationService = registrationService;
    this.passwordResetService = passwordResetService;
  }

  @GetMapping("/login")
  public String loginPage() {
    return "auth/login";
  }

  /**
   * Public signup removed - users are now created by admin only Redirects to login to show message
   */
  @GetMapping("/signup")
  public String signupRedirect(RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("message",
        "User registration is now managed by company administrators. Please contact your admin.");
    return "redirect:/login";
  }

  @GetMapping("/verify")
  public String verifyEmail(@RequestParam("token") String token,
      RedirectAttributes redirectAttributes) {
    try {
      boolean verified = registrationService.verifyToken(token);

      if (verified) {
        redirectAttributes.addFlashAttribute("message",
            "Your email has been verified successfully! You can now log in.");
      } else {
        redirectAttributes.addFlashAttribute("error",
            "Invalid or expired verification link. Please request a new one.");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "Something went wrong during verification. Please try again later.");
    }

    return "redirect:/login";
  }

  @GetMapping("/resend-verification")
  public String resendVerificationPage(
      @RequestParam(value = "email", required = false) String email, Model model) {
    model.addAttribute("email", email);
    return "auth/resend-verification";
  }

  @PostMapping("/resend-verification")
  public String resendVerification(@RequestParam("email") String input,
      RedirectAttributes redirectAttributes) {
    boolean isEmail = input.contains("@");

    User user = isEmail ? userService.findByEmail(input) : userService.findByUsername(input);

    if (user == null) {
      redirectAttributes.addFlashAttribute("error",
          "Please enter a valid " + (isEmail ? "email address." : "username."));
      return "redirect:/resend-verification";
    }

    if (user.isEnabled()) {
      redirectAttributes.addFlashAttribute("message",
          "Your account is already verified. You can log in now.");
      return "redirect:/login";
    }

    try {
      registrationService.sendVerificationEmail(user);
      redirectAttributes.addFlashAttribute("message",
          "Verification email resent successfully! Please check your inbox.");
    } catch (EmailAlreadySentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "Something went wrong while sending the verification email. Please try again later.");
    }

    return "redirect:/login";
  }

  /**
   * Forgot Password - Step 1: Request password reset code
   */
  @GetMapping("/forgot-password")
  public String forgotPasswordPage() {
    return "auth/forgot-password";
  }

  @PostMapping("/forgot-password")
  public String requestPasswordReset(@RequestParam("email") String input,
      RedirectAttributes redirectAttributes) {
    boolean isEmail = input.contains("@");

    User user = isEmail ? userService.findByEmail(input) : userService.findByUsername(input);

    if (user == null) {
      redirectAttributes.addFlashAttribute("error",
          "Please enter a valid " + (isEmail ? "email address." : "username."));
      return "redirect:/forgot-password";
    }

    if (!user.isEnabled()) {
      redirectAttributes.addFlashAttribute("error",
          "Your account is not verified yet. Please verify your email first.");
      return "redirect:/forgot-password";
    }

    try {
      passwordResetService.sendPasswordResetCode(user);
      redirectAttributes.addFlashAttribute("message",
          "A 6-digit verification code has been sent to your registered email.");
      return "redirect:/verify-reset-code?email=" + encodeEmail(user.getEmail());
    } catch (EmailAlreadySentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/forgot-password";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "Something went wrong while sending the reset code. Please try again later.");
      return "redirect:/forgot-password";
    }
  }

  /**
   * Forgot Password - Step 2: Verify 6-digit code
   */
  @GetMapping("/verify-reset-code")
  public String verifyResetCodePage(@RequestParam(value = "email", required = false) String email,
      Model model) {
    model.addAttribute("email", email);
    return "auth/verify-reset-code";
  }

  @PostMapping("/verify-reset-code")
  public String verifyResetCode(@RequestParam("code") String code,
      @RequestParam("email") String email, RedirectAttributes redirectAttributes) {
    User user = passwordResetService.verifyPasswordResetCode(code, email);

    if (user == null) {
      redirectAttributes.addFlashAttribute("error",
          "Invalid or expired code. Please request a new password reset.");
      return "redirect:/verify-reset-code?email=" + encodeEmail(email);
    }

    redirectAttributes.addFlashAttribute("message", "Code verified successfully!");
    return "redirect:/reset-password?code=" + code + "&email=" + encodeEmail(email);
  }

  /**
   * Forgot Password - Step 3: Set new password
   */
  @GetMapping("/reset-password")
  public String resetPasswordPage(@RequestParam("code") String code,
      @RequestParam("email") String email, Model model) {
    model.addAttribute("code", code);
    model.addAttribute("email", email);
    return "auth/reset-password";
  }

  @PostMapping("/reset-password")
  public String resetPassword(@RequestParam("code") String code,
      @RequestParam("email") String email, @RequestParam("password") String password,
      @RequestParam("confirmPassword") String confirmPassword,
      RedirectAttributes redirectAttributes) {

    String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);

    if (password == null || password.isEmpty()) {
      redirectAttributes.addFlashAttribute("error", "Password cannot be empty.");
      return "redirect:/reset-password?code=" + code + "&email=" + encodeEmail(email);
    }

    if (!password.equals(confirmPassword)) {
      redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
      return "redirect:/reset-password?code=" + code + "&email=" + encodeEmail(email);
    }

    if (password.length() < 6) {
      redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters long.");
      return "redirect:/reset-password?code=" + code + "&email=" + encodeEmail(email);
    }

    try {
      if (passwordResetService.resetPasswordWithCode(code, email, password)) {
        redirectAttributes.addFlashAttribute("message",
            "Password reset successfully! You can now log in with your new password.");
        return "redirect:/login";
      } else {
        redirectAttributes.addFlashAttribute("error",
            "Invalid or expired code. Please request a new password reset.");
        return "redirect:/forgot-password";
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "Something went wrong while resetting your password. Please try again later.");
      return "redirect:/reset-password?code=" + code + "&email=" + encodeEmail(email);
    }
  }

  private String encodeEmail(String email) {
    return java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
  }

}
