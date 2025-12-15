package com.example.ordermanager.exception;

public class EmailAlreadySentException extends RuntimeException {
  public EmailAlreadySentException(String message) {
    super(message);
  }
}
