package com.sothrose.assetflow_portfolio_service.model;

import lombok.*;

import java.math.BigDecimal;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DepositDto {
    private String portfolioId;
    private Long userId;
    private String assetName;
    private BigDecimal quantity;
    private AssetType assetType;
}
