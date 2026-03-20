package com.example.ordermanager.client.exception;

/**
 * Exception thrown when attempting to delete a client that has active orders. This prevents
 * orphaned orders and maintains referential integrity.
 */
public class ClientHasActiveOrdersException extends RuntimeException {

  private final Long clientId;
  private final long orderCount;

  public ClientHasActiveOrdersException(Long clientId, long orderCount) {
    super(String.format(
        "Cannot delete client with ID %d. This client has %d active order(s). "
            + "Please delete or reassign the orders before deleting the client.",
        clientId, orderCount));
    this.clientId = clientId;
    this.orderCount = orderCount;
  }

  public Long getClientId() {
    return clientId;
  }

  public long getOrderCount() {
    return orderCount;
  }
}

