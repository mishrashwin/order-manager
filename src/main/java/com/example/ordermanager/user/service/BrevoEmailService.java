package com.example.ordermanager.user.service;

import com.example.ordermanager.aspect.SkipMethodLogging;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.payment.entity.Payment;
import com.example.ordermanager.user.entity.User;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@SkipMethodLogging
public class BrevoEmailService {

  private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

  private final WebClient webClient;
  private final String brevoApiKey;
  private final String fromEmail;
  private final Duration requestTimeout;

  public BrevoEmailService(@Value("${brevo.api-key:}") String brevoApiKey,
      @Value("${spring.mail.from}") String fromEmail,
      @Value("${brevo.request-timeout-ms:5000}") long requestTimeoutMs,
      WebClient.Builder webClientBuilder) {
    this.brevoApiKey = brevoApiKey;
    this.fromEmail = fromEmail;
    this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
    this.webClient = webClientBuilder.baseUrl("https://api.brevo.com").build();
  }

  public void sendVerificationEmail(String to, String verificationUrl) {
    String subject = "Verify Your Email - Order Manager";
    String htmlContent =
        "<html><body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Poppins, sans-serif; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">"
            + "<h2 style=\"color: #1f2937; margin-bottom: 20px;\">Welcome to Order Manager!</h2>"
            + "<p style=\"color: #555; font-size: 16px; line-height: 1.6; margin-bottom: 20px;\">"
            + "Thank you for signing up. We're excited to have you on board." + "</p>"
            + "<p style=\"color: #555; font-size: 16px; line-height: 1.6; margin-bottom: 30px;\">"
            + "To get started, please verify your email address by clicking the button below:"
            + "</p>" + "<div style=\"text-align: center; margin: 30px 0;\">" + "<a href=\""
            + escapeHtml(verificationUrl)
            + "\" style=\"display: inline-block; padding: 14px 32px; background: #007bff; color: white; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;\">"
            + "Verify Email Address" + "</a>" + "</div>"
            + "<p style=\"color: #888; font-size: 14px; line-height: 1.6; margin-top: 30px;\">"
            + "Or copy and paste this link in your browser:" + "</p>"
            + "<p style=\"color: #007bff; font-size: 13px; word-break: break-all;\">"
            + escapeHtml(verificationUrl) + "</p>"
            + "<hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;\">"
            + "<p style=\"color: #888; font-size: 13px; line-height: 1.6;\">"
            + "<strong>Security note:</strong> This verification link will expire in 24 hours. "
            + "If you didn't create an account, please ignore this email or contact our support team."
            + "</p>" + "<p style=\"color: #888; font-size: 13px; line-height: 1.6;\">"
            + "With Order Manager, you'll be able to manage orders, clients, and vendors all in one place — with better visibility, faster operations, and seamless collaboration with your team."
            + "</p>" + getEmailSignature() + "</div></body></html>";

    sendEmail(to, subject, htmlContent);
  }

  public void sendPasswordResetEmail(String to, String code) {
    String subject = "Password Reset Code - Order Manager";
    String htmlContent = "<html><body>" + "<p>You requested a password reset.</p>"
        + "<p>Your 6-digit verification code is: <strong>" + code + "</strong></p>"
        + "<p>This code will expire in 15 minutes.</p>"
        + "<p>If you didn't request this, please ignore this email.</p>" + getEmailSignature()
        + "</body></html>";

    sendEmail(to, subject, htmlContent);
  }

