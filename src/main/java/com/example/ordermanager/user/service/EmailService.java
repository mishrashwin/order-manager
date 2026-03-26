package com.example.ordermanager.user.service;

import com.example.ordermanager.aspect.SkipMethodLogging;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.payment.entity.Payment;
import com.example.ordermanager.user.entity.User;
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
@SkipMethodLogging
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

  public void sendNewCompanyRegistrationNotification(String to, Company company, User adminUser,
      String reviewUrl) {
    log.info("Sending new company registration notification to owner: {}", to);
    try {
      brevoEmailService.sendNewCompanyRegistrationNotification(to, company, adminUser, reviewUrl);
      log.info("Owner notification sent successfully for company: {}", company.getName());
    } catch (Exception e) {
      log.error("Failed to send owner notification for company: {}", company.getName(), e);
      throw e;
    }
  }

  public void sendCompanyApprovedWelcomeEmail(String to, Company company, User adminUser,
      String loginUrl) {
    log.info("Sending company approval welcome email to: {}", to);
    try {
      brevoEmailService.sendCompanyApprovedWelcomeEmail(to, company, adminUser, loginUrl);
      log.info("Company approval welcome email sent successfully to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send company approval welcome email to: {}", to, e);
      throw e;
    }
  }

  public void sendCompanyRejectedEmail(String to, Company company, User adminUser) {
    log.info("Sending company rejection email to: {}", to);
    try {
      brevoEmailService.sendCompanyRejectedEmail(to, company, adminUser);
      log.info("Company rejection email sent successfully to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send company rejection email to: {}", to, e);
      throw e;
    }
  }

  public void sendCompanyAccessRevokedEmail(String to, Company company, User adminUser,
      String loginUrl) {
    log.info("Sending company access revoked email to: {}", to);
    try {
      brevoEmailService.sendCompanyAccessRevokedEmail(to, company, adminUser, loginUrl);
      log.info("Company access revoked email sent successfully to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send company access revoked email to: {}", to, e);
      throw e;
    }
  }

  public void sendCompanyAccessRestoredEmail(String to, Company company, User adminUser,
      String loginUrl) {
    log.info("Sending company access restored email to: {}", to);
    try {
      brevoEmailService.sendCompanyAccessRestoredEmail(to, company, adminUser, loginUrl);
      log.info("Company access restored email sent successfully to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send company access restored email to: {}", to, e);
      throw e;
    }
  }

  public void sendPaymentNotificationEmail(String to, Payment payment, Company company) {
    log.info("Sending payment notification email to owner for company: {}", company.getName());
    try {
      brevoEmailService.sendPaymentNotificationEmail(to, payment, company);
      log.info("Payment notification email sent successfully to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send payment notification email to: {}", to, e);
      throw e;
    }
  }
}

