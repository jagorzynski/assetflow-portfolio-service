package com.sothrose.assetflow_portfolio_service.validator;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;

import com.sothrose.assetflow_portfolio_service.model.PortfolioDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PortfolioDtoValidator {
  public List<String> validatePortfolioDto(PortfolioDto portfolioDto) {
    List<String> validationErrors = newArrayList();

    if (isNull(portfolioDto)) {
      validationErrors.add("PortfolioDto cannot be null");
    }

    if (isNull(portfolioDto.getUserId())) {
      validationErrors.add("UserId cannot be null");
    }

    if (isNullOrEmpty(portfolioDto.getExchangeName())) {
      validationErrors.add("ExchangeName cannot be null or empty");
    }

    return validationErrors;
  }
}
