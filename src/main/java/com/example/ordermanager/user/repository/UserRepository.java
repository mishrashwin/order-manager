package com.example.ordermanager.user.repository;

import com.example.ordermanager.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  Optional<User> findByMobileNumber(String mobileNumber);

  Optional<User> findFirstByCompanyIdAndRoleOrderByIdAsc(Long companyId, String role);

  List<User> findByCompanyId(Long companyId);

  long countByCompanyId(Long companyId);

  long countByCompanyIdAndRole(Long companyId, String role);
}
