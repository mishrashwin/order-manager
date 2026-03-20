package com.example.ordermanager.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.config.GlobalModelAttributes;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class GlobalModelAttributesTest {

  @Mock
  private SecurityContextHelper securityContextHelper;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void cachesResolvedCompanyNameAndGreetingPerRequest() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "tenant-user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    when(securityContextHelper.isOwnerContext()).thenReturn(false);

    Company company = new Company();
    company.setName("ACME INC");

    User user = new User();
    user.setFirstName("Alice");
    user.setCompany(company);
    when(securityContextHelper.getUserFromContext()).thenReturn(user);

    GlobalModelAttributes advice = new GlobalModelAttributes(securityContextHelper);
    MockHttpServletRequest request = new MockHttpServletRequest();

    ExtendedModelMap model1 = new ExtendedModelMap();
    advice.addCompanyNameToModel(model1, request);

    assertThat(model1.get("companyName")).isEqualTo("ACME INC");
    assertThat(model1.get("navbarGreetingName")).isEqualTo("Alice");

    ExtendedModelMap model2 = new ExtendedModelMap();
    advice.addCompanyNameToModel(model2, request);

    assertThat(model2.get("companyName")).isEqualTo("ACME INC");
    assertThat(model2.get("navbarGreetingName")).isEqualTo("Alice");
    verify(securityContextHelper, times(1)).getUserFromContext();
  }

  @Test
  void cachesOwnerContextAndSkipsUserLookup() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "owner-user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

    when(securityContextHelper.isOwnerContext()).thenReturn(true);

    GlobalModelAttributes advice = new GlobalModelAttributes(securityContextHelper);
    MockHttpServletRequest request = new MockHttpServletRequest();

    ExtendedModelMap model1 = new ExtendedModelMap();
    advice.addCompanyNameToModel(model1, request);

    ExtendedModelMap model2 = new ExtendedModelMap();
    advice.addCompanyNameToModel(model2, request);

    assertThat(model1.get("companyName")).isEqualTo("Owner Console");
    assertThat(model2.get("companyName")).isEqualTo("Owner Console");
    assertThat(model2.get("navbarGreetingName")).isEqualTo("owner-user");
    verify(securityContextHelper, never()).getUserFromContext();
  }
}


