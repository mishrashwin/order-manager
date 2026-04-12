package com.example.ordermanager.vendor.service.impl;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.NumberToWordsUtil;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.service.VendorPdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorPdfServiceImpl implements VendorPdfService {

  private static final Logger log = LoggerFactory.getLogger(VendorPdfServiceImpl.class);
  private final TemplateEngine templateEngine;
  private final CompanyService companyService;

  @Override
  @Transactional(readOnly = true)
  public byte[] generatePdf(VendorPo vendorPo) {
    log.info("Generating PDF for VendorPo id={}", vendorPo.getId());

    try {
      // Get company information
      Company company = companyService.getCompanyById(vendorPo.getCompany().getId())
          .orElseThrow(() -> new RuntimeException("Company not found"));

      // Calculate GST breakdown by back-calculating taxable value from GST-inclusive lineTotal
      java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
      java.math.BigDecimal totalGst = java.math.BigDecimal.ZERO;

      for (VendorPoItem item : vendorPo.getItems()) {
        java.math.BigDecimal lineTotal = java.math.BigDecimal.valueOf(item.getLineTotal());

        java.math.BigDecimal gstPercent = item.getGstPercentage() != null
            ? item.getGstPercentage()
            : java.math.BigDecimal.ZERO;

        java.math.BigDecimal divisor = java.math.BigDecimal.ONE.add(
            gstPercent.divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
        );

        // BACK CALCULATION: extract taxable value from GST-inclusive price
        java.math.BigDecimal taxable = lineTotal.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal gstAmount = lineTotal.subtract(taxable);

        // store for PDF usage
        item.setGstAmount(gstAmount);

        // calculate taxable rate per unit for PDF display
        BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
        if (qty.compareTo(BigDecimal.ZERO) > 0) {
          BigDecimal taxableRate = taxable.divide(qty, 2, java.math.RoundingMode.HALF_UP);
          item.setTaxableRate(taxableRate);
        }

        subtotal = subtotal.add(taxable);
        totalGst = totalGst.add(gstAmount);
      }

      // Determine if intra-state (same state) or inter-state (different state)
      boolean isIntraState = false;
      if (vendorPo.getCompany() != null && vendorPo.getVendor() != null) {
        com.example.ordermanager.utils.IndianState companyState = vendorPo.getCompany().getState();
        com.example.ordermanager.utils.IndianState vendorState = vendorPo.getVendor().getState();
        if (companyState != null && vendorState != null) {
          isIntraState = companyState.equals(vendorState);
        }
      }

      // Split tax based on state comparison
      BigDecimal cgst = BigDecimal.ZERO;
      BigDecimal sgst = BigDecimal.ZERO;
      BigDecimal igst = BigDecimal.ZERO;

      if (isIntraState) {
        cgst = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        sgst = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
      } else {
        igst = totalGst;
      }

      BigDecimal finalTotal = subtotal.add(totalGst);

      // Format numbers with comma separators for cleaner display
      DecimalFormat df = new DecimalFormat("#,##0.00");

      // Format item-level values
      for (VendorPoItem item : vendorPo.getItems()) {
        if (item.getTaxableRate() != null) {
          item.setFormattedTaxableRate(df.format(item.getTaxableRate()));
        }
        if (item.getGstAmount() != null) {
          item.setFormattedGstAmount(df.format(item.getGstAmount()));
        }
        item.setFormattedLineTotal(df.format(item.getLineTotal()));
      }

      String formattedSubtotal = df.format(subtotal);
      String formattedCgst = df.format(cgst);
      String formattedSgst = df.format(sgst);
      String formattedIgst = df.format(igst);
      String formattedFinalTotal = df.format(finalTotal);

      String amountInWords = NumberToWordsUtil.convertToWords(finalTotal.doubleValue());
      // Prepare Thymeleaf context with base64 images
      Context context = new Context();
      Map<String, Object> variables = new java.util.HashMap<>();
      variables.put("company", company);
      variables.put("vendorPo", vendorPo);
      variables.put("amountInWords", amountInWords);
      variables.put("subtotal", formattedSubtotal);
      variables.put("cgst", formattedCgst);
      variables.put("sgst", formattedSgst);
      variables.put("igst", formattedIgst);
      variables.put("isIntraState", isIntraState);
      variables.put("finalTotal", formattedFinalTotal);
      variables.put("logoBase64", toBase64(company.getLogoData(), company.getLogoContentType()));
      variables.put("signatureBase64", toBase64(company.getSignatureData(), company.getSignatureContentType()));
      context.setVariables(variables);

      // Render HTML template
      String html = templateEngine.process("vendor/pdf/vendor_po", context);

      // Convert HTML to PDF
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withHtmlContent(html, null);
      builder.toStream(outputStream);
      builder.run();

      byte[] pdfBytes = outputStream.toByteArray();
      log.info("Successfully generated PDF for VendorPo id={}, size={} bytes", vendorPo.getId(),
          pdfBytes.length);

      return pdfBytes;

    } catch (IOException e) {
      log.error("Error generating PDF for VendorPo id={}: {}", vendorPo.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed to generate PDF", e);
    } catch (Exception e) {
      log.error("Unexpected error generating PDF for VendorPo id={}: {}", vendorPo.getId(),
          e.getMessage(), e);
      throw new RuntimeException("Failed to generate PDF", e);
    }
  }

  private String toBase64(byte[] data, String contentType) {
    if (data == null) {
      return null;
    }
    return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data);
  }
}

