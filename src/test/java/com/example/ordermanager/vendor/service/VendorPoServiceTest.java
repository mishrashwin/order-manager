package com.example.ordermanager.vendor.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.repository.ProductRepository;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import com.example.ordermanager.vendor.repository.VendorPoRepository;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.example.ordermanager.vendor.service.impl.VendorPoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorPoServiceTest {

  @Mock
  VendorPoRepository vendorPoRepository;
  @Mock
  VendorRepository vendorRepository;
  @Mock
  CompanyRepository companyRepository;
  @Mock
  ProductRepository productRepository;
  @Mock
  SecurityContextHelper securityContextHelper;
  @Mock
  com.example.ordermanager.utils.Helper helper;

  VendorPoServiceImpl service;
  private Company testCompany;
  private Vendor testVendor;
  private VendorPo testVendorPo;

  @BeforeEach
  void setUp() {
    service = new VendorPoServiceImpl(vendorPoRepository, vendorRepository, companyRepository,
        productRepository, securityContextHelper, po -> new byte[0], helper);

    // Setup test data
    testCompany = new Company();
    testCompany.setId(1L);
    testCompany.setName("Test Company");

    testVendor = new Vendor();
    testVendor.setId(1L);
    testVendor.setCompanyName("Test Vendor");
    testVendor.setEmail("vendor@test.com");

    testVendorPo = new VendorPo();
    testVendorPo.setId(1L);
    testVendorPo.setPoNumber("PO-001");
    testVendorPo.setVendor(testVendor);
    testVendorPo.setCompany(testCompany);
    testVendorPo.setDeliveryDate(LocalDate.now().plusWeeks(2));
    testVendorPo.setStatus(VendorPoStatus.DRAFT);
    testVendorPo.setTotalAmount(1000.00);
  }

  @Test
  void createVendorPo_callsSave() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
    when(vendorPoRepository.countByCompany_Id(1L)).thenReturn(0L);
    when(helper.extractInitials("Test Company")).thenReturn("TC");
    when(vendorPoRepository.save(any(VendorPo.class))).thenAnswer(i -> i.getArgument(0));

    VendorPo result = service.createVendorPo(testVendorPo);

    verify(vendorPoRepository, times(1)).save(testVendorPo);
    assertEquals(testCompany, result.getCompany());
    assertEquals(VendorPoStatus.DRAFT, result.getStatus());
    assertEquals("TC001", result.getPoNumber());
  }

  @Test
  void createVendorPo_withItems_calculatesTotals() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
    when(vendorPoRepository.countByCompany_Id(1L)).thenReturn(0L);
    when(helper.extractInitials("Test Company")).thenReturn("TC");
    when(vendorPoRepository.save(any(VendorPo.class))).thenAnswer(i -> i.getArgument(0));

    // Setup items
    VendorPoItem item1 = new VendorPoItem();
    item1.setQuantity(2.00);
    item1.setUnitPrice(100.00);

    VendorPoItem item2 = new VendorPoItem();
    item2.setQuantity(3.00);
    item2.setUnitPrice(200.00);

    testVendorPo.setItems(Arrays.asList(item1, item2));

    VendorPo result = service.createVendorPo(testVendorPo);

    assertEquals(new BigDecimal("800.00"), result.getTotalAmount());
    verify(vendorPoRepository).save(testVendorPo);
  }

  @Test
  void getVendorPoById_returnsPo() {
    when(vendorPoRepository.findById(1L)).thenReturn(Optional.of(testVendorPo));

    VendorPo result = service.getVendorPoById(1L);

    assertEquals(testVendorPo, result);
    verify(vendorPoRepository).findById(1L);
  }

  @Test
  void getVendorPoById_throwsExceptionWhenNotFound() {
    when(vendorPoRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> service.getVendorPoById(1L));
  }

  @Test
  void listVendorPos_returnsPagedResults() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    Page<VendorPo> expectedPage = new PageImpl<>(Arrays.asList(testVendorPo));
    when(vendorPoRepository.findByCompany_Id(eq(1L), any(PageRequest.class))).thenReturn(expectedPage);

    Page<VendorPo> result = service.listVendorPos(null, null, PageRequest.of(0, 50));

    assertEquals(expectedPage, result);
    verify(vendorPoRepository).findByCompany_Id(eq(1L), any(PageRequest.class));
  }

  @Test
  void updateVendorPo_updatesExistingPo() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(vendorPoRepository.findById(1L)).thenReturn(Optional.of(testVendorPo));
    when(vendorPoRepository.save(any(VendorPo.class))).thenAnswer(i -> i.getArgument(0));

    testVendorPo.setNote("Updated note");
    VendorPo result = service.updateVendorPo(1L, testVendorPo);

    assertEquals("Updated note", result.getNote());
    assertEquals("PO-001", result.getPoNumber()); // PO number should remain unchanged
    verify(vendorPoRepository).save(testVendorPo);
  }

  @Test
  void deleteVendorPo_deletesPo() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(vendorPoRepository.findById(1L)).thenReturn(Optional.of(testVendorPo));

    service.deleteVendorPo(1L);

    verify(vendorPoRepository).delete(testVendorPo);
  }

  @Test
  void changeStatus_updatesStatus() {
    when(vendorPoRepository.findById(1L)).thenReturn(Optional.of(testVendorPo));
    when(vendorPoRepository.save(any(VendorPo.class))).thenAnswer(i -> i.getArgument(0));

    VendorPo result = service.changeStatus(1L, VendorPoStatus.SENT);

    assertEquals(VendorPoStatus.SENT, result.getStatus());
    verify(vendorPoRepository).save(testVendorPo);
  }

  @Test
  void generatePdf_returnsPdfBytes() {
    when(vendorPoRepository.findById(1L)).thenReturn(Optional.of(testVendorPo));

    byte[] result = service.generatePdf(1L);

    assertNotNull(result);
    assertEquals(0, result.length); // Placeholder implementation returns empty bytes
    verify(vendorPoRepository).findById(1L);
  }

  @Test
  void createVendorPo_throwsExceptionForInvalidCompany() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(companyRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> service.createVendorPo(testVendorPo));
  }

  @Test
  void createVendorPo_withVendor_setsVendorDetails() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendor));
    when(vendorPoRepository.countByCompany_Id(1L)).thenReturn(0L);
    when(helper.extractInitials("Test Company")).thenReturn("TC");
    when(vendorPoRepository.save(any(VendorPo.class))).thenAnswer(i -> i.getArgument(0));

    testVendorPo.setVendor(testVendor);
    VendorPo result = service.createVendorPo(testVendorPo);

    assertEquals("Test Vendor", result.getVendorName());
    assertEquals("vendor@test.com", result.getEmails());
    assertEquals(testVendor.getGstn(), result.getVendorGstn());
  }

  @Test
  void createVendorPo_withItems_setsItemDetails() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(1L);
    when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
    when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
    when(vendorPoRepository.countByCompany_Id(1L)).thenReturn(0L);
    when(helper.extractInitials("Test Company")).thenReturn("TC");
    when(vendorPoRepository.save(any(VendorPo.class))).thenAnswer(i -> i.getArgument(0));

    Product product = new Product();
    product.setId(1L);
    product.setName("Test Product");
    product.setHsnCode("HSN123");
    product.setUnit("PCS");

    VendorPoItem item = new VendorPoItem();
    item.setProduct(product);
    item.setQuantity(5.00);
    item.setUnitPrice(100.00);

    testVendorPo.setItems(Arrays.asList(item));
    VendorPo result = service.createVendorPo(testVendorPo);

    assertEquals("Test Product", result.getItems().get(0).getProductName());
    assertEquals("HSN123", result.getItems().get(0).getHsnCode());
    assertEquals("PCS", result.getItems().get(0).getUnit());
  }
}

