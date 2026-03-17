package com.example.ordermanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorPageController {

  @GetMapping("/access-denied")
  public String accessDenied(@RequestParam(value = "reason", required = false) String reason,
      Model model) {
    model.addAttribute("title", "Access Restricted");
    model.addAttribute("message", resolveMessage(reason));
    model.addAttribute("reason", reason == null ? "forbidden" : reason);
    return "error/access-denied";
  }

  private String resolveMessage(String reason) {
    if ("suspended".equals(reason)) {
      return "Your company access has been suspended by the application owner, usually due to payment or account status. Please contact your company admin or support team.";
    }
    if ("pending-approval".equals(reason)) {
      return "Your company registration is pending owner approval. You will be able to access the platform after approval.";
    }
    if ("rejected".equals(reason)) {
      return "Your company registration was not approved. Please contact support for further details.";
    }
    return "You do not currently have permission to access this page.";
  }
}
