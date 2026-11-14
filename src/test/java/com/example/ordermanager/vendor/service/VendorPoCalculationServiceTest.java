package com.example.ordermanager.vendor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoAddressSnapshot;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VendorPoCalculationServiceTest {

  private final VendorPoCalculationService service = new VendorPoCalculationService();

  /** Verifies item tax, state tax split, excluded charges, and rounded decimal totals. */
  @Test
  void calculatesIntraStateAmountsAndExplicitExcludedFreight() {
    VendorPo po = basePo();
    po.setFreightIncluded(false);
    po.setFreightAmount(new BigDecimal("10.00"));
    po.setFreightTaxable(true);
    po.setFreightGstPercentage(new BigDecimal("10.00"));
    VendorPoItem item = new VendorPoItem();
    item.setQuantity(2d);
    item.setPricingUnit(BigDecimal.ONE);
    item.setPreGstUnitPrice(new BigDecimal("100.00"));
    item.setGstPercentage(new BigDecimal("18.00"));
    po.addItem(item);

    service.calculate(po);

    assertEquals(new BigDecimal("210.00"), po.getSubtotalBeforeTax());
    assertEquals(new BigDecimal("37.00"), po.getTotalTax());
    assertEquals(new BigDecimal("18.50"), po.getCgstAmount());
    assertEquals(new BigDecimal("18.50"), po.getSgstAmount());
    assertEquals(new BigDecimal("247.00"), po.getTotalAmount());
  }

  /** Rejects excluded charges whose explicit taxable/non-taxable selection is missing. */
  @Test
  void rejectsChargeWithoutExplicitTaxTreatment() {
    VendorPo po = basePo();
    po.setFreightIncluded(false);
    po.setFreightAmount(new BigDecimal("5"));
    po.setFreightTaxable(null);

    assertThrows(IllegalArgumentException.class, () -> service.calculate(po));
  }

  /** Rejects negative financial inputs before persisting PO totals. */
  @Test
  void rejectsNegativeChargeAmount() {
    VendorPo po = basePo();
    po.setFreightIncluded(false);
    po.setFreightAmount(new BigDecimal("-1.00"));

    assertThrows(IllegalArgumentException.class, () -> service.calculate(po));
  }

  /** Creates a base PO with explicit Included defaults and matching GST state codes. */
  private VendorPo basePo() {
    VendorPo po = new VendorPo();
    VendorPoAddressSnapshot bill = new VendorPoAddressSnapshot();
    bill.setStateCode("27");
    po.setBillToSnapshot(bill);
    po.setVendorStateCode("27");
    po.setFreightIncluded(true);
    po.setPackingIncluded(true);
    po.setFreightTaxable(false);
    po.setPackingTaxable(false);
    return po;
  }
}