  public void sendNewCompanyRegistrationNotification(String to, Company company, User adminUser,
      String reviewUrl) {
    String subject = "New company registration awaiting approval - Order Manager";
    String htmlContent = "<html><body>"
        + "<p>A new company has completed registration and is awaiting your approval.</p>"
        + "<p><strong>Company:</strong> " + escapeHtml(company.getName()) + "</p>"
        + "<p><strong>Primary admin:</strong> " + escapeHtml(adminUser.getFirstName()) + " "
        + escapeHtml(adminUser.getLastName()) + "</p>" + "<p><strong>Email:</strong> "
        + escapeHtml(adminUser.getEmail()) + "</p>" + "<p><strong>Mobile:</strong> "
        + escapeHtml(adminUser.getMobileNumber()) + "</p>"
        + "<p>Please review this company from your owner console:</p>" + "<p><a href=\"" + reviewUrl
        + "\">Open owner company approvals</a></p>"
        + "<p>You can approve or reject the company after logging in with your owner account.</p>"
        + getEmailSignature() + "</body></html>";

    sendEmail(to, subject, htmlContent);
  }

  public void sendCompanyApprovedWelcomeEmail(String to, Company company, User adminUser,
      String loginUrl) {
    String greetingName =
        adminUser.getFirstName() == null || adminUser.getFirstName().isBlank() ? "there"
            : escapeHtml(adminUser.getFirstName());
    String subject = "Welcome to Order Manager - Your company is now approved";
    String verificationNote = adminUser.isEnabled()
        ? "You can sign in right away using your registered credentials."
        : "Please verify your email address first, then sign in using your registered credentials.";

    String htmlContent = "<html><body>" + "<p>Hi " + greetingName + ",</p>"
        + "<p>Great news — your company <strong>" + escapeHtml(company.getName())
        + "</strong> has been successfully approved and your Order Manager workspace is now ready.</p>"
        + "<p>With Order Manager, your team can manage orders, clients, and vendors from one place with better visibility and control.</p>"
        + "<p>" + verificationNote + "</p>" + "<p><a href=\"" + loginUrl
        + "\" style=\"display:inline-block;padding:12px 20px;background:#1877f2;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600;\">Log in to Order Manager</a></p>"
        + "<p>If the button above does not work, use this link:</p>" + "<p><a href=\"" + loginUrl
        + "\">" + escapeHtml(loginUrl) + "</a></p>"
        + "<p>We’re excited to help you manage all your orders from one place.</p>"
        + getEmailSignature() + "</body></html>";

    sendEmail(to, subject, htmlContent);
  }

  public void sendCompanyRejectedEmail(String to, Company company, User adminUser) {
    String greetingName = resolveGreetingName(adminUser);
    String subject = "Update on your Order Manager company registration";
    String htmlContent = "<html><body>" + "<p>Hi " + greetingName + ",</p>"
        + "<p>Thank you for registering <strong>" + escapeHtml(company.getName())
        + "</strong> with Order Manager.</p>"
        + "<p>After reviewing the registration, we’re unable to approve the company at this time.</p>"
        + "<p>If you believe this is an error or would like to continue the onboarding process, please reply to this email or contact our support team with your company details.</p>"
        + "<p>We appreciate your interest in Order Manager and would be happy to assist you further.</p>"
        + getEmailSignature() + "</body></html>";

    sendEmail(to, subject, htmlContent);
  }

  public void sendCompanyAccessRevokedEmail(String to, Company company, User adminUser,
      String loginUrl) {
    String greetingName = resolveGreetingName(adminUser);
    String htmlContent = "<html><body>" + "<p>Hi " + greetingName + ",</p>"
        + "<p>This is to let you know that access to your company workspace <strong>"
        + escapeHtml(company.getName()) + "</strong> has been temporarily suspended.</p>"
        + "<p>During this time, your team will not be able to sign in or use the platform.</p>"
        + "<p>This usually happens due to account, billing, or compliance review. Please contact your administrator or support team for assistance.</p>"
        + "<p>Once access is restored, you can sign in again here:</p>" + "<p><a href=\"" + loginUrl
        + "\">" + escapeHtml(loginUrl) + "</a></p>" + getEmailSignature() + "</body></html>";

    sendEmail(to, "Your Order Manager company access has been suspended", htmlContent);
  }

