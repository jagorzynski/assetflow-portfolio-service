package com.sothrose.assetflow_portfolio_service.service;

import static com.sothrose.assetflow_portfolio_service.model.ActionType.*;
import static com.sothrose.assetflow_portfolio_service.model.ProcessingStatus.SUCCESS;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;

import com.sothrose.assetflow_portfolio_service.client.UserServiceClient;
import com.sothrose.assetflow_portfolio_service.exception.*;
import com.sothrose.assetflow_portfolio_service.model.*;
import com.sothrose.assetflow_portfolio_service.repository.PortfolioRepository;
import com.sothrose.assetflow_portfolio_service.validator.PortfolioDtoValidator;
import com.sothrose.assetflow_portfolio_service.validator.TradeDtoValidator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PortfolioService {
  public static final String DELIMITER = ", ";
  public static final String USER_NOT_ACTIVE_LOG_MSG = "User with id: [{}] is not active";
  public static final String USER_NOT_ACTIVE_EXCEPTION_MSG = "User with id: [%s] is not active";
  public static final String PORTFOLIO_NOT_PRESENT_LOG_MSG =
      "Portfolio with id: [{}] for user with id: [{}] not present";
  public static final String PORTFOLIO_NOT_PRESENT_ERROR_MSG =
      "Error occurred when fetching portfolio with id: [%s] for user with id: [%s], portfolio not present";
  public static final String NOT_ENOUGH_QUANTITY_LOG_MSG =
      "Error occurred when performing transaction on asset: [{}] for user with id: [{}], not enough quantity present in the portfolio";
  public static final String NOT_ENOUGH_QUANTITY_EXCEPTION_MSG =
      "Error occurred when performing transaction on asset: [%s] for user with id: [%s], not enough quantity present in the portfolio";

  @Value("${kafka.topic.portfolio-updates}")
  private String portfolioUpdatesTopic;

  private final PortfolioRepository portfolioRepository;
  private final UserServiceClient userServiceClient;
  private final TradeDtoValidator tradeDtoValidator;
  private final PortfolioDtoValidator portfolioDtoValidator;
  private final KafkaTemplate<String, PortfolioUpdatedEvent> kafkaTemplate;

  @KafkaListener(topics = "${kafka.topic.trade-created}", groupId = "portfolio-service-group")
  public void handleTradeCreated(TradeCreatedEvent event, Acknowledgment ack) {
    try {
      log.info("Received an event for processing: [{}]", event);
      processTrade(event.toTradeDto());
      ack.acknowledge();
    } catch (Exception e) {
      throw new RecoverableDataAccessException(
          format("Processing an event failed, will retry: [%s]", e.getMessage()), e);
    }
  }

  public void createPortfolio(PortfolioDto portfolioDto) {
    var portfolioDtoValidationErrors = portfolioDtoValidator.validatePortfolioDto(portfolioDto);

    if (!portfolioDtoValidationErrors.isEmpty()) {
      var validationErrors = join(DELIMITER, portfolioDtoValidationErrors);
      log.error("PortfolioDto contains validation errors: [{}]", validationErrors);
      throw new PortfolioDtoValidationError(
          format("PortfolioDto contains validation errors: [%s]", validationErrors));
    }

    var userId = portfolioDto.getUserId();
    if (!isActiveUser(userId)) {
      userNotActiveLog(userId);
      throw new UserNotActiveException(format(USER_NOT_ACTIVE_EXCEPTION_MSG, userId));
    }

    var portfolioType = portfolioDto.getPortfolioType();
    var exchangeName = portfolioDto.getExchangeName();
    var alreadyExistingPortfolioOpt =
        portfolioRepository.findByUserIdAndPortfolioTypeAndExchange(
            userId, portfolioType, exchangeName);
    if (alreadyExistingPortfolioOpt.isPresent()) {
      log.error(
          "Portfolio for user with id: [{}] with portfolio type: [{}] for exchange: [{}] already present",
          userId,
          portfolioType,
          exchangeName);
      throw new PortfolioAlreadyPresentException(
          format(
              "Portfolio for user with id: [%s] with portfolio type: [%s] for exchange: [%s] already present",
              userId, portfolioType, exchangeName));
    }

    log.info("Saving new portfolio: [{}] for a user with id: [{}]", portfolioType, userId);
    portfolioRepository.save(portfolioDto.toPortfolio());
  }

  @Retry(name = "mongoPortfolio")
  @CircuitBreaker(name = "mongoPortfolio")
  @Bulkhead(name = "mongoPortfolio", type = Bulkhead.Type.SEMAPHORE)
  public PortfolioDto fetchPortfolioById(String portfolioId) {
    log.info("Fetching portfolio with id: [{}]", portfolioId);
    return portfolioRepository.findById(portfolioId).map(PortfolioDto::from).orElse(null);
  }

  @Retry(name = "mongoPortfolio")
  @CircuitBreaker(name = "mongoPortfolio")
  @Bulkhead(name = "mongoPortfolio", type = Bulkhead.Type.SEMAPHORE)
  public List<PortfolioDto> fetchAllPortfoliosForUser(Long userId) {
    log.info("Fetching all portfolios for a user with id: [{}]", userId);
    return portfolioRepository.findAllByUserId(userId).stream().map(PortfolioDto::from).toList();
  }

  @Retry(name = "mongoPortfolio")
  @CircuitBreaker(name = "mongoPortfolio")
  @Bulkhead(name = "mongoPortfolio", type = Bulkhead.Type.SEMAPHORE)
  public void deletePortfolioById(String portfolioId) {
    log.info("Deleting portfolio with id: [{}]", portfolioId);
    portfolioRepository.deleteById(portfolioId);
  }

  public ProcessingStatus deposit(DepositDto depositDto) {

    var userId = depositDto.getUserId();
    if (!isActiveUser(userId)) {
      userNotActiveLog(userId);
      throw new UserNotActiveException(format(USER_NOT_ACTIVE_EXCEPTION_MSG, userId));
    }

    var portfolioId = depositDto.getPortfolioId();
    var portfolioOpt = portfolioRepository.findByIdAndUserId(portfolioId, userId);

    if (portfolioOpt.isEmpty()) {
      portfolioNotPresentLog(portfolioId, userId);
      throw new PortfolioNotPresentException(
          format(PORTFOLIO_NOT_PRESENT_ERROR_MSG, portfolioId, userId));
    }

    var portfolio = portfolioOpt.get();
    var assetName = depositDto.getAssetName();
    var presentAssetOpt =
        portfolio.getAssets().stream()
            .filter(asset -> asset.getName().equalsIgnoreCase(assetName))
            .findFirst();

    var depositQuantity = depositDto.getQuantity();
    if (presentAssetOpt.isEmpty()) {
      portfolio.getAssets().add(new Asset(assetName, depositQuantity, depositDto.getAssetType()));
      log.info("Saving deposit for a user with id: [{}]", depositDto.getUserId());
      portfolioRepository.save(portfolio);

      var event =
          new PortfolioUpdatedEvent(
              portfolioId,
              userId,
              assetName,
              depositQuantity,
              DEPOSIT.name(),
              ZERO,
              now(),
              DEPOSIT);
      sendEvent(event);
      return SUCCESS;
    }

    var presentAsset = presentAssetOpt.get();
    presentAsset.addQuantity(depositQuantity);
    log.info("Saving deposit for a user with id: [{}]", depositDto.getUserId());
    portfolioRepository.save(portfolio);

    var event =
        new PortfolioUpdatedEvent(
            portfolioId, userId, assetName, depositQuantity, DEPOSIT.name(), ZERO, now(), DEPOSIT);
    sendEvent(event);
    return SUCCESS;
  }

  public ProcessingStatus withdraw(WithdrawDto withdrawDto) {

    var userId = withdrawDto.getUserId();
    if (!isActiveUser(userId)) {
      userNotActiveLog(userId);
      throw new UserNotActiveException(format(USER_NOT_ACTIVE_EXCEPTION_MSG, userId));
    }

    var portfolioId = withdrawDto.getPortfolioId();
    var portfolioOpt = portfolioRepository.findByIdAndUserId(portfolioId, userId);

    if (portfolioOpt.isEmpty()) {
      portfolioNotPresentLog(portfolioId, userId);
      throw new PortfolioNotPresentException(
          format(PORTFOLIO_NOT_PRESENT_ERROR_MSG, portfolioId, userId));
    }

    var portfolio = portfolioOpt.get();
    var assetName = withdrawDto.getAssetName();
    var presentAssetOpt =
        portfolio.getAssets().stream()
            .filter(asset -> asset.getName().equalsIgnoreCase(assetName))
            .findFirst();

    if (presentAssetOpt.isEmpty()) {
      log.error(
          "Error occurred when fetching asset to withdraw: [{}] from portfolio with id: [{}], asset not present in portfolio",
          assetName,
          portfolioId);
      throw new AssetNotPresentException(
          format(
              "Error occurred when fetching asset to withdraw: [%s] from portfolio with id: [%s], asset not present in portfolio",
              assetName, portfolioId));
    }

    var presentAsset = presentAssetOpt.get();
    var presentAssetName = presentAsset.getName();
    var quantityToWithdraw = withdrawDto.getQuantityToWithdraw();
    if (notEnoughAssetQuantity(presentAsset, quantityToWithdraw)) {
      notEnoughAssetQuantityLog(presentAssetName, userId);
      throw new NotEnoughAssetQuantityException(
          format(NOT_ENOUGH_QUANTITY_EXCEPTION_MSG, presentAssetName, userId));
    }

    presentAsset.removeQuantity(quantityToWithdraw);
    log.info("Saving withdrawal for a user with id: [{}]", withdrawDto.getUserId());
    portfolioRepository.save(portfolio);

    var event =
        new PortfolioUpdatedEvent(
            portfolioId,
            userId,
            WITHDRAW.name(),
            ZERO,
            assetName,
            quantityToWithdraw,
            now(),
            WITHDRAW);
    sendEvent(event);
    return SUCCESS;
  }

  @Transactional
  public ProcessingStatus processTrade(TradeDto tradeDto) {
    var tradeDtoValidationErrors = tradeDtoValidator.validateTradeDto(tradeDto);

    if (!tradeDtoValidationErrors.isEmpty()) {
      var validationErrors = join(DELIMITER, tradeDtoValidationErrors);
      log.error("TradeDto contains validation errors: [{}]", validationErrors);
      throw new TradeDtoValidationError(
          format("TradeDto contains validation errors: [%s]", validationErrors));
    }

    var userId = tradeDto.getUserId();
    if (!isActiveUser(userId)) {
      userNotActiveLog(userId);
      throw new UserNotActiveException(format(USER_NOT_ACTIVE_EXCEPTION_MSG, userId));
    }

    var exchangeName = tradeDto.getExchangeName();
    var assetType = tradeDto.getAssetType();
    var portfolioOpt =
        portfolioRepository.findByUserIdAndPortfolioTypeAndExchange(
            userId, assetType, exchangeName);

    if (portfolioOpt.isEmpty()) {
      log.error(
          "Portfolio type: [{}] in exchange: [{}] for user with id: [{}] not present",
          assetType,
          exchangeName,
          userId);
      throw new PortfolioNotPresentException(
          format(
              "Error occurred when fetching portfolio type: [%s] in exchange: [%s] for user with id: [%s], portfolio not present",
              assetType, exchangeName, userId));
    }

    var portfolio = portfolioOpt.get();
    var assetToPay = tradeDto.getAssetToPay();
    var presentAssetToPayOpt =
        portfolio.getAssets().stream()
            .filter(asset -> asset.getName().equalsIgnoreCase(assetToPay))
            .findFirst();
    var portfolioId = portfolio.getId();

    if (presentAssetToPayOpt.isEmpty()) {
      log.error(
          "Error occurred when fetching asset to pay: [{}] from portfolio with id: [{}], asset not present in portfolio",
          assetToPay,
          portfolioId);
      throw new AssetNotPresentException(
          format(
              "Error occurred when fetching asset to pay: [%s] from portfolio with id: [%s], asset not present in portfolio",
              assetToPay, portfolioId));
    }

    var presentAssetToPay = presentAssetToPayOpt.get();
    var presentAssetToPayName = presentAssetToPay.getName();
    var amountToPay = tradeDto.getAmountToPay();
    if (notEnoughAssetQuantity(presentAssetToPay, amountToPay)) {
      notEnoughAssetQuantityLog(presentAssetToPayName, userId);
      throw new NotEnoughAssetQuantityException(
          format(NOT_ENOUGH_QUANTITY_EXCEPTION_MSG, presentAssetToPayName, userId));
    }

    presentAssetToPay.removeQuantity(amountToPay);

    var assetToBuy = tradeDto.getAssetToBuy();
    var presentAssetToBuyOpt =
        portfolio.getAssets().stream()
            .filter(asset -> asset.getName().equalsIgnoreCase(assetToBuy))
            .findFirst();

    var amountBought = tradeDto.getAmountBought();
    if (presentAssetToBuyOpt.isEmpty()) {
      portfolio.getAssets().add(new Asset(assetToBuy, amountBought, assetType));
      log.info("Saving new trade: [{}] for a user with id: [{}]", assetType, userId);
      portfolioRepository.save(portfolio);
      return SUCCESS;
    }

    var presentAssetToBuy = presentAssetToBuyOpt.get();
    presentAssetToBuy.addQuantity(amountBought);
    log.info("Saving new trade: [{}] for a user with id: [{}]", assetType, userId);
    portfolioRepository.save(portfolio);

    var event =
        new PortfolioUpdatedEvent(
            portfolioId, userId, assetToBuy, amountBought, assetToPay, amountToPay, now(), TRADE);
    sendEvent(event);
    return SUCCESS;
  }

  private void sendEvent(PortfolioUpdatedEvent event) {
    kafkaTemplate.send(portfolioUpdatesTopic, event);
  }

  private boolean notEnoughAssetQuantity(Asset presentAssetToPay, BigDecimal amountToPay) {
    return presentAssetToPay.getQuantity().compareTo(amountToPay) < 0;
  }

  private boolean isActiveUser(Long userId) {
    var userDto = userServiceClient.fetchUserData(userId);

    if (isNull(userDto) || isNull(userDto.getIsActive())) {
      log.info(
          "Error occurred when fetching user data for user with id: [{}], UserDto contains validation errors",
          userId);
      throw new UserDtoValidationException(
          format(
              "Error occurred when fetching user data for user with id: [%s], UserDto contains validation errors",
              userId));
    }

    return userDto.getIsActive();
  }

  private void userNotActiveLog(Long userId) {
    log.error(USER_NOT_ACTIVE_LOG_MSG, userId);
  }

  private void portfolioNotPresentLog(String portfolioId, Long userId) {
    log.error(PORTFOLIO_NOT_PRESENT_LOG_MSG, portfolioId, userId);
  }

  private void notEnoughAssetQuantityLog(String presentAssetName, Long userId) {
    log.error(NOT_ENOUGH_QUANTITY_LOG_MSG, presentAssetName, userId);
  }
}
