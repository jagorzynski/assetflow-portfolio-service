package com.sothrose.assetflow_portfolio_service.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Asset {
  private String name;
  private BigDecimal quantity;
  private AssetType assetType;

  public void addQuantity(BigDecimal value) {
    quantity = quantity.add(value);
  }

  public void removeQuantity(BigDecimal value) {
    quantity = quantity.subtract(value);
  }
}
