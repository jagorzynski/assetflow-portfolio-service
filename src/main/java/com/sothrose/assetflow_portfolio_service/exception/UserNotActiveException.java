package com.sothrose.assetflow_portfolio_service.exception;

public class UserNotActiveException extends RuntimeException {
  public UserNotActiveException(String message) {
    super(message);
  }
}
