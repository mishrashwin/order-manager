package com.example.ordermanager.order.repository;

import com.example.ordermanager.order.entity.OrderActivity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderActivityRepository extends JpaRepository<OrderActivity, Long> {

  long countByCompanyId(Long companyId);

  @Query("SELECT a.id FROM OrderActivity a WHERE a.companyId = :companyId "
      + "ORDER BY a.activityAt DESC, a.id DESC")
  List<Long> findLatestIdsByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

  @Modifying
  @Query("DELETE FROM OrderActivity a WHERE a.companyId = :companyId AND a.id NOT IN :retainedIds")
  void deleteByCompanyIdAndIdNotIn(@Param("companyId") Long companyId,
      @Param("retainedIds") List<Long> retainedIds);

  /**
   * TENANT-AWARE: Get activity log entries for a company within a date/time range, optionally
   * filtered by a free-text search term matched against PO number, client name, actor username,
   * actor full name, and description. Results are sorted newest-first.
   *
   * @param companyId Company ID
   * @param start Start of time window (inclusive)
   * @param end End of time window (inclusive)
   * @param search Free-text search term; pass {@code null} or blank to skip
   * @return Activity entries sorted by activityAt descending
   */
  @Query(
      value = "SELECT a FROM OrderActivity a WHERE a.companyId = :companyId "
          + "AND a.activityAt BETWEEN :start AND :end " + "AND (:search IS NULL OR :search = '' "
          + "  OR LOWER(a.orderPoNo) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.orderClientName) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.actorFullName) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))) "
          + "ORDER BY a.activityAt DESC, a.id DESC",
      countQuery = "SELECT COUNT(a) FROM OrderActivity a WHERE a.companyId = :companyId "
          + "AND a.activityAt BETWEEN :start AND :end " + "AND (:search IS NULL OR :search = '' "
          + "  OR LOWER(a.orderPoNo) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.orderClientName) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.actorFullName) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "  OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<OrderActivity> findPageByCompanyAndDateRangeAndSearch(@Param("companyId") Long companyId,
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
      @Param("search") String search, Pageable pageable);
}

