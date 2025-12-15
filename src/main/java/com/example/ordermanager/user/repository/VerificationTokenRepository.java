package com.example.ordermanager.user.repository;

import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
  Optional<VerificationToken> findByToken(String token);

  Optional<VerificationToken> findByUser(User user);
}
