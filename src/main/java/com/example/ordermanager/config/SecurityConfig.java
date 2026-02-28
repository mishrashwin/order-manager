package com.example.ordermanager.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;


@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final CustomUserDetailsService customUserDetailsService;

  public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
    this.customUserDetailsService = customUserDetailsService;
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()).authenticationProvider(authenticationProvider())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/verify", "/resend-verification", "/company/register",
                "/forgot-password", "/verify-reset-code", "/reset-password", "/css/**", "/js/**",
                "/images/**")
            .permitAll().requestMatchers("/admin/**").hasAnyRole("ADMIN").anyRequest()
            .authenticated())
        .formLogin(form -> form.loginPage("/login").failureHandler(authenticationFailureHandler())
            .defaultSuccessUrl("/dashboard", true).permitAll())
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
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
      }
    };
  }
}
