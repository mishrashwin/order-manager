package com.example.ordermanager.order.repository;

import com.example.ordermanager.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByProductId(Long productId);

  /**
   * Fetches all OrderItems whose product belongs to a given company, in a single query. Used to
   * build the order-usage map for the product list page without N+1 queries.
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.product IS NOT NULL AND oi.product.company.id = :companyId")
  List<OrderItem> findByProductCompanyId(@Param("companyId") Long companyId);
}
