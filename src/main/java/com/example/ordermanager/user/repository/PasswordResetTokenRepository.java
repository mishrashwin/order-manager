package com.example.ordermanager.user.repository;

import com.example.ordermanager.user.entity.PasswordResetToken;
import com.example.ordermanager.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByUser(User user);

  Optional<PasswordResetToken> findByUserEmail(String email);

  Optional<PasswordResetToken> findByCode(String code);
}

