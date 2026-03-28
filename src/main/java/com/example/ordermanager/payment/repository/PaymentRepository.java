package com.example.ordermanager.payment.repository;

import com.example.ordermanager.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  List<Payment> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

  List<Payment> findAllByOrderByCreatedAtDesc();

  Optional<Payment> findByIdAndCompanyId(Long id, Long companyId);

  boolean existsByCompanyIdAndPaymentMonthAndPaymentYear(Long companyId, int paymentMonth,
      int paymentYear);
}
