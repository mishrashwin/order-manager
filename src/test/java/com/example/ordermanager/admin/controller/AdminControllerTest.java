package com.example.ordermanager.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/**
 * Admin Controller Test - verifies user management endpoints and error handling
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock
  private UserService userService;
  @Mock
  private CompanyService companyService;
  @Mock
  private SecurityContextHelper securityContextHelper;

  private AdminController adminController;

  @BeforeEach
  void setUp() {
    adminController = new AdminController(userService, companyService, securityContextHelper);
  }

  @Test
  void addUser_success_redirectsToUserList() {
    User user = new User();
    user.setUsername("newuser");
    user.setEmail("newuser@acme.com");
    user.setMobileNumber("+919876543210");
    user.setPassword("SecurePass123");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);

    String view = adminController.addUser(user, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("User 'newuser' created successfully. Verification email sent.");
  }

  @Test
  void addUser_mobileValidationFailure_returnsFormWithInlineMobileError() {
    User user = new User();
    user.setUsername("newuser");
    user.setEmail("newuser@acme.com");
    user.setMobileNumber("12345");
    user.setPassword("SecurePass123");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    doThrow(new IllegalArgumentException(
        "Mobile number is invalid. Use country code + mobile number (for example +919876543210)."))
        .when(userService).createUserByAdmin(user, 5L);

    String view = adminController.addUser(user, model, redirectAttributes);

    assertThat(view).isEqualTo("admin/users/form");
    assertThat(model.getAttribute("error")).isEqualTo(
        "Mobile number is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("mobileError")).isEqualTo(
        "Mobile number is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("user")).isEqualTo(user);
    assertThat(model.getAttribute("roles")).isEqualTo(new String[] {"USER", "MANAGER", "ADMIN"});
  }

  @Test
  void updateUser_success_redirectsToUserList() {
    User user = new User();
    user.setId(20L);
    user.setUsername("existinguser");
    user.setEmail("existing@acme.com");
    user.setMobileNumber("+919876543210");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);

    String view = adminController.updateUser(20L, user, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("User updated successfully.");
  }

  @Test
  void updateUser_mobileValidationFailure_returnsEditFormWithInlineMobileError() {
    User submittedUser = new User();
    submittedUser.setId(20L);
    submittedUser.setUsername("existinguser");
    submittedUser.setEmail("existing@acme.com");
    submittedUser.setMobileNumber("invalid");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    User existingUser = new User();
    existingUser.setId(20L);
    existingUser.setUsername("existinguser");
    existingUser.setEmail("existing@acme.com");
    existingUser.setMobileNumber("+919876543210");

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    doThrow(new IllegalArgumentException(
        "Mobile number is invalid. Please provide valid mobile number.")).when(userService)
        .updateUserByAdmin(20L, submittedUser, 5L);
    when(userService.getUserByIdAndCompany(20L, 5L)).thenReturn(existingUser);

    String view = adminController.updateUser(20L, submittedUser, model, redirectAttributes);

    assertThat(view).isEqualTo("admin/users/form-edit");
    assertThat(model.getAttribute("error"))
        .isEqualTo("Mobile number is invalid. Please provide valid mobile number.");
    assertThat(model.getAttribute("mobileError"))
        .isEqualTo("Mobile number is invalid. Please provide valid mobile number.");
    assertThat(model.getAttribute("user")).isEqualTo(existingUser);
    assertThat(model.getAttribute("roles")).isEqualTo(new String[] {"USER", "MANAGER", "ADMIN"});
  }
}

