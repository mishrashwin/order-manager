package com.example.ordermanager.user.repository;

import com.example.ordermanager.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  Optional<User> findByMobileNumber(String mobileNumber);

  Optional<User> findFirstByCompanyIdAndRoleOrderByIdAsc(Long companyId, String role);

  List<User> findByCompanyId(Long companyId);

  long countByCompanyId(Long companyId);

  long countByCompanyIdAndRole(Long companyId, String role);

  @Query("SELECT u.company.id AS companyId, COUNT(u) AS userCount FROM User u WHERE u.company.id IN :companyIds GROUP BY u.company.id")
  List<CompanyUserCount> countByCompanyIdIn(@Param("companyIds") List<Long> companyIds);

  @Query("SELECT u.company.id AS companyId, COUNT(u) AS userCount FROM User u WHERE u.company.id IN :companyIds AND u.role = :role GROUP BY u.company.id")
  List<CompanyUserCount> countByCompanyIdInAndRole(@Param("companyIds") List<Long> companyIds,
      @Param("role") String role);
}
