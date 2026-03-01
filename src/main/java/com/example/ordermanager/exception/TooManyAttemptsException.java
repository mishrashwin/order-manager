package com.example.ordermanager.exception;

public class TooManyAttemptsException extends RuntimeException {
  public TooManyAttemptsException(String message) {
    super(message);
  }
}
