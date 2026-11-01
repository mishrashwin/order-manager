package com.example.ordermanager.product.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.repository.ProductRepository;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final CompanyService companyService;
  private final VendorRepository vendorRepository;
  private final Helper helper;

  public ProductService(ProductRepository productRepository, CompanyService companyService,
      VendorRepository vendorRepository, Helper helper) {
    this.productRepository = productRepository;
    this.companyService = companyService;
    this.vendorRepository = vendorRepository;
    this.helper = helper;
  }

  public List<Product> getProductsByCompanyId(Long companyId) {
    return productRepository.findByCompanyId(companyId);
  }

  public Page<Product> getProductsByCompanyId(Long companyId, Pageable pageable) {
    return productRepository.findByCompanyId(companyId, pageable);
  }

  public Page<Product> searchProducts(Long companyId, String search, Pageable pageable) {
    if (search == null || search.trim().isEmpty()) {
      return productRepository.findByCompanyId(companyId, pageable);
    }
    return productRepository.findByCompanyIdAndNameContainingIgnoreCase(companyId, search,
        pageable);
  }

  public Product getProductById(Long id) {
    return productRepository.findById(id).orElse(null);
  }

  public Product saveProductWithCompany(Product product, Long companyId) {
    Company company =
        companyService.getCompanyById(companyId).orElseThrow(() -> new IllegalArgumentException(
            "Company not found. Cannot create product without a company."));

    product.setCompany(company);

    if (product.getName() != null) {
      product.setName(helper.toTitleCase(product.getName()));
    }
    if (product.getBrand() != null) {
      product.setBrand(helper.toTitleCase(product.getBrand()));
    }
    if (product.getCategory() != null) {
      product.setCategory(helper.toTitleCase(product.getCategory()));
    }

    // Resolve vendor entity by id if only id is present on the bound object
    if (product.getVendor() != null && product.getVendor().getId() != null) {
      product.setVendor(vendorRepository.findById(product.getVendor().getId()).orElse(null));
    }

    // Calculate GST-inclusive price from pre-GST price if pre-GST price is set
    if (product.getPreGstPrice() != null) {
      product.calculateGstInclusivePrice();
    } else if (product.getPrice() != null && product.getGstPercentage() != null) {
      // For backward compatibility: if only legacy price is set, calculate pre-GST price
      java.math.BigDecimal gstMultiplier = product.getGstPercentage()
          .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
      java.math.BigDecimal preGstPrice = java.math.BigDecimal.valueOf(product.getPrice())
          .divide(java.math.BigDecimal.ONE.add(gstMultiplier), 2, java.math.RoundingMode.HALF_UP);
      product.setPreGstPrice(preGstPrice);
    }

    return productRepository.save(product);
  }

  public void deleteProduct(Long id) {
    productRepository.deleteById(id);
  }
}
