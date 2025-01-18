package com.sothrose.assetflow_portfolio_service.exception;

public class PortfolioAlreadyPresentException extends RuntimeException {
  public PortfolioAlreadyPresentException(String message) {
    super(message);
  }
}
