package com.example.ordermanager.config;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.service.CompanyService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice to add common model attributes to all views. Adds company name to all
 * authenticated pages for display in navbar.
 */
@ControllerAdvice
public class GlobalModelAttributes {

  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public GlobalModelAttributes(CompanyService companyService,
      SecurityContextHelper securityContextHelper) {
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  /**
   * Add company name to all views for authenticated users. This is used in the navbar to display
   * company name instead of generic "Order Manager"
   */
  @ModelAttribute
  public void addCompanyNameToModel(Model model) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // Only add company name if user is authenticated and not anonymous
    if (authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {

      model.addAttribute("navbarGreetingName", authentication.getName());

      if (securityContextHelper.isOwnerContext()) {
        model.addAttribute("companyName", "Owner Console");
        model.addAttribute("navbarGreetingName", "Owner");
        return;
      }

      try {
        var user = securityContextHelper.getUserFromContext();
        Long companyId = null;
        String firstName = null;
        if (user != null) {
          firstName = user.getFirstName();
          if (user.getCompany() != null) {
            companyId = user.getCompany().getId();
          }
        }
        String companyName = (companyId != null)
            ? companyService.getCompanyById(companyId).map(Company::getName).orElse("Order Manager")
            : "Order Manager";

        model.addAttribute("companyName", companyName);
        if (firstName != null && !firstName.isBlank()) {
          model.addAttribute("navbarGreetingName", firstName);
        }
      } catch (Exception e) {
        // If there's any issue getting company name, use default
        model.addAttribute("companyName", "Order Manager");
      }
    } else {
      // For unauthenticated users, use default
      model.addAttribute("companyName", "Order Manager");
      model.addAttribute("navbarGreetingName", "User");
    }
  }
}


