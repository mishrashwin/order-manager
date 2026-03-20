package com.example.ordermanager.config;

import com.example.ordermanager.utils.SecurityContextHelper;
import jakarta.servlet.http.HttpServletRequest;
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

  private static final String REQ_ATTR_COMPANY_NAME = "cachedCompanyName";
  private static final String REQ_ATTR_GREETING_NAME = "cachedNavbarGreetingName";

  private final SecurityContextHelper securityContextHelper;

  public GlobalModelAttributes(SecurityContextHelper securityContextHelper) {
    this.securityContextHelper = securityContextHelper;
  }

  @ModelAttribute
  public void addCompanyNameToModel(Model model, HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      model.addAttribute("companyName", "Order Manager");
      model.addAttribute("navbarGreetingName", "User");
      return;
    }

    String cachedCompanyName = (String) request.getAttribute(REQ_ATTR_COMPANY_NAME);
    String cachedGreetingName = (String) request.getAttribute(REQ_ATTR_GREETING_NAME);
    if (cachedCompanyName != null) {
      model.addAttribute("companyName", cachedCompanyName);
      model.addAttribute("navbarGreetingName",
          cachedGreetingName != null ? cachedGreetingName : authentication.getName());
      return;
    }

    String greetingName = authentication.getName();

    if (securityContextHelper.isOwnerContext()) {
      request.setAttribute(REQ_ATTR_COMPANY_NAME, "Owner Console");
      request.setAttribute(REQ_ATTR_GREETING_NAME, greetingName);
      model.addAttribute("companyName", "Owner Console");
      model.addAttribute("navbarGreetingName", greetingName);
      return;
    }

    try {
      var user = securityContextHelper.getUserFromContext();
      if (user != null && user.getCompany() != null) {
        String companyName = user.getCompany().getName();
        request.setAttribute(REQ_ATTR_COMPANY_NAME, companyName);
        model.addAttribute("companyName", companyName);
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
          greetingName = user.getFirstName();
        }
      } else {
        request.setAttribute(REQ_ATTR_COMPANY_NAME, "Order Manager");
        model.addAttribute("companyName", "Order Manager");
      }
    } catch (Exception e) {
      request.setAttribute(REQ_ATTR_COMPANY_NAME, "Order Manager");
      model.addAttribute("companyName", "Order Manager");
    }

    request.setAttribute(REQ_ATTR_GREETING_NAME, greetingName);
    model.addAttribute("navbarGreetingName", greetingName);
  }
}


