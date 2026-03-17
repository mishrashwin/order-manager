package com.example.ordermanager.dto;

import com.example.ordermanager.entity.CompanyApprovalStatus;
import java.time.LocalDateTime;

public record OwnerCompanySummary(Long companyId, String companyName, CompanyApprovalStatus approvalStatus,
    boolean active, long usersCount, LocalDateTime createdAt, String approvedBy,
    LocalDateTime approvedAt) {
}

