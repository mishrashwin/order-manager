package com.example.ordermanager.dto;

public record OwnerDashboardMetrics(long totalCompanies, long approvedCompanies, long pendingCompanies,
    long rejectedCompanies, long activeCompanies, long suspendedCompanies) {
}

