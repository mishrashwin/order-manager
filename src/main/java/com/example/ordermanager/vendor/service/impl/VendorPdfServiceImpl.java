package com.example.ordermanager.vendor.service.impl;

import com.example.ordermanager.utils.NumberToWordsUtil;
import com.example.ordermanager.utils.IndianQuantityFormatter;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.service.VendorPdfService;
import com.example.ordermanager.utils.IndianMoneyFormatter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Renders Vendor POs exclusively from persisted buyer, vendor, and financial snapshots. */
@Service
@RequiredArgsConstructor
public class VendorPdfServiceImpl implements VendorPdfService {

  private final TemplateEngine templateEngine;
  private final ObjectMapper objectMapper;

  /**
   * Generates a PDF from immutable PO snapshot fields without consulting live Company or Vendor
   * data.
   */
  @Override
  public byte[] generatePdf(VendorPo po) {
    try {
      if (po.getItems() != null) {
        for (VendorPoItem item : po.getItems()) {
          item.setProductName(unescapeHtmlEntities(item.getProductName()));
          item.setFormattedQuantity(IndianQuantityFormatter.format(item.getQuantity()));
          BigDecimal rate =
              item.getTaxableRate() == null ? item.getUnitPrice() : item.getTaxableRate();
          item.setFormattedTaxableRate(IndianMoneyFormatter.format(rate));
          item.setFormattedGstAmount(IndianMoneyFormatter.format(item.getGstAmount()));
          item.setFormattedLineTotal(IndianMoneyFormatter.format(item.getLineTotal()));
        }
      }
      po.setNote(unescapeHtmlEntities(po.getNote()));
      po.setPaymentTerms(unescapeHtmlEntities(po.getPaymentTerms()));
      po.setDeliveryTerms(unescapeHtmlEntities(po.getDeliveryTerms()));
      po.setPenaltyClause(unescapeHtmlEntities(po.getPenaltyClause()));
      po.setBuyerCompanyName(unescapeHtmlEntities(po.getBuyerCompanyName()));
      po.setBuyerGstin(unescapeHtmlEntities(po.getBuyerGstin()));
      po.setVendorAddress(unescapeHtmlEntities(po.getVendorAddress()));
      po.setVendorGstn(unescapeHtmlEntities(po.getVendorGstn()));
      List<String> terms = po.getTermsConditionsSnapshot() == null ? List.of()
          : objectMapper.readValue(po.getTermsConditionsSnapshot(), new TypeReference<>() {});
      terms = terms.stream().map(this::unescapeHtmlEntities).toList();
      Context context = new Context();
      HashMap<String, Object> variables = new HashMap<>();
      variables.put("vendorPo", po);
      variables.put("itemColumnWidths", VendorPoPdfColumnWidths.calculate(po.getItems()));
      variables.put("buyerName", safe(po.getBuyerCompanyName()));
      variables.put("buyerGstin", safe(po.getBuyerGstin()));
      variables.put("buyerAddress", po.getBillToSnapshot());
      variables.put("shipToAddress", po.getShipToSnapshot());
      variables.put("vendorAddress", safe(po.getVendorAddress()));
      variables.put("vendorGstin", safe(po.getVendorGstn()));
      variables.put("amountInWords", NumberToWordsUtil.convertToWords(po.getTotalAmount()));
      variables.put("subtotal", IndianMoneyFormatter.format(po.getSubtotalBeforeTax()));
      variables.put("cgst", IndianMoneyFormatter.format(po.getCgstAmount()));
      variables.put("sgst", IndianMoneyFormatter.format(po.getSgstAmount()));
      variables.put("igst", IndianMoneyFormatter.format(po.getIgstAmount()));
      variables.put("finalTotal", IndianMoneyFormatter.format(po.getTotalAmount()));
      variables.put("freightIncluded", Boolean.TRUE.equals(po.getFreightIncluded()));
      variables.put("freightAmount", IndianMoneyFormatter.format(po.getFreightAmount()));
      variables.put("packingIncluded", Boolean.TRUE.equals(po.getPackingIncluded()));
      variables.put("packingAmount", IndianMoneyFormatter.format(po.getPackingAmount()));
      variables.put("termsConditions", terms);
      variables.put("termsConditionsEnabled", po.isTermsConditionsEnabled());
      variables.put("isIntraState",
          orZero(po.getCgstAmount()).signum() > 0 || orZero(po.getSgstAmount()).signum() > 0);
      variables.put("logoBase64", base64(po.getBuyerLogoData(), po.getBuyerLogoContentType()));
      variables.put("signatureBase64",
          base64(po.getBuyerSignatureData(), po.getBuyerSignatureContentType()));
      context.setVariables(variables);
      String html = templateEngine.process("vendor/pdf/vendor_po", context)
          .replaceAll("&(?!(?:#\\d+|#x[0-9A-Fa-f]+|amp|lt|gt|apos|quot);)", "&amp;");
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withHtmlContent(html, null);
      builder.toStream(output);
      try {
        builder.useFont(() -> getClass().getResourceAsStream("/fonts/segoeui.ttf"), "Segoe UI");
      } catch (Exception e) {
        // Fallback to default font if font not available
      }
      builder.run();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to generate Vendor PO PDF", exception);
    }
  }

  /** Returns a non-null display string for optional snapshot text fields. */
  private String safe(String value) {
    return value == null ? "" : value;
  }

  /**
   * Supplies a zero display value for legacy draft fields predating persisted decimal snapshots.
   */
  private BigDecimal orZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Encodes saved image bytes for inline rendering in the generated PDF. */
  private String base64(byte[] bytes, String contentType) {
    return bytes == null ? null
        : "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
  }

  /** Unescapes HTML entities to their character equivalents for PDF rendering. */
  private String unescapeHtmlEntities(String text) {
    if (text == null) return null;
    return text.replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&apos;", "'");
  }
}
