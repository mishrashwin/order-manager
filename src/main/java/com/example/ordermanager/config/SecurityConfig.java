package com.example.ordermanager.config;

import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Security Configuration - Using DaoAuthenticationProvider with CustomUserDetailsService for
 * username/password authentication - Intentionally configured to suppress warning about
 * UserDetailsService beans
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * DaoAuthenticationProvider configured with CustomUserDetailsService This is intentional and
   * properly configured
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider(
      CustomUserDetailsService customUserDetailsService, BCryptPasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);
    return authProvider;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      DaoAuthenticationProvider authenticationProvider, AccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"),
        new AntPathRequestMatcher("/support"))).authenticationProvider(authenticationProvider)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/signup", "/verify", "/resend-verification",
                "/company/register", "/forgot-password", "/verify-reset-code", "/reset-password",
                "/access-denied", "/support", "/css/**", "/js/**", "/images/**")
            .permitAll().requestMatchers("/owner/**").hasRole("OWNER").requestMatchers("/admin/**")
            .hasRole("ADMIN")
            .requestMatchers("/dashboard", "/orders/**", "/clients/**", "/vendors/**")
            .hasAnyRole("USER", "MANAGER", "ADMIN").anyRequest().authenticated())
        .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler))
        .formLogin(form -> form.loginPage("/login").failureHandler(authenticationFailureHandler())
            .successHandler(authenticationSuccessHandler()).permitAll())
        .logout(
            logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());

    return http.build();
  }


  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    return new SimpleUrlAuthenticationFailureHandler() {
      @Override
      public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
          AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String redirectUrl = "/login?error";
        if (username != null && !username.isEmpty()) {
          redirectUrl += "&username="
              + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
          redirectUrl += "&reason=" + java.net.URLEncoder.encode(exception.getMessage(),
              java.nio.charset.StandardCharsets.UTF_8);
        }
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
      }
    };
  }

  @Bean
  public AuthenticationSuccessHandler authenticationSuccessHandler() {
    return (request, response, authentication) -> {
      var authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
          .collect(Collectors.toSet());
      if (authorities.contains("ROLE_OWNER")) {
        response.sendRedirect("/owner/dashboard");
        return;
      }
      if (authorities.contains("ROLE_ADMIN")) {
        response.sendRedirect("/dashboard");
        return;
      }
      response.sendRedirect("/dashboard");
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler(UserRepository userRepository) {
    return (request, response, accessDeniedException) -> {
      String reason = "forbidden";
      if (accessDeniedException instanceof CsrfException) {
        // Check if this is a login-related CSRF failure
        String requestURI = request.getRequestURI();
        String referer = request.getHeader("referer");

        // If it's a login CSRF failure, redirect back to login with a helpful message
        if ("/login".equals(requestURI) || (referer != null && referer.contains("/login"))) {
          response.sendRedirect("/login?error&reason=csrf-expired");
          return;
        }

        reason = "csrf";
      }
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated()
          && !(authentication instanceof AnonymousAuthenticationToken)) {
        userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
          if (user.getCompany() == null) {
            return;
          }
          if (!user.getCompany().isActive()) {
            request.setAttribute("deniedReason", "suspended");
            return;
          }
          if (CompanyApprovalStatus.PENDING.equals(user.getCompany().getApprovalStatus())) {
            request.setAttribute("deniedReason", "pending-approval");
            return;
          }
          if (CompanyApprovalStatus.REJECTED.equals(user.getCompany().getApprovalStatus())) {
            request.setAttribute("deniedReason", "rejected");
          }
        });
      }
      Object deniedReason = request.getAttribute("deniedReason");
      if (deniedReason != null) {
        reason = deniedReason.toString();
      }
      String redirectUrl = "/access-denied?reason="
          + java.net.URLEncoder.encode(reason, java.nio.charset.StandardCharsets.UTF_8);
      response.sendRedirect(redirectUrl);
    };
  }
}
