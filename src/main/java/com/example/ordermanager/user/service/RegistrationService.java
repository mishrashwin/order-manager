package com.example.ordermanager.user.service;

import com.example.ordermanager.exception.EmailAlreadySentException;
import com.example.ordermanager.repository.CompanyRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.entity.VerificationToken;
import com.example.ordermanager.user.repository.UserRepository;
import com.example.ordermanager.user.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegistrationService {

  private final UserRepository userRepository;
  private final VerificationTokenRepository tokenRepository;
  private final EmailService emailService;
  private final CompanyRepository companyRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Value("${app.base.url}")
  private String baseUrl;

  public RegistrationService(UserRepository userRepository,
      VerificationTokenRepository tokenRepository, EmailService emailService,
      CompanyRepository companyRepository) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.emailService = emailService;
    this.companyRepository = companyRepository;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  public void sendVerificationEmail(User user) {
    Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);

    VerificationToken newToken = null;
    if (existingTokenOpt.isPresent()) {
      VerificationToken existingToken = existingTokenOpt.get();

      // Check if token still valid
      if (existingToken.getExpiryDate().isAfter(LocalDateTime.now())) {
        // Don't resend if still valid
        throw new EmailAlreadySentException(
            "Verification email has already been sent. Please check your inbox.");
      } else {
        // Token expired — update it
        existingToken.setToken(UUID.randomUUID().toString());
        existingToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        tokenRepository.save(existingToken);
      }
    } else {
      // create new token
      newToken = new VerificationToken(null, UUID.randomUUID().toString(), user,
          LocalDateTime.now().plusHours(24));
      tokenRepository.save(newToken);
    }

    // send email
    String verifyUrl = baseUrl + "/verify?token="
        + (existingTokenOpt.isPresent() ? existingTokenOpt.get().getToken() : newToken.getToken());
    emailService.sendVerificationEmail(user.getEmail(), verifyUrl);
  }


  public boolean verifyToken(String token) {
    var optToken = tokenRepository.findByToken(token);
    if (optToken.isEmpty())
      return false;

    VerificationToken vt = optToken.get();
    if (vt.getExpiryDate().isBefore(LocalDateTime.now()))
      return false;

    User user = vt.getUser();
    user.setEnabled(true);
    userRepository.save(user);

    tokenRepository.delete(vt);
    return true;
  }
}
