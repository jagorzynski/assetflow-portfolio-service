package com.sothrose.assetflow_portfolio_service.model;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "portfolios")
public class Portfolio {
  @Id private String id;
  private Long userId;
  private AssetType portfolioType;
  private String exchange;
  private Set<Asset> assets;
}
