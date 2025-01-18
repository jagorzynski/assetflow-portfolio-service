package com.sothrose.assetflow_portfolio_service.validator;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.isNull;

import com.sothrose.assetflow_portfolio_service.model.TradeDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TradeDtoValidator {
  public List<String> validateTradeDto(TradeDto tradeDto) {
    List<String> validationErrors = newArrayList();

    if (isNull(tradeDto)) {
      validationErrors.add("TradeDto cannot be null");
    }

    if (isNull(tradeDto.getUserId())) {
      validationErrors.add("UserId cannot be null");
    }

    if (isNullOrEmpty(tradeDto.getAssetToPay())) {
      validationErrors.add("AssetToPay cannot be null or empty");
    }

    if (isNull(tradeDto.getAmountToPay()) || tradeDto.getAmountToPay().compareTo(ZERO) <= 0) {
      validationErrors.add("AmountToPay cannot be null or less or equal to 0");
    }

    if (isNullOrEmpty(tradeDto.getAssetToBuy())) {
      validationErrors.add("AssetToBuy cannot be null or empty");
    }

    if (isNull(tradeDto.getAmountBought()) || tradeDto.getAmountBought().compareTo(ZERO) <= 0) {
      validationErrors.add("AmountToPay cannot be null or less or equal to 0");
    }

    if (isNullOrEmpty(tradeDto.getExchangeName())) {
      validationErrors.add("ExchangeName cannot be null or empty");
    }

    return validationErrors;
  }
}
