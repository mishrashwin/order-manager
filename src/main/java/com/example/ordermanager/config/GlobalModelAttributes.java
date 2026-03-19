package com.example.ordermanager.config;

import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice to inject common model attributes (company name, greeting) into all
 * views. Cache company lookup per request to avoid N+1 queries on authenticated pages.
 */
@ControllerAdvice
public class GlobalModelAttributes {

  private final SecurityContextHelper securityContextHelper;

  public GlobalModelAttributes(SecurityContextHelper securityContextHelper) {
    this.securityContextHelper = securityContextHelper;
  }

  @ModelAttribute
  public void addCompanyNameToModel(Model model) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      model.addAttribute("companyName", "Order Manager");
      model.addAttribute("navbarGreetingName", "User");
      return;
    }

    model.addAttribute("navbarGreetingName", authentication.getName());

    if (securityContextHelper.isOwnerContext()) {
      model.addAttribute("companyName", "Owner Console");
      return;
    }

    try {
      var user = securityContextHelper.getUserFromContext();
      if (user != null && user.getCompany() != null) {
        model.addAttribute("companyName", user.getCompany().getName());
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
          model.addAttribute("navbarGreetingName", user.getFirstName());
        }
      } else {
        model.addAttribute("companyName", "Order Manager");
      }
    } catch (Exception e) {
      model.addAttribute("companyName", "Order Manager");
    }
  }
}


