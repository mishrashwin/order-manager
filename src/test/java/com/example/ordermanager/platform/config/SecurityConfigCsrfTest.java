package com.example.ordermanager.platform.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanager.company.controller.CompanyController;
import com.example.ordermanager.config.CustomUserDetailsService;
import com.example.ordermanager.config.SecurityConfig;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.controller.OrderRestController;
import com.example.ordermanager.order.service.OrderActivityService;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.user.controller.SupportController;
import com.example.ordermanager.user.repository.UserRepository;
import com.example.ordermanager.user.service.EmailService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {CompanyController.class, OrderRestController.class, SupportController.class})
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.owner.email=owner@example.com")
class SecurityConfigCsrfTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CompanyService companyService;

  @MockBean
  private SecurityContextHelper securityContextHelper;

  @MockBean
  private OrderService orderService;

  @MockBean
  private OrderActivityService orderActivityService;

  @MockBean
  private CustomUserDetailsService customUserDetailsService;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private EmailService emailService;

  @Test
  void companyRegisterGet_rendersCsrfHiddenField() throws Exception {
    mockMvc.perform(get("/company/register")).andExpect(status().isOk())
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }

  @Test
  void companyRegisterPost_withoutCsrf_redirectsToAccessDenied() throws Exception {
    mockMvc
        .perform(post("/company/register").param("companyName", "Acme Industries")
            .param("ownerFirstName", "Jane").param("ownerLastName", "Doe")
            .param("ownerEmail", "jane@example.com").param("ownerMobile", "1234567890")
            .param("username", "janedoe").param("password", "password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/access-denied?reason=forbidden"));
  }

  @Test
  void companyRegisterPost_withCsrf_allowsControllerFlow() throws Exception {
    Company savedCompany = new Company();
    savedCompany.setName("Acme Industries");
    when(companyService.registerCompanyWithAdmin(any(Company.class),
        any(com.example.ordermanager.user.entity.User.class))).thenReturn(savedCompany);

    mockMvc
        .perform(post("/company/register").with(csrf()).param("companyName", "Acme Industries")
            .param("ownerFirstName", "Jane").param("ownerLastName", "Doe")
            .param("ownerEmail", "jane@example.com").param("ownerMobile", "1234567890")
            .param("username", "janedoe").param("password", "password123"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered=true"));
  }

  @Test
  void logoutPost_withoutCsrf_redirectsToAccessDenied() throws Exception {
    mockMvc.perform(post("/logout")).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/access-denied?reason=forbidden"));
  }

  @Test
  void logoutPost_withCsrf_redirectsToLogin() throws Exception {
    mockMvc.perform(post("/logout").with(csrf()).with(user("tenantUser").roles("USER")))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
  }

  @Test
  void apiPost_withoutCsrf_isAllowedBecauseApiPathIsIgnored() throws Exception {
    Order saved = new Order();
    saved.setId(1L);
    saved.setCustomerName("Acme");
    saved.setProductName("Widget");
    when(orderService.createOrder(any(Order.class))).thenReturn(saved);

    mockMvc.perform(post("/api/orders").with(user("apiUser").roles("USER"))
        .contentType(MediaType.APPLICATION_JSON).content(
            "{\"customerName\":\"Acme\",\"productName\":\"Widget\",\"quantity\":1,\"totalAmount\":10.0}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void supportPost_withoutCsrf_isAllowedBecauseSupportPathIsIgnored() throws Exception {
    mockMvc
        .perform(post("/support").param("name", "Jane Doe").param("email", "jane@example.com")
            .param("mobile", "9999999999").param("subject", "Login issue")
            .param("description", "Unable to login"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/support?submitted"));
  }
}


