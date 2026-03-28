package com.example.ordermanager.owner.dto;

import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import java.time.LocalDateTime;

public record OwnerCompanySummary(Long companyId, String companyName, CompanyApprovalStatus approvalStatus,
    boolean active, long adminCount, long userCount, LocalDateTime createdAt,
    String adminFirstName, String adminLastName, String adminEmail, String adminMobileNumber,
    Double monthlyFee) {
}

