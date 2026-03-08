package com.example.ordermanager.user.service;

public class BrevoEmailException extends RuntimeException {

  public BrevoEmailException(String message) {
    super(message);
  }

  public BrevoEmailException(String message, Throwable cause) {
    super(message, cause);
  }
}

