package com.example.ordermanager.vendor.service;

/** Holds the seven vendor fields parsed from one populated worksheet row. */
public record VendorBulkUploadRowDto(int excelRowNumber, String companyName, String contactPerson,
    String email, String phone, String gstn, String state, String address) {}
