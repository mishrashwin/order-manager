package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface VendorPoService {

  VendorPo createVendorPo(VendorPo po);

  VendorPo updateVendorPo(Long id, VendorPo po);

  VendorPo getVendorPoById(Long id);

  Page<VendorPo> listVendorPos(LocalDate startDate, LocalDate endDate, Pageable pageable);

  Page<VendorPo> searchVendorPos(LocalDate startDate, LocalDate endDate, String search,
      Pageable pageable);

  VendorPo changeStatus(Long id, VendorPoStatus status);

  void deleteVendorPo(Long id);

  byte[] generatePdf(Long id);

  /** Creates the next editable revision from an immutable tenant-owned PO row. */
  VendorPo createRevision(Long id);

  /** Lists revision history for a PO after confirming Company ownership. */
  List<VendorPo> getRevisionHistory(Long id);

}

