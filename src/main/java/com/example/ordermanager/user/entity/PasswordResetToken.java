package com.example.ordermanager.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String code; // 6-digit code

  @OneToOne
  @JoinColumn(nullable = false, name = "user_id")
  private User user;

  private LocalDateTime expiryDate;

  private boolean verified = false; // Flag to track if code has been verified

  private LocalDateTime createdAt = LocalDateTime.now();

  private int failedAttempts = 0;

  public static final int MAX_ATTEMPTS = 5;
}

