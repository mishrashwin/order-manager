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
}
