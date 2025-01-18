package com.sothrose.assetflow_portfolio_service.exception;

public class NotEnoughAssetQuantityException extends RuntimeException {
  public NotEnoughAssetQuantityException(String message) {
    super(message);
  }
}
