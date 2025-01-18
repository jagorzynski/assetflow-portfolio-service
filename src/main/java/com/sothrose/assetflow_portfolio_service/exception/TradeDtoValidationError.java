package com.sothrose.assetflow_portfolio_service.exception;

public class TradeDtoValidationError extends RuntimeException {
  public TradeDtoValidationError(String message) {
    super(message);
  }
}
