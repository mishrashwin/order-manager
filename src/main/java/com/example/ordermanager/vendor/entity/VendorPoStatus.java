package com.example.ordermanager.vendor.entity;

public enum VendorPoStatus {
  DRAFT, SENT, APPROVED, REJECTED, RECEIVED;

  public boolean isFinal() {
    return this == APPROVED || this == REJECTED || this == RECEIVED;
  }

  public boolean isActive() {
    return this == DRAFT || this == SENT;
  }

  public String getDisplayName() {
    switch (this) {
      case DRAFT:
        return "Draft — Not Sent";
      case SENT:
        return "Sent to Vendor";
      case APPROVED:
        return "Approved";
      case REJECTED:
        return "Rejected";
      case RECEIVED:
        return "Goods Received";
      default:
        return this.name();
    }
  }

  /** Short label used where space is tight (e.g. badge in list). */
  public String getShortName() {
    switch (this) {
      case DRAFT:
        return "Not Sent";
      case SENT:
        return "Sent";
      case APPROVED:
        return "Approved";
      case REJECTED:
        return "Rejected";
      case RECEIVED:
        return "Received";
      default:
        return this.name();
    }
  }

  /** Bootstrap icon class to show alongside the badge. */
  public String getIcon() {
    switch (this) {
      case DRAFT:
        return "bi-pencil-square";
      case SENT:
        return "bi-envelope-check";
      case APPROVED:
        return "bi-check-circle-fill";
      case REJECTED:
        return "bi-x-circle-fill";
      case RECEIVED:
        return "bi-box-seam";
      default:
        return "bi-circle";
    }
  }

  public String getBadgeColor() {
    switch (this) {
      case DRAFT:
        return "warning text-dark";
      case SENT:
        return "info";
      case APPROVED:
        return "success";
      case REJECTED:
        return "danger";
      case RECEIVED:
        return "primary";
      default:
        return "secondary";
    }
  }
}
