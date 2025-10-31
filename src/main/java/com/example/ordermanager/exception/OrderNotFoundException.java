package com.example.ordermanager.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("No order exists with ID " + id);
    }
}