  public void sendCompanyAccessRestoredEmail(String to, Company company, User adminUser,
      String loginUrl) {
    String greetingName = resolveGreetingName(adminUser);
    String verificationNote =
        adminUser.isEnabled() ? "You can sign in again using your existing credentials."
            : "Please verify your email first, then sign in using your registered credentials.";
    String htmlContent = "<html><body>" + "<p>Hi " + greetingName + ",</p>"
        + "<p>Good news — access to your company workspace <strong>" + escapeHtml(company.getName())
        + "</strong> has been restored.</p>"
        + "<p>Your team can now continue managing orders, clients, and vendors from one place.</p>"
        + "<p>" + verificationNote + "</p>" + "<p><a href=\"" + loginUrl
        + "\" style=\"display:inline-block;padding:12px 20px;background:#1877f2;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600;\">Log in to Order Manager</a></p>"
        + "<p>If the button above does not work, use this link:</p>" + "<p><a href=\"" + loginUrl
        + "\">" + escapeHtml(loginUrl) + "</a></p>" + getEmailSignature() + "</body></html>";

    sendEmail(to, "Your Order Manager company access has been restored", htmlContent);
  }

  public void sendSupportEmail(String to, String senderName, String senderEmail,
      String senderMobile, String subject, String description, byte[] attachmentContent,
      String attachmentFilename, String attachmentContentType) {
    String emailSubject = "Support Request: " + subject;
    String attachmentNote = attachmentFilename != null && !attachmentFilename.isBlank()
        ? "<p><strong>Attachment:</strong> " + escapeHtml(attachmentFilename) + "</p>"
        : "";
    String htmlContent = "<html><body>"
        + "<p>A new support request has been submitted via the Order Manager support form.</p>"
        + "<hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 16px 0;\">"
        + "<p><strong>Name:</strong> " + escapeHtml(senderName) + "</p>"
        + "<p><strong>Email:</strong> " + escapeHtml(senderEmail) + "</p>"
        + "<p><strong>Mobile:</strong> " + escapeHtml(senderMobile) + "</p>"
        + "<p><strong>Subject:</strong> " + escapeHtml(subject) + "</p>"
        + "<p><strong>Description:</strong></p>"
        + "<p style=\"white-space: pre-wrap; background:#f9fafb; border:1px solid #e5e7eb; border-radius:6px; padding:12px;\">"
        + escapeHtml(description) + "</p>" + attachmentNote
        + "<hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 16px 0;\">"
        + "<p style=\"font-size:12px; color:#888;\">This message was submitted from the public Support page.</p>"
        + getEmailSignature() + "</body></html>";

    sendEmailWithAttachment(to, emailSubject, htmlContent, attachmentContent, attachmentFilename,
        attachmentContentType);
  }

