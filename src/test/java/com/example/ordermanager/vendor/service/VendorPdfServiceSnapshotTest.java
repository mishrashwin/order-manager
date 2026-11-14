package com.example.ordermanager.vendor.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoAddressSnapshot;
import com.example.ordermanager.vendor.service.impl.VendorPdfServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class VendorPdfServiceSnapshotTest {
  @Mock
  TemplateEngine templateEngine;
  @Mock
  ObjectMapper objectMapper;

  /** Ensures PDF rendering receives saved Buyer/address snapshots and persisted totals. */
  @Test
  void rendersFromPersistedSnapshotValues() throws Exception {
    when(objectMapper.readValue(eq("[]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(java.util.List.of());
    when(templateEngine.process(eq("vendor/pdf/vendor_po"), any(Context.class)))
        .thenReturn("<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>snapshot</body></html>");
    VendorPo po = new VendorPo();
    po.setBuyerCompanyName("Snapshot Buyer");
    po.setBuyerGstin("27ABCDE1234F1Z5");
    VendorPoAddressSnapshot address = new VendorPoAddressSnapshot();
    address.setAddressLine1("Frozen Road");
    address.setDrugLicenseNumber("DL-1");
    po.setBillToSnapshot(address);
    po.setShipToSnapshot(address);
    po.setTotalAmount(new BigDecimal("12.34"));
    po.setSubtotalBeforeTax(new BigDecimal("10.00"));
    po.setTotalTax(new BigDecimal("2.34"));
    po.setCgstAmount(new BigDecimal("1.17"));
    po.setSgstAmount(new BigDecimal("1.17"));
    po.setIgstAmount(BigDecimal.ZERO);
    po.setTermsConditionsSnapshot("[]");
    po.setItems(java.util.List.of());

    VendorPdfServiceImpl service = new VendorPdfServiceImpl(templateEngine, objectMapper);
    byte[] pdf = service.generatePdf(po);

    assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("vendor/pdf/vendor_po"), context.capture());
    assertTrue(context.getValue().getVariable("buyerName").equals("Snapshot Buyer"));
    assertTrue(context.getValue().getVariable("buyerAddress") == address);
    assertTrue(context.getValue().getVariable("finalTotal").equals("12.34"));
    assertTrue(context.getValue().getVariable("amountInWords")
        .equals("Rupees Twelve and Thirty Four Paisa Only"));
  }
}
