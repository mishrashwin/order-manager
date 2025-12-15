package com.example.ordermanager.user.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String verificationUrl) {
        System.out.println("sendVerificationEmail to email id : "+ to);
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("mishrashwin199401@gmail.com");
        mail.setTo(to);
        mail.setSubject("Verify your email - Order Manager");
        mail.setText("Welcome to Order Manager!\n\nClick the link below to verify your email:\n"
                + verificationUrl + "\n\nThis link will expire in 24 hours.");
        mailSender.send(mail);
    }
}