  public void sendPaymentNotificationEmail(String to, Payment payment, Company company) {
    String monthYear = java.time.Month.of(payment.getPaymentMonth()).getDisplayName(
        java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + payment.getPaymentYear();
    String subject = "New Payment Submitted - " + escapeHtml(company.getName());
    String htmlContent = "<html><body>" + "<p>A new payment has been submitted by <strong>"
        + escapeHtml(company.getName()) + "</strong>.</p>"
        + "<p><strong>Payment for Month:</strong> " + escapeHtml(monthYear) + "</p>"
        + "<p><strong>Payment Message:</strong> "
        + escapeHtml(payment.getPaymentMsg() != null ? payment.getPaymentMsg() : "-") + "</p>"
        + (payment.getPaymentSsFilename() != null
            ? "<p><strong>Screenshot:</strong> " + escapeHtml(payment.getPaymentSsFilename())
                + " (available in owner payments panel)</p>"
            : "")
        + "<p>Please log in to your owner console to review this payment.</p>" + getEmailSignature()
        + "</body></html>";

    sendEmail(to, subject, htmlContent);
  }

  private void sendEmail(String to, String subject, String htmlContent) {
    sendEmailWithAttachment(to, subject, htmlContent, null, null, null);
  }

  private void sendEmailWithAttachment(String to, String subject, String htmlContent,
      byte[] attachmentContent, String attachmentFilename, String attachmentContentType) {
    if (brevoApiKey == null || brevoApiKey.isEmpty()) {
      throw new BrevoEmailException("Brevo API key is not configured");
    }

    try {
      String requestBody = buildBrevoRequest(to, subject, htmlContent, attachmentContent,
          attachmentFilename, attachmentContentType);

      webClient.post().uri("/v3/smtp/email").header("api-key", brevoApiKey)
          .header("Content-Type", "application/json").bodyValue(requestBody).retrieve()
          .bodyToMono(String.class)
          // Ensure a slow Brevo response cannot block request threads indefinitely.
          .timeout(requestTimeout).block();

    } catch (WebClientResponseException e) {
      log.error("Brevo API rejected email request. status={}, to={}, response={}",
          e.getStatusCode(), to, e.getResponseBodyAsString(), e);
      throw new BrevoEmailException("Brevo API returned an error while sending email", e);
    } catch (WebClientRequestException e) {
      log.error("Brevo is unreachable while sending email to: {}", to, e);
      throw new BrevoEmailException("Brevo is currently unreachable. Please try again shortly.", e);
    } catch (RuntimeException e) {
      if (isTimeout(e)) {
        log.error("Brevo request timed out after {} ms for recipient: {}",
            requestTimeout.toMillis(), to, e);
        throw new BrevoEmailException("Brevo request timed out. Please try again shortly.", e);
      }
      log.error("Unexpected error sending email via Brevo to: {}", to, e);
      throw new BrevoEmailException("Unexpected error while sending email", e);
    }
  }

  private boolean isTimeout(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof TimeoutException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private String buildBrevoRequest(String to, String subject, String htmlContent) {
    return buildBrevoRequest(to, subject, htmlContent, null, null, null);
  }

  private String buildBrevoRequest(String to, String subject, String htmlContent,
      byte[] attachmentContent, String attachmentFilename, String attachmentContentType) {
    try {
      String attachmentJson = "";
      if (attachmentContent != null && attachmentContent.length > 0 && attachmentFilename != null
          && !attachmentFilename.isBlank()) {
        String base64Content = Base64.getEncoder().encodeToString(attachmentContent);
        String mimeType = attachmentContentType != null && !attachmentContentType.isBlank()
            ? attachmentContentType
            : "application/octet-stream";
        attachmentJson = """
            , "attachment": [
              {
                "content": "%s",
                "name": "%s",
                "type": "%s"
              }
            ]""".formatted(base64Content, escapeJson(attachmentFilename), escapeJson(mimeType));
      }
      return """
          {
            "sender": {
              "name": "Order Manager",
              "email": "%s"
            },
            "to": [
              {
                "email": "%s"
              }
            ],
            "subject": "%s",
            "htmlContent": "%s"%s
          }
          """.formatted(escapeJson(fromEmail), escapeJson(to), escapeJson(subject),
          escapeJson(htmlContent), attachmentJson);
    } catch (Exception e) {
      log.error("Error building Brevo request", e);
      throw new RuntimeException("Error building email request: " + e.getMessage(), e);
    }
  }

  private String getEmailSignature() {
    return "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 20px 0;\">"
        + "<p style=\"font-size: 12px; color: #666;\">" + "Best Regards,<br/>"
        + "<strong>AshLabs Pvt Ltd</strong><br/>"
        + "<em>Your Trusted Order Management Solution</em>" + "</p>";
  }

  private String escapeJson(String str) {
    if (str == null) {
      return "";
    }
    return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private String escapeHtml(String str) {
    if (str == null) {
      return "";
    }
    return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }

  private String resolveGreetingName(User adminUser) {
    return adminUser.getFirstName() == null || adminUser.getFirstName().isBlank() ? "there"
        : escapeHtml(adminUser.getFirstName());
  }
}

