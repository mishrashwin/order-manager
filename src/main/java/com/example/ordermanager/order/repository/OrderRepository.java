package com.example.ordermanager.order.repository;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByCompanyId(Long companyId);

  @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
  List<Order> findByCompanyIdAndOrderDateBetween(Long companyId, LocalDate startDate,
      LocalDate endDate);

  @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
  Optional<Order> findByIdAndCompanyId(Long id, Long companyId);

  @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
  @Query("SELECT o FROM Order o WHERE o.id = :id")
  Optional<Order> findDetailedById(@Param("id") Long id);

  /**
   * Count orders associated with a specific client. Used for validation before client deletion.
   *
   * @param clientId Client ID
   * @return Number of orders for this client
   */
  long countByClientId(Long clientId);

  /**
   * TENANT-AWARE: Get orders by company and delivery date range. Used for fetching orders for
   * urgent notification filtering.
   *
   * @param companyId Company ID
   * @param fromDate Start date for delivery window
   * @param toDate End date for delivery window
   * @return List of orders within the delivery date range
   */
  List<Order> findByCompanyIdAndDeliveryDateBetween(Long companyId, LocalDate fromDate,
      LocalDate toDate);

  /**
   * TENANT-AWARE: Get orders by company with delivery date on or before a given date. Used for
   * fetching overdue and upcoming urgent orders for dashboard notifications.
   *
   * @param companyId Company ID
   * @param toDate Upper bound for delivery date (inclusive); orders with null delivery date are
   *        excluded by JPA
   * @return List of orders with delivery date less than or equal to the cutoff
   */
  @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
  List<Order> findByCompanyIdAndDeliveryDateLessThanEqual(Long companyId, LocalDate toDate);

  /**
   * Aggregate order statistics per client for a company within a date range. Returns one row per
   * (client.id, client.name, customerName) group with order count and total amount. A LEFT JOIN is
   * used so that legacy orders without a client relationship are included as a separate group.
   *
   * <p>
   * Each row contains: [0] client.id (Long, nullable), [1] client.name (String, nullable), [2]
   * customerName (String, nullable), [3] order count (Long), [4] total amount (Double).
   *
   * @param companyId Company ID
   * @param startDate Start of order date range (inclusive)
   * @param endDate End of order date range (inclusive)
   * @param statuses Set of OrderStatus values to include
   * @return Rows sorted by order count descending
   */
  @Query("SELECT c.id, c.name, o.customerName, COUNT(o.id), COALESCE(SUM(o.totalAmount), 0.0) "
      + "FROM Order o LEFT JOIN o.client c " + "WHERE o.company.id = :companyId "
      + "AND o.orderDate BETWEEN :startDate AND :endDate " + "AND o.status IN :statuses "
      + "GROUP BY c.id, c.name, o.customerName " + "ORDER BY COUNT(o.id) DESC")
  List<Object[]> findClientOrderStats(@Param("companyId") Long companyId,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
      @Param("statuses") Collection<OrderStatus> statuses);

  /**
   * TENANT-AWARE: Get orders for a specific client (by client entity ID) within a date range. Used
   * for the Order Statistics drill-down table.
   *
   * @param companyId Company ID
   * @param startDate Start of order date range (inclusive)
   * @param endDate End of order date range (inclusive)
   * @param clientId Client entity ID
   * @param statuses Set of OrderStatus values to include
   * @return Orders sorted by order date descending
   */
  @Query("SELECT o FROM Order o " + "WHERE o.company.id = :companyId "
      + "AND o.orderDate BETWEEN :startDate AND :endDate " + "AND o.client.id = :clientId "
      + "AND o.status IN :statuses " + "ORDER BY o.orderDate DESC NULLS LAST")
  List<Order> findByCompanyAndDateRangeAndClientId(@Param("companyId") Long companyId,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
      @Param("clientId") Long clientId, @Param("statuses") Collection<OrderStatus> statuses);

  /**
   * TENANT-AWARE: Get legacy orders (no client relationship) matched by customer name within a date
   * range. Used for the Order Statistics drill-down table when clientId is null.
   *
   * @param companyId Company ID
   * @param startDate Start of order date range (inclusive)
   * @param endDate End of order date range (inclusive)
   * @param customerName Customer name to match (case-insensitive)
   * @param statuses Set of OrderStatus values to include
   * @return Orders sorted by order date descending
   */
  @Query("SELECT o FROM Order o " + "WHERE o.company.id = :companyId "
      + "AND o.orderDate BETWEEN :startDate AND :endDate " + "AND o.client IS NULL "
      + "AND UPPER(o.customerName) = UPPER(:customerName) " + "AND o.status IN :statuses "
      + "ORDER BY o.orderDate DESC NULLS LAST")
  List<Order> findByCompanyAndDateRangeAndCustomerName(@Param("companyId") Long companyId,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
      @Param("customerName") String customerName,
      @Param("statuses") Collection<OrderStatus> statuses);
}
