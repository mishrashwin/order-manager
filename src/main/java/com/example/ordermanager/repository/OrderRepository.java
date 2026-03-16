package com.example.ordermanager.repository;

import com.example.ordermanager.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByCompanyId(Long companyId);

  List<Order> findByCompanyIdAndOrderDateBetween(Long companyId, LocalDate startDate,
      LocalDate endDate);

  /**
   * Count orders associated with a specific client Used for validation before client deletion
   *
   * @param clientId Client ID
   * @return Number of orders for this client
   */
  long countByClientId(Long clientId);
}
