package com.example.ordermanager.company.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.ordermanager.company.entity.CompanyAddress;
import com.example.ordermanager.company.dto.CompanyAddressForm;
import com.example.ordermanager.company.service.CompanyAddressService;
import com.example.ordermanager.utils.IndianState;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

/** MVC coverage for validation, authorization, and feedback on Company Address actions. */
@WebMvcTest(CompanyAddressController.class)
@Import(CompanyAddressControllerTest.AdminPathSecurityConfiguration.class)
class CompanyAddressControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CompanyAddressService addressService;

  @MockBean
  private SecurityContextHelper securityContextHelper;

  /** Applies the production ADMIN path rule in the MVC slice test context. */
  @TestConfiguration
  static class AdminPathSecurityConfiguration {

    /** Restricts address management routes to ADMIN and retains MVC CSRF protection. */
    @org.springframework.context.annotation.Bean
    SecurityFilterChain addressTestSecurityFilterChain(HttpSecurity http) throws Exception {
      http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/admin/**")
          .hasRole("ADMIN").anyRequest().authenticated());
      http.csrf(Customizer.withDefaults());
      return http.build();
    }
  }

  /** Lists the Company Address management page for an authenticated administrator. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void list_returnsAddressManagementView() throws Exception {
    when(addressService.listAddresses()).thenReturn(List.of(new CompanyAddress()));

    mockMvc.perform(get("/admin/company/addresses"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/company/addresses"));

    verify(addressService).listAddresses();
  }

  /** Opens a blank address form with the supported state options. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void newForm_returnsAddressForm() throws Exception {
    mockMvc.perform(get("/admin/company/addresses/new")).andExpect(status().isOk())
        .andExpect(view().name("admin/company/address-form"));
  }

  /** Creates an address from valid form data and redirects with confirmation feedback. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void create_validFormRedirectsWithSuccessMessage() throws Exception {
    mockMvc
        .perform(post("/admin/company/addresses").with(csrf()).param("nickname", "Warehouse")
            .param("addressLine1", "12 Market Road").param("city", "Mumbai")
            .param("state", "MAHARASHTRA").param("postalCode", "400001").param("country", "India")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("message", "Company address added successfully."));

    verify(addressService).createAddress(any());
  }

  /** Returns field validation errors without calling the address service for an invalid form. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void create_missingRequiredFieldsReturnsValidationErrors() throws Exception {
    mockMvc
        .perform(post("/admin/company/addresses").with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk()).andExpect(view().name("admin/company/address-form"));

    verify(addressService, never()).createAddress(any());
  }

  /** Loads a Company-scoped address into the edit view. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void editForm_returnsAddressForm() throws Exception {
    CompanyAddress address = new CompanyAddress();
    address.setId(42L);
    address.setNickname("Main Office");
    address.setAddressLine1("12 Market Road");
    address.setCity("Mumbai");
    address.setState(IndianState.MAHARASHTRA);
    address.setPostalCode("400001");
    address.setCountry("India");
    when(addressService.getAddress(42L)).thenReturn(address);

    mockMvc.perform(get("/admin/company/addresses/42/edit")).andExpect(status().isOk())
        .andExpect(view().name("admin/company/address-form"));
  }

  /** Redirects safely when an address ID is outside the authenticated Company's scope. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void editForm_missingCompanyAddressRedirectsWithError() throws Exception {
    when(addressService.getAddress(42L)).thenThrow(new IllegalArgumentException("Company address not found"));

    mockMvc.perform(get("/admin/company/addresses/42/edit"))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("error", "Company address not found"));
  }

  /** Updates a valid address form and redirects with confirmation feedback. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void update_validFormRedirectsWithSuccessMessage() throws Exception {
    mockMvc
        .perform(post("/admin/company/addresses/42").with(csrf()).param("nickname", "Warehouse")
            .param("addressLine1", "12 Market Road").param("city", "Mumbai")
            .param("state", "MAHARASHTRA").param("postalCode", "400001").param("country", "India")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("message", "Company address updated successfully."));

    verify(addressService).updateAddress(org.mockito.ArgumentMatchers.eq(42L),
        any(CompanyAddressForm.class));
  }

  /** Returns field validation errors and does not invoke update for an incomplete form. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void update_missingRequiredFieldsReturnsValidationErrors() throws Exception {
    mockMvc
        .perform(post("/admin/company/addresses/42").with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk()).andExpect(view().name("admin/company/address-form"));

    verify(addressService, never()).updateAddress(org.mockito.ArgumentMatchers.eq(42L), any());
  }

  /** Changes the Bill To default through the tenant-scoped service and reports confirmation. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void setDefaultBillTo_redirectsWithConfirmation() throws Exception {
    when(addressService.setDefaultBillTo(42L)).thenReturn(new CompanyAddress());

    mockMvc.perform(post("/admin/company/addresses/42/default-bill-to").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("message", "Default Bill To address updated."));

    verify(addressService).setDefaultBillTo(42L);
  }

  /** Changes the Ship To default and returns positive confirmation. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void setDefaultShipTo_redirectsWithConfirmation() throws Exception {
    when(addressService.setDefaultShipTo(42L)).thenReturn(new CompanyAddress());

    mockMvc.perform(post("/admin/company/addresses/42/default-ship-to").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("message", "Default Ship To address updated."));

    verify(addressService).setDefaultShipTo(42L);
  }

  /** Deactivates an eligible non-default address and returns confirmation. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void deactivate_redirectsWithConfirmation() throws Exception {
    when(addressService.deactivateAddress(42L)).thenReturn(new CompanyAddress());

    mockMvc.perform(post("/admin/company/addresses/42/deactivate").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("message", "Company address deactivated."));

    verify(addressService).deactivateAddress(42L);
  }

  /** Reports a rejected default deactivation without hiding its service error. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void deactivate_defaultAddressShowsError() throws Exception {
    when(addressService.deactivateAddress(42L))
        .thenThrow(new IllegalStateException("Choose replacement default address roles first"));

    mockMvc.perform(post("/admin/company/addresses/42/deactivate").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("error", "Choose replacement default address roles first"));
  }

  /** Reactivates the selected Company address and reports completion to the administrator. */
  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void activate_redirectsWithConfirmation() throws Exception {
    when(addressService.activateAddress(42L)).thenReturn(new CompanyAddress());

    mockMvc.perform(post("/admin/company/addresses/42/activate").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("message", "Company address activated."));

    verify(addressService).activateAddress(42L);
  }

  /** Prevents non-administrator roles from opening Company Address management. */
  @Test
  @WithMockUser(username = "user@example.com", roles = "USER")
  void list_nonAdminIsForbidden() throws Exception {
    mockMvc.perform(get("/admin/company/addresses")).andExpect(status().isForbidden());
  }
}
