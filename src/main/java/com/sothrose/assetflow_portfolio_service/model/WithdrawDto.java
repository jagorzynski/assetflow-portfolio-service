package com.sothrose.assetflow_portfolio_service.model;

import java.math.BigDecimal;
import lombok.*;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WithdrawDto {
  private String portfolioId;
  private Long userId;
  private String assetName;
  private BigDecimal quantityToWithdraw;
  private AssetType assetType;
}
