package com.sothrose.assetflow_portfolio_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PortfolioUpdatedEvent {
  private String portfolioId;
  private Long userId;
  private String haveName;
  private BigDecimal haveValue;
  private String owesName;
  private BigDecimal owesValue;
  private LocalDateTime timestamp;
  private ActionType actionType;
}
