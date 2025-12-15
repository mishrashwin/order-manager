package com.example.ordermanager.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String token;

  @OneToOne
  @JoinColumn(nullable = false, name = "user_id")
  private User user;

  private LocalDateTime expiryDate;
}
