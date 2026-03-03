package com.example.ordermanager.user.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EmailService - Delegates to Brevo API
 *
 * Uses Brevo API (HTTP-based) instead of SMTP for Render free tier compatibility SMTP ports (25,
 * 465, 587, 2525) are blocked by Render, so API is the solution
 */
@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final BrevoEmailService brevoEmailService;

  public EmailService(BrevoEmailService brevoEmailService) {
    this.brevoEmailService = brevoEmailService;
  }

  public void sendVerificationEmail(String to, String verificationUrl) {
    log.info("Sending verification email via Brevo to: " + to);
    try {
      brevoEmailService.sendVerificationEmail(to, verificationUrl);
      log.info("Verification email sent successfully to: " + to);
    } catch (Exception e) {
      log.error("Failed to send verification email to: " + to, e);
      throw e;
    }
  }

  public void sendPasswordResetEmail(String to, String code) {
    log.info("Sending password reset email via Brevo to: " + to);
    try {
      brevoEmailService.sendPasswordResetEmail(to, code);
      log.info("Password reset email sent successfully to: " + to);
    } catch (Exception e) {
      log.error("Failed to send password reset email to: " + to, e);
      throw e;
    }
  }
}

