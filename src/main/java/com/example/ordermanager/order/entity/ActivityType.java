package com.example.ordermanager.order.entity;

/** Types of changes that are tracked in the order activity audit log. */
public enum ActivityType {

  CREATED("Order Created"), STATUS_CHANGED("Status Changed"), UPDATED("Order Updated"), DELETED(
      "Order Deleted");

  private final String displayName;

  ActivityType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}

