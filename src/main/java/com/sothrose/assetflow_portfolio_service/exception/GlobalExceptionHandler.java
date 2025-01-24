package com.sothrose.assetflow_portfolio_service.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
    UserDtoValidationException.class,
    PortfolioDtoValidationException.class,
    PortfolioAlreadyPresentException.class,
    NotEnoughAssetQuantityException.class,
    PortfolioNotPresentException.class,
    TradeDtoValidationException.class,
    UserNotActiveException.class,
    AssetNotPresentException.class
  })
  public ResponseEntity<String> handleUserExceptions(Exception ex) {
    return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGeneralException(Exception ex) {
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body("An unexpected error occurred: " + ex.getMessage());
  }
}
