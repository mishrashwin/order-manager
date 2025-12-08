package com.example.ordermanager.entity;

public enum OrderStatus {

    CREATED(false, "Created", "secondary"),
    VENDOR_PROCESSING(false, "Vendor Processing", "info"),
    OWN_PROCESSING(false, "Own Processing", "warning text-dark"),
    DISPATCHED(false, "Dispatched", "primary"),
    DELIVERED(true, "Delivered", "success"),
    PENDING_PAYMENT(true, "Pending Payment", "dark"),
    COMPLETED(true, "Completed", "success"),
    RETURNED(true, "Returned", "secondary text-dark"),
    CANCELLED(true, "Cancelled", "danger");

    private final boolean isFinal;
    private final String displayName;
    private final String badgeColor;

    OrderStatus(boolean isFinal, String displayName, String badgeColor) {
        this.isFinal = isFinal;
        this.displayName = displayName;
        this.badgeColor = badgeColor;
    }

    /** ✅ Whether this status is terminal (order process finished or stopped) */
    public boolean isFinal() {
        return isFinal;
    }

    /** ✅ Whether this status represents an active order */
    public boolean isActive() {
        return !isFinal;
    }

    /** ✅ Friendly display name for UI */
    public String getDisplayName() {
        return displayName;
    }

    /** ✅ Bootstrap badge color (used in list and dashboard) */
    public String getBadgeColor() {
        return badgeColor;
    }
}
