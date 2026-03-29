package com.example.ordermanager.user.controller;

import com.example.ordermanager.user.service.EmailService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SupportController - Handles the public support/grievance form. This endpoint is intentionally
 * publicly accessible (no authentication required) so that any user, including those locked out,
 * can report issues.
 */
@Controller
@RequestMapping("/support")
public class SupportController {

  private static final Logger log = LoggerFactory.getLogger(SupportController.class);

  private final EmailService emailService;
  private final String ownerEmail;

  public SupportController(EmailService emailService,
      @Value("${app.owner.email}") String ownerEmail) {
    this.emailService = emailService;
    this.ownerEmail = ownerEmail;
  }

  @GetMapping
  public String showSupportForm(Model model) {
    return "auth/support";
  }

  @PostMapping
  public String submitSupportForm(@RequestParam String name, @RequestParam String email,
      @RequestParam String mobile, @RequestParam String subject, @RequestParam String description,
      @RequestParam(value = "screenshot", required = false) MultipartFile screenshot,
      RedirectAttributes redirectAttributes) {

    if (name.isBlank() || email.isBlank() || mobile.isBlank() || subject.isBlank()
        || description.isBlank()) {
      redirectAttributes.addFlashAttribute("error", "All fields except attachment are required.");
      return "redirect:/support";
    }

    byte[] attachmentContent = null;
    String attachmentFilename = null;
    String attachmentContentType = null;

    if (screenshot != null && !screenshot.isEmpty()) {
      try {
        attachmentContent = screenshot.getBytes();
        attachmentFilename = screenshot.getOriginalFilename();
        attachmentContentType = screenshot.getContentType();
      } catch (IOException e) {
        log.warn("Failed to read attachment for support request from {}: {}", email,
            e.getMessage());
      }
    }

    try {
      emailService.sendSupportEmail(ownerEmail, name, email, mobile, subject, description,
          attachmentContent, attachmentFilename, attachmentContentType);
      redirectAttributes.addFlashAttribute("message",
          "Your support request has been submitted. We will get back to you shortly.");
    } catch (Exception e) {
      log.error("Failed to send support request email from {}: {}", email, e.getMessage(), e);
      redirectAttributes.addFlashAttribute("error",
          "Failed to send your request. Please try again later.");
    }

    return "redirect:/support?submitted";
  }
}
