package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.VendorPo;

public interface VendorPdfService {
  byte[] generatePdf(VendorPo vendorPo);
}

