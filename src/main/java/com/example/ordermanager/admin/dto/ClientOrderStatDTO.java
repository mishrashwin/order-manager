package com.example.ordermanager.admin.dto;

/**
 * DTO representing aggregated order statistics for a single client. Used for the Admin Order
 * Statistics pie chart and drill-down table.
 */
public class ClientOrderStatDTO {

  private final Long clientId;
  private final String clientName;
  private final long orderCount;
  private final double totalAmount;
  private final double percentage;

  public ClientOrderStatDTO(Long clientId, String clientName, long orderCount, double totalAmount,
      double percentage) {
    this.clientId = clientId;
    this.clientName = clientName;
    this.orderCount = orderCount;
    this.totalAmount = totalAmount;
    this.percentage = percentage;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientName() {
    return clientName;
  }

  public long getOrderCount() {
    return orderCount;
  }

  public double getTotalAmount() {
    return totalAmount;
  }

  public double getPercentage() {
    return percentage;
  }
}
