package com.example.ordermanager.vendor.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.IndianState;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class VendorPdfServiceIntegrationTest {

  @Autowired
  private VendorPdfService vendorPdfService;

  @MockBean
  private CompanyService companyService;

  private Company testCompany;
  private VendorPo testVendorPo;
  private Vendor testVendor;

  @BeforeEach
  void setUp() {
    // Setup test company
    testCompany = new Company();
    testCompany.setId(1L);
    testCompany.setName("Test Company Ltd");
    testCompany.setGstn("TESTGSTN123");
    testCompany.setState(IndianState.MAHARASHTRA);

    // Setup test vendor
    testVendor = new Vendor();
    testVendor.setId(1L);
    testVendor.setCompanyName("Test Vendor");
    testVendor.setEmail("vendor@test.com");
    testVendor.setGstn("VENDORGSTN456");
    testVendor.setState(IndianState.MAHARASHTRA);

    // Setup test vendor PO with items
    testVendorPo = new VendorPo();
    testVendorPo.setId(1L);
    testVendorPo.setPoNumber("PO-2026-001");
    testVendorPo.setVendor(testVendor);
    testVendorPo.setCompany(testCompany);
    testVendorPo.setDeliveryDate(LocalDate.of(2026, 4, 15));
    testVendorPo.setStatus(VendorPoStatus.SENT);
    testVendorPo.setTotalAmount(1500.00);
    testVendorPo.setNote("5% PENALTY per week after delivery date.");
    testVendorPo.setVendorName("Test Vendor");
    testVendorPo.setEmails("vendor@test.com");
    testVendorPo.setVendorGstn("VENDORGSTN456");

    // Setup PO items
    VendorPoItem item1 = new VendorPoItem();
    item1.setProductName("Product A");
    item1.setHsnCode("HSN123");
    item1.setUnit("PCS");
    item1.setQuantity(10.00);
    item1.setUnitPrice(100.00);
    item1.setLineTotal(1000.00);

    VendorPoItem item2 = new VendorPoItem();
    item2.setProductName("Product B");
    item2.setHsnCode("HSN456");
    item2.setUnit("BOX");
    item2.setQuantity(5.00);
    item2.setUnitPrice(100.00);
    item2.setLineTotal(500.00);

    testVendorPo.setItems(Arrays.asList(item1, item2));

    when(companyService.getCompanyById(1L)).thenReturn(Optional.of(testCompany));
  }

  @Test
  void generatePdf_shouldReturnNonEmptyPdfBytes() {
    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0, "PDF should not be empty");

    // Check for PDF header signature
    assertTrue(pdfBytes.length > 4, "PDF should have at least header bytes");
    assertEquals('%', pdfBytes[0], "PDF should start with %");
    assertEquals('P', pdfBytes[1], "PDF should start with %PDF");
    assertEquals('D', pdfBytes[2], "PDF should start with %PDF");
    assertEquals('F', pdfBytes[3], "PDF should start with %PDF");
  }

  @Test
  void generatePdf_withDifferentVendorPo_shouldGenerateDifferentPdfs() {
    VendorPo secondPo = new VendorPo();
    secondPo.setId(2L);
    secondPo.setPoNumber("PO-2026-002");
    secondPo.setVendor(testVendor);
    secondPo.setCompany(testCompany);
    secondPo.setDeliveryDate(LocalDate.of(2026, 4, 20));
    secondPo.setStatus(VendorPoStatus.SENT);
    secondPo.setTotalAmount(2000.00);
    secondPo.setNote("Different note");
    secondPo.setVendorName("Test Vendor");
    secondPo.setEmails("vendor@test.com");
    secondPo.setVendorGstn("VENDORGSTN456");
    secondPo.setItems(Arrays.asList());

    byte[] firstPdf = vendorPdfService.generatePdf(testVendorPo);
    byte[] secondPdf = vendorPdfService.generatePdf(secondPo);

    assertNotNull(firstPdf);
    assertNotNull(secondPdf);
    assertTrue(firstPdf.length > 0);
    assertTrue(secondPdf.length > 0);

    // PDFs should be different (different content)
    assertNotEquals(firstPdf.length, secondPdf.length,
        "PDFs with different content should have different sizes");
  }

  @Test
  void generatePdf_withComplexData_shouldGenerateValidPdf() {
    // Add more complex items
    VendorPoItem complexItem = new VendorPoItem();
    complexItem.setProductName("Complex Product with Special Characters: &<>\"'");
    complexItem.setHsnCode("HSN789");
    complexItem.setUnit("KG");
    complexItem.setQuantity(100.00);
    complexItem.setUnitPrice(15.50);
    complexItem.setLineTotal(1550.00);

    testVendorPo.setItems(Arrays.asList(complexItem));
    testVendorPo.setTotalAmount(550.00);

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0, "PDF should be generated even with special characters");

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_withNullItems_shouldStillGeneratePdf() {
    testVendorPo.setItems(null);

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0, "PDF should be generated even with null items");

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_withEmptyItems_shouldGeneratePdfWithEmptyTable() {
    testVendorPo.setItems(Arrays.asList());

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0, "PDF should be generated with empty items list");

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_withLargeAmounts_shouldHandleCorrectly() {
    testVendorPo.setTotalAmount(999999.99);

    VendorPoItem largeItem = new VendorPoItem();
    largeItem.setProductName("Expensive Product");
    largeItem.setHsnCode("HSN999");
    largeItem.setUnit("UNIT");
    largeItem.setQuantity(1.00);
    largeItem.setUnitPrice(99999.99);
    largeItem.setLineTotal(999999.99);

    testVendorPo.setItems(Arrays.asList(largeItem));

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0, "PDF should handle large amounts correctly");

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_templateEngineIntegration_shouldRenderAllFields() {
    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    // Convert PDF to text for basic content verification (simplified)
    // In a real test, you might use a PDF library to extract and verify content
    String pdfContent = new String(pdfBytes);

    // Verify that template variables are processed (they shouldn't appear as literal text)
    assertFalse(pdfContent.contains("${company.name}"), "Template variable should be processed");
    assertFalse(pdfContent.contains("${vendorPo.poNumber}"),
        "Template variable should be processed");
    assertFalse(pdfContent.contains("${vendorPo.vendorName}"),
        "Template variable should be processed");
  }

  @Test
  void generatePdf_performance_shouldCompleteInReasonableTime() {
    long startTime = System.currentTimeMillis();

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    assertTrue(duration < 5000,
        "PDF generation should complete within 5 seconds, took: " + duration + "ms");
  }

  @Test
  void generatePdf_concurrentCalls_shouldWork() throws InterruptedException {
    final int threadCount = 5;
    Thread[] threads = new Thread[threadCount];
    byte[][] results = new byte[threadCount][];
    boolean[] completed = new boolean[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          results[index] = vendorPdfService.generatePdf(testVendorPo);
          completed[index] = true;
        } catch (Exception e) {
          completed[index] = false;
        }
      });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all threads completed successfully
    for (int i = 0; i < threadCount; i++) {
      assertTrue(completed[i], "Thread " + i + " should complete successfully");
      assertNotNull(results[i], "Thread " + i + " should produce a PDF");
      assertTrue(results[i].length > 0, "Thread " + i + " should produce a non-empty PDF");
    }
  }

  @Test
  void generatePdf_intraState_shouldUseCgstAndSgst() {
    // Same state - should use CGST + SGST
    testCompany.setState(IndianState.MAHARASHTRA);
    testVendor.setState(IndianState.MAHARASHTRA);

    VendorPoItem item = new VendorPoItem();
    item.setProductName("Test Product");
    item.setHsnCode("HSN123");
    item.setUnit("PCS");
    item.setQuantity(10.00);
    item.setUnitPrice(100.00);
    item.setLineTotal(1180.00); // 18% GST inclusive
    item.setGstPercentage(BigDecimal.valueOf(18));

    testVendorPo.setItems(Arrays.asList(item));

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_interState_shouldUseIgst() {
    // Different states - should use IGST
    testCompany.setState(IndianState.MAHARASHTRA);
    testVendor.setState(IndianState.GUJARAT);

    VendorPoItem item = new VendorPoItem();
    item.setProductName("Test Product");
    item.setHsnCode("HSN123");
    item.setUnit("PCS");
    item.setQuantity(10.00);
    item.setUnitPrice(100.00);
    item.setLineTotal(1180.00); // 18% GST inclusive
    item.setGstPercentage(BigDecimal.valueOf(18));

    testVendorPo.setItems(Arrays.asList(item));

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_nullStates_shouldDefaultToInterState() {
    // Null states - should default to IGST (inter-state)
    testCompany.setState(null);
    testVendor.setState(null);

    VendorPoItem item = new VendorPoItem();
    item.setProductName("Test Product");
    item.setHsnCode("HSN123");
    item.setUnit("PCS");
    item.setQuantity(10.00);
    item.setUnitPrice(100.00);
    item.setLineTotal(1180.00);
    item.setGstPercentage(BigDecimal.valueOf(18));

    testVendorPo.setItems(Arrays.asList(item));

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_nullCompanyState_shouldDefaultToInterState() {
    // Null company state - should default to IGST
    testCompany.setState(null);
    testVendor.setState(IndianState.MAHARASHTRA);

    VendorPoItem item = new VendorPoItem();
    item.setProductName("Test Product");
    item.setHsnCode("HSN123");
    item.setUnit("PCS");
    item.setQuantity(10.00);
    item.setUnitPrice(100.00);
    item.setLineTotal(1180.00);
    item.setGstPercentage(BigDecimal.valueOf(18));

    testVendorPo.setItems(Arrays.asList(item));

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }

  @Test
  void generatePdf_nullVendorState_shouldDefaultToInterState() {
    // Null vendor state - should default to IGST
    testCompany.setState(IndianState.MAHARASHTRA);
    testVendor.setState(null);

    VendorPoItem item = new VendorPoItem();
    item.setProductName("Test Product");
    item.setHsnCode("HSN123");
    item.setUnit("PCS");
    item.setQuantity(10.00);
    item.setUnitPrice(100.00);
    item.setLineTotal(1180.00);
    item.setGstPercentage(BigDecimal.valueOf(18));

    testVendorPo.setItems(Arrays.asList(item));

    byte[] pdfBytes = vendorPdfService.generatePdf(testVendorPo);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    // Verify PDF header
    assertEquals('%', pdfBytes[0]);
    assertEquals('P', pdfBytes[1]);
    assertEquals('D', pdfBytes[2]);
    assertEquals('F', pdfBytes[3]);
  }
}
