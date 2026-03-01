package com.example.ordermanager.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;

  @Value("${spring.mail.from}")
  private String fromEmail;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendVerificationEmail(String to, String verificationUrl) {
    log.info("sendVerificationEmail to email id : " + to);
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(fromEmail);
    mail.setTo(to);
    mail.setSubject("Verify your email - Order Manager");
    mail.setText("Welcome to Order Manager!\n\nClick the link below to verify your email:\n"
        + verificationUrl + "\n\nThis link will expire in 24 hours.");
    mailSender.send(mail);
    log.info("Verification Email sent successfully to : " + to);
  }

  public void sendPasswordResetEmail(String to, String code) {
    log.info("sendPasswordResetEmail to email id : " + to);
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(fromEmail);
    mail.setTo(to);
    mail.setSubject("Password Reset Code - Order Manager");
    mail.setText("You requested a password reset.\n\n" + "Your 6-digit verification code is: "
        + code + "\n\n" + "This code will expire in 15 minutes.\n\n"
        + "If you didn't request this, please ignore this email.");
    mailSender.send(mail);
    log.info("Password Reset Email sent successfully to : " + to);
  }
}
