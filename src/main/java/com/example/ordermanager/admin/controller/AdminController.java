package com.example.ordermanager.admin.controller;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin Controller - manages users within their company. All endpoints are protected
 * with @PreAuthorize("hasRole('ADMIN')")
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final UserService userService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public AdminController(UserService userService, CompanyService companyService,
      SecurityContextHelper securityContextHelper) {
    this.userService = userService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping("/dashboard")
  public String adminDashboard(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("companyId", companyId);
    return "admin/dashboard";
  }

  @GetMapping("/users")
  public String listUsers(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    List<User> users = userService.getUsersByCompany(companyId);
    long adminCount = userService.getAdminCountInCompany(companyId);
    String currentUsername = securityContextHelper.getCurrentUsername();
    model.addAttribute("users", users);
    model.addAttribute("companyId", companyId);
    model.addAttribute("adminCount", adminCount);
    model.addAttribute("currentUsername", currentUsername);
    return "admin/users/list";
  }

  @GetMapping("/users/add")
  public String showAddUserForm(Model model) {
    model.addAttribute("user", new User());
    model.addAttribute("roles", new String[] {"USER", "MANAGER", "ADMIN"});
    return "admin/users/form";
  }

  @PostMapping("/users/add")
  public String addUser(@ModelAttribute User user, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      userService.createUserByAdmin(user, companyId);
      redirectAttributes.addFlashAttribute("message",
          "User '" + user.getUsername() + "' created successfully. Verification email sent.");
      return "redirect:/admin/users";
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isMobileValidationError(message)) {
        model.addAttribute("mobileError", message);
      }
      model.addAttribute("user", user);
      model.addAttribute("roles", new String[] {"USER", "MANAGER", "ADMIN"});
      return "admin/users/form";
    }
  }

  private boolean isMobileValidationError(String message) {
    return message != null && message.toLowerCase().contains("mobile");
  }

  @GetMapping("/users/{id}/edit")
  public String showEditUserForm(@PathVariable Long id, Model model) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      User user = userService.getUserByIdAndCompany(id, companyId);
      model.addAttribute("user", user);
      model.addAttribute("roles", new String[] {"USER", "MANAGER", "ADMIN"});
      return "admin/users/form-edit";
    } catch (IllegalArgumentException e) {
      return "redirect:/admin/users?error=" + e.getMessage();
    }
  }

  @PostMapping("/users/{id}/role")
  public String updateUserRole(@PathVariable Long id, @RequestParam String role,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      userService.updateUserRole(id, role, companyId);
      redirectAttributes.addFlashAttribute("message", "User role updated successfully.");
      return "redirect:/admin/users";
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users";
    }
  }

  @PostMapping("/users/{id}/delete")
  public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      String currentUsername = securityContextHelper.getCurrentUsername();
      User user = userService.getUserByIdAndCompany(id, companyId);

      if (user.getUsername().equals(currentUsername)) {
        long adminCount = userService.getAdminCountInCompany(companyId);
        if ("ADMIN".equals(user.getRole()) && adminCount <= 1) {
          redirectAttributes.addFlashAttribute("error",
              "You cannot delete your own account when you are the only admin!");
          return "redirect:/admin/users";
        }
        redirectAttributes.addFlashAttribute("error", "You cannot delete your own account!");
        return "redirect:/admin/users";
      }

      String username = user.getUsername();
      userService.deleteUserByAdmin(id, companyId);
      redirectAttributes.addFlashAttribute("message",
          "User '" + username + "' deleted successfully.");
      return "redirect:/admin/users";
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users";
    }
  }

  @PostMapping("/users/{id}/toggle-status")
  public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      String currentUsername = securityContextHelper.getCurrentUsername();
      User user = userService.getUserByIdAndCompany(id, companyId);

      if (user.getUsername().equals(currentUsername)) {
        redirectAttributes.addFlashAttribute("error",
            "You cannot change the active status of your own account!");
        return "redirect:/admin/users";
      }

      boolean newStatus = !user.isAccountActive();
      userService.setUserActive(id, newStatus, companyId);
      String statusLabel = newStatus ? "activated" : "deactivated";
      redirectAttributes.addFlashAttribute("message",
          "User '" + user.getUsername() + "' has been " + statusLabel + " successfully.");
      return "redirect:/admin/users";
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users";
    }
  }

  @PostMapping("/users/{id}/resend-verification")
  public String resendVerificationEmail(@PathVariable Long id,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      User user = userService.getUserByIdAndCompany(id, companyId);
      redirectAttributes.addFlashAttribute("message",
          "Verification email resent to " + user.getEmail());
      return "redirect:/admin/users";
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users";
    }
  }

  @PostMapping("/users/{id}/update")
  public String updateUser(@PathVariable Long id, @ModelAttribute User user, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      userService.updateUserByAdmin(id, user, companyId);
      redirectAttributes.addFlashAttribute("message", "User updated successfully.");
      return "redirect:/admin/users";
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isMobileValidationError(message)) {
        model.addAttribute("mobileError", message);
      }
      // Fetch fresh user data for the form
      try {
        Long companyId = securityContextHelper.getCompanyIdFromContext();
        User refreshedUser = userService.getUserByIdAndCompany(id, companyId);
        model.addAttribute("user", refreshedUser);
      } catch (Exception ex) {
        // Fallback to submitted user if fetch fails
        model.addAttribute("user", user);
      }
      model.addAttribute("roles", new String[] {"USER", "MANAGER", "ADMIN"});
      return "admin/users/form-edit";
    }
  }

  @GetMapping("/company")
  public String viewCompany(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyService.getCompanyById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    model.addAttribute("company", company);
    return "admin/company/view";
  }

  @GetMapping("/company/edit")
  public String showEditCompanyForm(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyService.getCompanyById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    model.addAttribute("company", company);
    return "admin/company/edit";
  }

  @PostMapping("/company/update")
  public String updateCompany(@ModelAttribute Company company,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      companyService.updateCompany(companyId, company);
      redirectAttributes.addFlashAttribute("message", "Company details updated successfully.");
      return "redirect:/admin/company";

    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/company/edit";
    }
  }
}

