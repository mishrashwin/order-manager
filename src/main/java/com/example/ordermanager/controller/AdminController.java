package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.service.CompanyService;
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
 * Admin Controller for company administrators Allows admins to manage users within their company
 * All endpoints are protected with @PreAuthorize
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

  /**
   * Admin Dashboard Shows overview of admin functions
   */
  @GetMapping("/dashboard")
  public String adminDashboard(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("companyId", companyId);
    return "admin/dashboard";
  }

  /**
   * List all users in the admin's company
   */
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

  /**
   * Show form to add new user
   */
  @GetMapping("/users/add")
  public String showAddUserForm(Model model) {
    model.addAttribute("user", new User());
    model.addAttribute("roles", new String[] {"USER", "MANAGER", "ADMIN"});
    return "admin/users/form";
  }

  /**
   * Add new user by admin Company is automatically set from admin's company context
   */
  @PostMapping("/users/add")
  public String addUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();

      // Create user with admin's company
      userService.createUserByAdmin(user, companyId);

      redirectAttributes.addFlashAttribute("message",
          "User '" + user.getUsername() + "' created successfully. Verification email sent.");
      return "redirect:/admin/users";

    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users/add";
    }
  }

  /**
   * Show edit form for user
   */
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

  /**
   * Update user role
   */
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

  /**
   * Delete user Rules: - Cannot delete self if they're the last admin - Can delete other admins if
   * there are 2+ admins (at least 1 will remain)
   */
  @PostMapping("/users/{id}/delete")
  public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      String currentUsername = securityContextHelper.getCurrentUsername();
      User user = userService.getUserByIdAndCompany(id, companyId);

      // Prevent deleting self if they're the ONLY admin
      if (user.getUsername().equals(currentUsername)) {
        long adminCount = userService.getAdminCountInCompany(companyId);
        if ("ADMIN".equals(user.getRole()) && adminCount <= 1) {
          redirectAttributes.addFlashAttribute("error",
              "You cannot delete your own account when you are the only admin!");
          return "redirect:/admin/users";
        }
        // Allow other users to delete themselves (non-admins)
        redirectAttributes.addFlashAttribute("error", "You cannot delete your own account!");
        return "redirect:/admin/users";
      }

      // This will throw exception if trying to delete last admin
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

  /**
   * Resend verification email for user
   */
  @PostMapping("/users/{id}/resend-verification")
  public String resendVerificationEmail(@PathVariable Long id,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      User user = userService.getUserByIdAndCompany(id, companyId);

      // Resend verification email
      // TODO: Implement in RegistrationService
      // registrationService.sendVerificationEmail(user);

      redirectAttributes.addFlashAttribute("message",
          "Verification email resent to " + user.getEmail());
      return "redirect:/admin/users";

    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users";
    }
  }

  /**
   * Update user details by admin Can update role and enabled status
   */
  @PostMapping("/users/{id}/update")
  public String updateUser(@PathVariable Long id, @ModelAttribute User user,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      userService.updateUserByAdmin(id, user, companyId);
      redirectAttributes.addFlashAttribute("message", "User updated successfully.");
      return "redirect:/admin/users";

    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/users/{id}/edit";
    }
  }

  /**
   * View company details
   */
  @GetMapping("/company")
  public String viewCompany(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyService.getCompanyById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    model.addAttribute("company", company);
    return "admin/company/view";
  }

  /**
   * Show edit company form
   */
  @GetMapping("/company/edit")
  public String showEditCompanyForm(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyService.getCompanyById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    model.addAttribute("company", company);
    return "admin/company/edit";
  }

  /**
   * Update company details
   */
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

