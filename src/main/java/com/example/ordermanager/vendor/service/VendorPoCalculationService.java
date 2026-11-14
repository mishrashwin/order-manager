package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** Calculates persisted PO line, charge, tax, and total snapshots using decimal arithmetic. */
@Service
public class VendorPoCalculationService {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  /** Recalculates every item and charge using the PO's selected Bill To and Vendor state codes. */
  public void calculate(VendorPo po) {
    BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
    BigDecimal tax = BigDecimal.ZERO.setScale(2);
    BigDecimal cgst = BigDecimal.ZERO.setScale(2);
    BigDecimal sgst = BigDecimal.ZERO.setScale(2);
    BigDecimal igst = BigDecimal.ZERO.setScale(2);
    boolean intraState =
        po.getBillToSnapshot() != null && po.getBillToSnapshot().getStateCode() != null
            && po.getBillToSnapshot().getStateCode().equals(po.getVendorStateCode());

    if (po.getItems() != null) {
      for (VendorPoItem item : po.getItems()) {
        BigDecimal quantity = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        if (quantity.signum() <= 0) {
          throw new IllegalArgumentException("Item quantity must be greater than zero");
        }
        BigDecimal pricingUnit =
            item.getPricingUnit() == null ? BigDecimal.ONE : item.getPricingUnit();
        if (pricingUnit.signum() <= 0) {
          throw new IllegalArgumentException("Pricing Unit must be greater than zero");
        }
        BigDecimal rate = item.getPreGstUnitPrice();
        if (rate == null) {
          rate = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        }
        if (rate.signum() < 0) {
          throw new IllegalArgumentException("Item price cannot be negative");
        }
        BigDecimal lineTaxable = rate.multiply(quantity).divide(pricingUnit, 2, ROUNDING);
        item.setTaxableRate(rate.divide(pricingUnit, 4, ROUNDING));
        BigDecimal gstRate =
            item.getGstPercentage() == null ? BigDecimal.ZERO : item.getGstPercentage();
        if (gstRate.signum() < 0) {
          throw new IllegalArgumentException("GST rate cannot be negative");
        }
        BigDecimal lineTax = lineTaxable.multiply(gstRate).divide(ONE_HUNDRED, 2, ROUNDING);
        BigDecimal lineTotal = lineTaxable.add(lineTax).setScale(2, ROUNDING);
        item.setTaxableAmount(lineTaxable);
        item.setGstAmount(lineTax);
        item.setLineTotal(lineTotal);
        item.setGstInclusiveUnitPrice(
            rate.multiply(BigDecimal.ONE.add(gstRate.divide(ONE_HUNDRED, 6, ROUNDING))).setScale(4,
                ROUNDING));
        item.setUnitPrice(item.getGstInclusiveUnitPrice());

        if (intraState) {
          BigDecimal lineCgst = lineTax.divide(new BigDecimal("2"), 2, ROUNDING);
          item.setCgstAmount(lineCgst);
          item.setSgstAmount(lineTax.subtract(lineCgst));
          item.setIgstAmount(BigDecimal.ZERO.setScale(2));
        } else {
          item.setCgstAmount(BigDecimal.ZERO.setScale(2));
          item.setSgstAmount(BigDecimal.ZERO.setScale(2));
          item.setIgstAmount(lineTax);
        }
        subtotal = subtotal.add(lineTaxable);
        tax = tax.add(lineTax);
        cgst = cgst.add(item.getCgstAmount());
        sgst = sgst.add(item.getSgstAmount());
        igst = igst.add(item.getIgstAmount());
      }
    }

    BigDecimal freightBase = chargeBase(po.getFreightIncluded(), po.getFreightAmount(),
        po.getFreightTaxable(), po.getFreightGstPercentage(), "Freight");
    BigDecimal packingBase = chargeBase(po.getPackingIncluded(), po.getPackingAmount(),
        po.getPackingTaxable(), po.getPackingGstPercentage(), "Packing");
    subtotal = subtotal.add(freightBase).add(packingBase);
    BigDecimal freightTax =
        chargeTax(freightBase, po.getFreightTaxable(), po.getFreightGstPercentage());
    BigDecimal packingTax =
        chargeTax(packingBase, po.getPackingTaxable(), po.getPackingGstPercentage());
    BigDecimal chargeTax = freightTax.add(packingTax);
    tax = tax.add(chargeTax);
    if (intraState) {
      BigDecimal chargeCgst = chargeTax.divide(new BigDecimal("2"), 2, ROUNDING);
      cgst = cgst.add(chargeCgst);
      sgst = sgst.add(chargeTax.subtract(chargeCgst));
    } else {
      igst = igst.add(chargeTax);
    }

    subtotal = subtotal.setScale(2, ROUNDING);
    tax = tax.setScale(2, ROUNDING);
    po.setSubtotalBeforeTax(subtotal);
    po.setTotalTax(tax);
    po.setCgstAmount(cgst.setScale(2, ROUNDING));
    po.setSgstAmount(sgst.setScale(2, ROUNDING));
    po.setIgstAmount(igst.setScale(2, ROUNDING));
    po.setTotalAmount(subtotal.add(tax).setScale(2, ROUNDING));
  }

  /** Returns excluded charge value after requiring its explicit taxable treatment. */
  private BigDecimal chargeBase(Boolean included, BigDecimal amount, Boolean taxable,
      BigDecimal gstRate, String label) {
    if (included == null) {
      throw new IllegalArgumentException(label + " must be marked Included or Excluded");
    }
    if (taxable == null) {
      throw new IllegalArgumentException(label + " GST treatment must be selected");
    }
    if (taxable && (gstRate == null || gstRate.signum() < 0)) {
      throw new IllegalArgumentException(label + " GST rate is required when taxable");
    }
    if (amount != null && amount.signum() < 0) {
      throw new IllegalArgumentException(label + " amount cannot be negative");
    }
    if (included) {
      return BigDecimal.ZERO.setScale(2);
    }
    return amount == null ? BigDecimal.ZERO.setScale(2) : amount.setScale(2, ROUNDING);
  }

  /** Calculates tax only for excluded charges explicitly marked taxable. */
  private BigDecimal chargeTax(BigDecimal base, Boolean taxable, BigDecimal rate) {
    if (taxable == null || !taxable) {
      return BigDecimal.ZERO.setScale(2);
    }
    return base.multiply(rate).divide(ONE_HUNDRED, 2, ROUNDING);
  }
}
