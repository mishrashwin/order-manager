package com.example.ordermanager.order.repository;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  List<Order> findByCompanyId(Long companyId);

  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  Page<Order> findByCompanyId(Long companyId, Pageable pageable);

  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  @Query("SELECT o FROM Order o WHERE o.company.id = :companyId AND " +
         "(LOWER(o.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
         "LOWER(o.poOrderNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
         "LOWER(o.client.name) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Order> searchOrders(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  List<Order> findByCompanyIdAndOrderDateBetween(Long companyId, LocalDate startDate,
      LocalDate endDate);

  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  Optional<Order> findByIdAndCompanyId(Long id, Long companyId);

  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
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
   * TENANT-AWARE: Get orders by company with delivery date on or before a given date. Used for
   * fetching overdue and upcoming urgent orders for dashboard notifications.
   *
   * @param companyId Company ID
   * @param toDate Upper bound for delivery date (inclusive); orders with null delivery date are
   *        excluded by JPA
   * @return List of orders with delivery date less than or equal to the cutoff
   */
  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  List<Order> findByCompanyIdAndDeliveryDateLessThanEqual(Long companyId, LocalDate toDate);

  /**
   * Aggregate order statistics per client for a company within a date range. A LEFT JOIN is used so
   * orphaned legacy rows with no client relationship still appear as a single nullable-client
   * group.
   *
   * <p>
   * Each row contains: [0] client.id (Long, nullable), [1] client.name (String, nullable), [2]
   * order count (Long), [3] total amount (Double).
   */
  @Query("SELECT c.id, c.name, COUNT(o.id), COALESCE(SUM(o.totalAmount), 0.0) "
      + "FROM Order o LEFT JOIN o.client c " + "WHERE o.company.id = :companyId "
      + "AND o.orderDate BETWEEN :startDate AND :endDate " + "AND o.status IN :statuses "
      + "GROUP BY c.id, c.name " + "ORDER BY COUNT(o.id) DESC")
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
  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  @Query("SELECT o FROM Order o " + "WHERE o.company.id = :companyId "
      + "AND o.orderDate BETWEEN :startDate AND :endDate " + "AND o.client.id = :clientId "
      + "AND o.status IN :statuses " + "ORDER BY o.orderDate DESC NULLS LAST")
  List<Order> findByCompanyAndDateRangeAndClientId(@Param("companyId") Long companyId,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
      @Param("clientId") Long clientId, @Param("statuses") Collection<OrderStatus> statuses);

  /**
   * TENANT-AWARE: Get orders with no client relationship within a date range. Used for drill-down
   * on the nullable-client "Unknown" group in order statistics.
   */
  @EntityGraph(attributePaths = {"company", "client", "orderItems", "orderItems.product"})
  @Query("SELECT o FROM Order o " + "WHERE o.company.id = :companyId "
      + "AND o.orderDate BETWEEN :startDate AND :endDate " + "AND o.client IS NULL "
      + "AND o.status IN :statuses " + "ORDER BY o.orderDate DESC NULLS LAST")
  List<Order> findByCompanyAndDateRangeAndNoClient(@Param("companyId") Long companyId,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
      @Param("statuses") Collection<OrderStatus> statuses);
}
