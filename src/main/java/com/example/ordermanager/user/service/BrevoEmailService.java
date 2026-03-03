package com.example.ordermanager.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BrevoEmailService {

  private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

  private final WebClient webClient;
  private final String brevoApiKey;
  private final String fromEmail;
  private final ObjectMapper objectMapper;

  public BrevoEmailService(@Value("${brevo.api-key:}") String brevoApiKey,
      @Value("${spring.mail.from}") String fromEmail, WebClient.Builder webClientBuilder,
      ObjectMapper objectMapper) {
    this.brevoApiKey = brevoApiKey;
    this.fromEmail = fromEmail;
    this.webClient = webClientBuilder.baseUrl("https://api.brevo.com").build();
    this.objectMapper = objectMapper;
  }

  public void sendVerificationEmail(String to, String verificationUrl) {
    String subject = "Verify your email - Order Manager";
    String htmlContent = "<html><body>" + "<p>Welcome to Order Manager!</p>"
        + "<p>Click the link below to verify your email:</p>" + "<p><a href=\"" + verificationUrl
        + "\">Verify Email</a></p>" + "<p>This link will expire in 24 hours.</p>"
        + getEmailSignature() + "</body></html>";

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

  private void sendEmail(String to, String subject, String htmlContent) {
    if (brevoApiKey == null || brevoApiKey.isEmpty()) {
      log.error("Brevo API key not configured. Email not sent to: " + to);
      throw new RuntimeException("Brevo API key is not configured");
    }

    try {
      String requestBody = buildBrevoRequest(to, subject, htmlContent);

      webClient.post().uri("/v3/smtp/email").header("api-key", brevoApiKey)
          .header("Content-Type", "application/json").bodyValue(requestBody).retrieve()
          .bodyToMono(String.class).block();

      log.info("Email sent successfully via Brevo to: " + to);

    } catch (WebClientResponseException e) {
      log.error("Failed to send email via Brevo. Status: " + e.getRawStatusCode() + ", Response: "
          + e.getResponseBodyAsString(), e);
      throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error sending email via Brevo to: " + to, e);
      throw new RuntimeException("Error sending email: " + e.getMessage(), e);
    }
  }

  private String buildBrevoRequest(String to, String subject, String htmlContent) {
    try {
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
            "htmlContent": "%s"
          }
          """.formatted(escapeJson(fromEmail), escapeJson(to), escapeJson(subject),
          escapeJson(htmlContent));
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
}

