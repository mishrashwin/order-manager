package com.example.ordermanager.order.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit log entry that records every meaningful change to an Order. The order_id is stored as a
 * plain column (no FK) so entries survive order deletion and the full lifecycle remains queryable.
 */
@Entity
@Table(name = "order_activities")
@Getter
@Setter
@NoArgsConstructor
public class OrderActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Denormalized – preserved even after the order is deleted. */
  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "order_po_no")
  private String orderPoNo;

  @Column(name = "order_client_name")
  private String orderClientName;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false)
  private ActivityType activityType;

  /** Name of the field that changed (e.g. "status"), null for full-order events. */
  @Column(name = "field_changed")
  private String fieldChanged;

  @Column(name = "old_value")
  private String oldValue;

  @Column(name = "new_value")
  private String newValue;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "actor_username", nullable = false)
  private String actorUsername;

  @Column(name = "actor_full_name")
  private String actorFullName;

  @Column(name = "activity_at", nullable = false)
  private LocalDateTime activityAt;

  /** Stored as a plain column – no join needed for tenant-scoped queries. */
  @Column(name = "company_id", nullable = false)
  private Long companyId;
}

