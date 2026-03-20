package com.example.ordermanager.owner.dto;

public record OwnerDashboardMetrics(long totalCompanies, long approvedCompanies, long pendingCompanies,
    long rejectedCompanies, long activeCompanies, long suspendedCompanies) {
}

