package com.sothrose.assetflow_portfolio_service.model;

import static java.util.Collections.emptySet;

import java.util.Set;
import lombok.*;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PortfolioDto {
  private String id;
  private Long userId;
  private AssetType portfolioType;
  private String exchangeName;
  private Set<Asset> assets;

  public static PortfolioDto from(Portfolio portfolio) {
    return new PortfolioDto(
        portfolio.getId(),
        portfolio.getUserId(),
        portfolio.getPortfolioType(),
        portfolio.getExchange(),
        portfolio.getAssets());
  }

  public Portfolio toPortfolio() {
    return new Portfolio(id, userId, portfolioType, exchangeName, emptySet());
  }
}
