package com.sothrose.assetflow_portfolio_service.model;

import java.math.BigDecimal;
import lombok.*;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TradeDto {
  private Long userId;
  private String assetToPay;
  private BigDecimal amountToPay;
  private String assetToBuy;
  private BigDecimal amountBought;
  private AssetType assetType;
  private String exchangeName;
}
