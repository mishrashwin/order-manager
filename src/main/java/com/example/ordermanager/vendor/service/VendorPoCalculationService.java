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
  private static final BigDecimal MAX_NUMERIC_5_2 = new BigDecimal("999.99");
  private static final BigDecimal MAX_NUMERIC_12_2 = new BigDecimal("9999999999.99");
  private static final BigDecimal MAX_NUMERIC_12_3 = new BigDecimal("999999999.999");
  private static final BigDecimal MAX_NUMERIC_14_2 = new BigDecimal("999999999999.99");
  private static final BigDecimal MAX_NUMERIC_14_4 = new BigDecimal("9999999999.9999");
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
      int itemNumber = 0;
      for (VendorPoItem item : po.getItems()) {
        itemNumber++;
        BigDecimal quantity = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        requireFitsNumeric12Scale3(quantity, "Item " + itemNumber + " quantity");
        if (quantity.signum() <= 0) {
          throw new IllegalArgumentException("Item quantity must be greater than zero");
        }
        BigDecimal pricingUnit =
            item.getPricingUnit() == null ? BigDecimal.ONE : item.getPricingUnit();
        requireFitsNumeric12Scale3(pricingUnit, "Item " + itemNumber + " pricing unit");
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
        requireFitsNumeric12Scale2(rate, "Item " + itemNumber + " price");
        BigDecimal lineTaxable = rate.multiply(quantity).divide(pricingUnit, 2, ROUNDING);
        BigDecimal taxableRate = rate.divide(pricingUnit, 4, ROUNDING);
        requireFitsNumeric14Scale4(taxableRate, "Item " + itemNumber + " taxable unit rate");
        item.setTaxableRate(taxableRate);
        BigDecimal gstRate =
            item.getGstPercentage() == null ? BigDecimal.ZERO : item.getGstPercentage();
        if (gstRate.signum() < 0) {
          throw new IllegalArgumentException("GST rate cannot be negative");
        }
        requireFitsNumeric5Scale2(gstRate, "Item " + itemNumber + " GST rate");
        BigDecimal lineTax = lineTaxable.multiply(gstRate).divide(ONE_HUNDRED, 2, ROUNDING);
        BigDecimal lineTotal = lineTaxable.add(lineTax).setScale(2, ROUNDING);
        requireFitsNumeric12Scale2(lineTax, "Item " + itemNumber + " calculated GST amount");
        requireFitsNumeric14Scale2(lineTaxable, "Item " + itemNumber + " taxable amount");
        requireFitsNumeric14Scale2(lineTotal, "Item " + itemNumber + " line total");
        item.setTaxableAmount(lineTaxable);
        item.setGstAmount(lineTax);
        item.setLineTotal(lineTotal);
        BigDecimal gstInclusiveUnitPrice =
            rate.multiply(BigDecimal.ONE.add(gstRate.divide(ONE_HUNDRED, 6, ROUNDING))).setScale(4,
                ROUNDING);
        requireFitsNumeric12Scale2(gstInclusiveUnitPrice,
            "Item " + itemNumber + " GST-inclusive unit price");
        item.setGstInclusiveUnitPrice(gstInclusiveUnitPrice);
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
    requireFitsNumeric14Scale2(po.getSubtotalBeforeTax(), "PO subtotal");
    requireFitsNumeric14Scale2(po.getTotalTax(), "PO tax total");
    requireFitsNumeric14Scale2(po.getTotalAmount(), "PO total");
  }

  /** Rejects values that would exceed the database NUMERIC(12,2) capacity before persistence. */
  private void requireFitsNumeric12Scale2(BigDecimal value, String label) {
    if (value.setScale(2, ROUNDING).abs().compareTo(MAX_NUMERIC_12_2) > 0) {
      throw new IllegalArgumentException(label
          + " exceeds the supported limit of 9,999,999,999.99. Reduce the value and try again.");
    }
  }

  /** Rejects rates that would exceed the database NUMERIC(5,2) capacity before persistence. */
  private void requireFitsNumeric5Scale2(BigDecimal value, String label) {
    if (value.setScale(2, ROUNDING).abs().compareTo(MAX_NUMERIC_5_2) > 0) {
      throw new IllegalArgumentException(label + " exceeds the supported limit of 999.99.");
    }
  }

  /** Rejects quantity values that would exceed the database NUMERIC(12,3) capacity. */
  private void requireFitsNumeric12Scale3(BigDecimal value, String label) {
    if (value.setScale(3, ROUNDING).abs().compareTo(MAX_NUMERIC_12_3) > 0) {
      throw new IllegalArgumentException(
          label + " exceeds the supported limit of 999,999,999.999.");
    }
  }

  /** Rejects totals that would exceed the database NUMERIC(14,2) capacity before persistence. */
  private void requireFitsNumeric14Scale2(BigDecimal value, String label) {
    if (value.setScale(2, ROUNDING).abs().compareTo(MAX_NUMERIC_14_2) > 0) {
      throw new IllegalArgumentException(label
          + " exceeds the supported limit of 999,999,999,999.99. Reduce the value and try again.");
    }
  }

  /** Rejects rates that would exceed the database NUMERIC(14,4) capacity. */
  private void requireFitsNumeric14Scale4(BigDecimal value, String label) {
    if (value.setScale(4, ROUNDING).abs().compareTo(MAX_NUMERIC_14_4) > 0) {
      throw new IllegalArgumentException(
          label + " exceeds the supported limit of 9,999,999,999.9999.");
    }
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
    if (gstRate != null) {
      requireFitsNumeric5Scale2(gstRate, label + " GST rate");
    }
    if (amount != null && amount.signum() < 0) {
      throw new IllegalArgumentException(label + " amount cannot be negative");
    }
    BigDecimal base = amount == null ? BigDecimal.ZERO.setScale(2) : amount.setScale(2, ROUNDING);
    requireFitsNumeric14Scale2(base, label + " amount");
    if (included) {
      return BigDecimal.ZERO.setScale(2);
    }
    return base;
  }

  /** Calculates tax only for excluded charges explicitly marked taxable. */
  private BigDecimal chargeTax(BigDecimal base, Boolean taxable, BigDecimal rate) {
    if (taxable == null || !taxable) {
      return BigDecimal.ZERO.setScale(2);
    }
    return base.multiply(rate).divide(ONE_HUNDRED, 2, ROUNDING);
  }
}
