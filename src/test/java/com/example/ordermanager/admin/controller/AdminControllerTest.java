package com.example.ordermanager.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.example.ordermanager.admin.dto.ClientOrderStatDTO;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.time.LocalDate;
import java.util.List;
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
  @Mock
  private OrderService orderService;
  @Mock
  private PasswordVerificationService passwordVerificationService;

  private AdminController adminController;

  @BeforeEach
  void setUp() {
    adminController = new AdminController(userService, companyService, securityContextHelper,
        orderService, passwordVerificationService);
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

  @Test
  void toggleUserStatus_activate_redirectsWithSuccessMessage() {
    User user = new User();
    user.setId(30L);
    user.setUsername("targetuser");
    user.setAccountActive(false);

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("adminuser");
    when(userService.getUserByIdAndCompany(30L, 5L)).thenReturn(user);

    String view = adminController.toggleUserStatus(30L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("User 'targetuser' has been activated successfully.");
  }

  @Test
  void toggleUserStatus_deactivate_redirectsWithSuccessMessage() {
    User user = new User();
    user.setId(31L);
    user.setUsername("targetuser");
    user.setAccountActive(true);

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("adminuser");
    when(userService.getUserByIdAndCompany(31L, 5L)).thenReturn(user);

    String view = adminController.toggleUserStatus(31L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("User 'targetuser' has been deactivated successfully.");
  }

  @Test
  void toggleUserStatus_selfToggle_redirectsWithError() {
    User user = new User();
    user.setId(32L);
    user.setUsername("adminuser");
    user.setAccountActive(true);

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("adminuser");
    when(userService.getUserByIdAndCompany(32L, 5L)).thenReturn(user);

    String view = adminController.toggleUserStatus(32L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("You cannot change the active status of your own account!");
  }

  @Test
  void toggleUserStatus_userNotInCompany_redirectsWithError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("adminuser");
    doThrow(new IllegalArgumentException("User not found or does not belong to your company"))
        .when(userService).getUserByIdAndCompany(99L, 5L);

    String view = adminController.toggleUserStatus(99L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("User not found or does not belong to your company");
  }

  // ── Order Statistics endpoint tests ──────────────────────────────────────────

  @Test
  void showOrderStatistics_noParams_usesDefaultDateRangeAndReturnsView() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getClientOrderStats(eq(5L), any(LocalDate.class), any(LocalDate.class),
        eq("ALL"))).thenReturn(List.of());

    Model model = new ConcurrentModel();
    String view = adminController.showOrderStatistics(null, null, null, null, "ALL", model);

    assertThat(view).isEqualTo("admin/order-statistics");
    assertThat(model.getAttribute("stats")).isEqualTo(List.of());
    assertThat(model.getAttribute("totalOrders")).isEqualTo(0L);
    assertThat(model.getAttribute("statusFilter")).isEqualTo("ALL");
    assertThat(model.getAttribute("selectedClientOrders")).isNull();
  }

  @Test
  void showOrderStatistics_withClientId_populatesDrillDownOrders() {
    LocalDate start = LocalDate.now().minusMonths(1);
    LocalDate end = LocalDate.now();

    ClientOrderStatDTO stat = new ClientOrderStatDTO(10L, "ACME CORP", 3, 900.0, 100.0);
    List<ClientOrderStatDTO> stats = List.of(stat);

    Order o1 = new Order();
    o1.setStatus(OrderStatus.CREATED);
    o1.setOrderDate(LocalDate.now());
    List<Order> drillOrders = List.of(o1);

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getClientOrderStats(eq(5L), any(), any(), eq("ALL"))).thenReturn(stats);
    when(orderService.getOrdersByClientAndDateRange(eq(5L), eq(10L), anyString(), any(), any(),
        eq("ALL"))).thenReturn(drillOrders);

    Model model = new ConcurrentModel();
    String view = adminController.showOrderStatistics(start.toString(), end.toString(), 10L,
        "ACME CORP", "ALL", model);

    assertThat(view).isEqualTo("admin/order-statistics");
    assertThat(model.getAttribute("selectedClientId")).isEqualTo(10L);
    assertThat(model.getAttribute("selectedClientName")).isEqualTo("ACME CORP");
    assertThat(model.getAttribute("selectedClientOrders")).isEqualTo(drillOrders);
  }

  @Test
  void showOrderStatistics_withClientIdOnly_resolvesClientNameFromStats() {
    LocalDate start = LocalDate.now().minusMonths(1);
    LocalDate end = LocalDate.now();

    ClientOrderStatDTO stat = new ClientOrderStatDTO(10L, "BETA LTD", 2, 400.0, 100.0);
    List<ClientOrderStatDTO> stats = List.of(stat);

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getClientOrderStats(eq(5L), any(), any(), eq("ALL"))).thenReturn(stats);
    when(orderService.getOrdersByClientAndDateRange(eq(5L), eq(10L), eq("BETA LTD"), any(), any(),
        eq("ALL"))).thenReturn(List.of());

    Model model = new ConcurrentModel();
    // clientName param is null – should be resolved from stats
    String view = adminController.showOrderStatistics(start.toString(), end.toString(), 10L, null,
        "ALL", model);

    assertThat(view).isEqualTo("admin/order-statistics");
    assertThat(model.getAttribute("selectedClientName")).isEqualTo("BETA LTD");
  }

  @Test
  void showOrderStatistics_withStatusFilterActive_passesFilterToService() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getClientOrderStats(eq(5L), any(), any(), eq("ACTIVE")))
        .thenReturn(List.of());

    Model model = new ConcurrentModel();
    String view = adminController.showOrderStatistics(null, null, null, null, "ACTIVE", model);

    assertThat(view).isEqualTo("admin/order-statistics");
    assertThat(model.getAttribute("statusFilter")).isEqualTo("ACTIVE");
  }

  @Test
  void showOrderStatistics_totalOrdersSummedFromStats() {
    ClientOrderStatDTO s1 = new ClientOrderStatDTO(1L, "ALPHA", 4, 1000.0, 40.0);
    ClientOrderStatDTO s2 = new ClientOrderStatDTO(2L, "BETA", 6, 1500.0, 60.0);

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getClientOrderStats(eq(5L), any(), any(), eq("ALL")))
        .thenReturn(List.of(s1, s2));

    Model model = new ConcurrentModel();
    adminController.showOrderStatistics(null, null, null, null, "ALL", model);

    assertThat(model.getAttribute("totalOrders")).isEqualTo(10L);
  }

}
