package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface VendorPoService {

  VendorPo createVendorPo(VendorPo po);

  VendorPo updateVendorPo(Long id, VendorPo po);

  VendorPo getVendorPoById(Long id);

  Page<VendorPo> listVendorPos(LocalDate startDate, LocalDate endDate, Pageable pageable);

  Page<VendorPo> searchVendorPos(LocalDate startDate, LocalDate endDate, String search, Pageable pageable);

  VendorPo changeStatus(Long id, VendorPoStatus status);

  void deleteVendorPo(Long id);

  byte[] generatePdf(Long id);

}

