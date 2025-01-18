package com.sothrose.assetflow_portfolio_service.model;

import java.math.BigDecimal;
import lombok.*;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TradeCreatedEvent {
  private Long userId;
  private String assetToPay;
  private BigDecimal amountToPay;
  private String assetToBuy;
  private BigDecimal amountBought;
  private AssetType assetType;
  private String exchangeName;

  public TradeDto toTradeDto() {
    return TradeDto.builder()
        .userId(userId)
        .assetToPay(assetToPay)
        .amountToPay(amountToPay)
        .assetToBuy(assetToBuy)
        .amountBought(amountBought)
        .assetType(assetType)
        .exchangeName(exchangeName)
        .build();
  }
}
