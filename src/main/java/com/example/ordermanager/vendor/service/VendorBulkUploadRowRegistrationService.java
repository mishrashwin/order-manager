package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.entity.VendorBulkUploadRequest;
import com.example.ordermanager.vendor.repository.VendorBulkUploadRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Commits each vendor with a durable upload/row correlation under a reconciliation fence. */
@Service
public class VendorBulkUploadRowRegistrationService {

  private final VendorBulkUploadRequestRepository requestRepository;
  private final VendorService vendorService;

  /** Wires the locked request repository and existing tenant-aware vendor registration service. */
  public VendorBulkUploadRowRegistrationService(VendorBulkUploadRequestRepository requestRepository,
      VendorService vendorService) {
    this.requestRepository = requestRepository;
    this.vendorService = vendorService;
  }

  /**
   * Atomically saves one vendor and its audit correlation while holding the upload request fence.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Vendor register(Vendor vendor, Long companyId, Long requestId, int excelRowNumber) {
    VendorBulkUploadRequest request =
        requestRepository.findOwnedRequestForUpdate(requestId, companyId)
            .orElseThrow(() -> new IllegalStateException("Registration request is unavailable."));
    if (request.getStatus() != VendorBulkUploadRequest.Status.IN_PROGRESS) {
      throw new IllegalStateException("Registration request was stopped for reconciliation.");
    }
    vendor.setBulkUploadRequestId(requestId);
    vendor.setBulkUploadRowNumber(excelRowNumber);
    vendor.setBulkUploadProcessedAt(java.time.LocalDateTime.now());
    Vendor saved = vendorService.saveVendorWithCompany(vendor, companyId);
    request.touchProgress();
    requestRepository.save(request);
    return saved;
  }
}
