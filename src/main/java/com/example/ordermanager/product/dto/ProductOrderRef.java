package com.example.ordermanager.product.dto;

public class ProductOrderRef {
  private final Long orderId;
  private final String poOrderNo;

  public ProductOrderRef(Long orderId, String poOrderNo) {
    this.orderId = orderId;
    this.poOrderNo = poOrderNo;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getPoOrderNo() {
    return poOrderNo;
  }
}
