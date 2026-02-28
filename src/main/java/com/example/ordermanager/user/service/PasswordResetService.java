package com.example.ordermanager.user.service;

import com.example.ordermanager.exception.EmailAlreadySentException;
import com.example.ordermanager.user.entity.PasswordResetToken;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.PasswordResetTokenRepository;
import com.example.ordermanager.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;
  private final BCryptPasswordEncoder passwordEncoder;

  public PasswordResetService(PasswordResetTokenRepository tokenRepository,
      UserRepository userRepository, EmailService emailService) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.emailService = emailService;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  /**
   * Generate and send a 6-digit password reset code to user's email
   *
   * @param user User requesting password reset
   * @throws EmailAlreadySentException If code already sent recently
   */
  public void sendPasswordResetCode(User user) throws EmailAlreadySentException {
    Optional<PasswordResetToken> existingTokenOpt = tokenRepository.findByUser(user);

    PasswordResetToken newToken = null;

    if (existingTokenOpt.isPresent()) {
      PasswordResetToken existingToken = existingTokenOpt.get();

      // Check if token still valid and not yet verified
      if (existingToken.getExpiryDate().isAfter(LocalDateTime.now())
          && !existingToken.isVerified()) {
        throw new EmailAlreadySentException(
            "A password reset code has already been sent. Please check your email or wait for it to expire.");
      } else {
        // Token expired or already verified — create a new one
        existingToken.setCode(generateSixDigitCode());
        existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        existingToken.setVerified(false);
        tokenRepository.save(existingToken);
      }
    } else {
      // Create new token
      newToken = new PasswordResetToken(null, generateSixDigitCode(), user,
          LocalDateTime.now().plusMinutes(15), false, LocalDateTime.now());
      tokenRepository.save(newToken);
    }

    // Send email with code
    String code =
        existingTokenOpt.isPresent() ? existingTokenOpt.get().getCode() : newToken.getCode();
    emailService.sendPasswordResetEmail(user.getEmail(), code);
  }

  /**
   * Verify the 6-digit code
   *
   * @param code The 6-digit code entered by user
   * @return User if code is valid and not expired, null otherwise
   */
  public User verifyPasswordResetCode(String code) {
    Optional<PasswordResetToken> optToken = tokenRepository.findByCode(code);

    if (optToken.isEmpty()) {
      return null;
    }

    PasswordResetToken token = optToken.get();

    // Check if code is expired
    if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
      return null;
    }

    // Mark as verified
    token.setVerified(true);
    tokenRepository.save(token);

    return token.getUser();
  }

  /**
   * Reset password with verified code
   *
   * @param code The verified 6-digit code
   * @param newPassword New password to set
   * @return true if password reset successfully
   */
  public boolean resetPasswordWithCode(String code, String newPassword) {
    Optional<PasswordResetToken> optToken = tokenRepository.findByCode(code);

    if (optToken.isEmpty()) {
      return false;
    }

    PasswordResetToken token = optToken.get();

    // Check if code is expired or not verified
    if (token.getExpiryDate().isBefore(LocalDateTime.now()) || !token.isVerified()) {
      return false;
    }

    User user = token.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    // Delete the token after successful reset
    tokenRepository.delete(token);

    return true;
  }

  /**
   * Generate a random 6-digit code
   *
   * @return Random 6-digit code as string
   */
  private String generateSixDigitCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }
}

