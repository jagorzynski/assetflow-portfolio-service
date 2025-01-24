package com.sothrose.assetflow_portfolio_service.service;

import static com.sothrose.assetflow_portfolio_service.model.AssetType.CRYPTO;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sothrose.assetflow_portfolio_service.client.UserServiceClient;
import com.sothrose.assetflow_portfolio_service.exception.PortfolioAlreadyPresentException;
import com.sothrose.assetflow_portfolio_service.exception.PortfolioDtoValidationException;
import com.sothrose.assetflow_portfolio_service.exception.UserNotActiveException;
import com.sothrose.assetflow_portfolio_service.model.Portfolio;
import com.sothrose.assetflow_portfolio_service.model.PortfolioDto;
import com.sothrose.assetflow_portfolio_service.model.PortfolioUpdatedEvent;
import com.sothrose.assetflow_portfolio_service.model.UserDto;
import com.sothrose.assetflow_portfolio_service.repository.PortfolioRepository;
import com.sothrose.assetflow_portfolio_service.validator.PortfolioDtoValidator;
import com.sothrose.assetflow_portfolio_service.validator.TradeDtoValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

class PortfolioServiceTest {
  @Mock private PortfolioRepository portfolioRepository;
  @Mock private UserServiceClient userServiceClient;
  @Mock private TradeDtoValidator tradeDtoValidator;
  @Mock private PortfolioDtoValidator portfolioDtoValidator;
  @Mock private KafkaTemplate<String, PortfolioUpdatedEvent> kafkaTemplate;
  @Mock private RedisService redisService;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private PortfolioService portfolioService;

  private PortfolioDto portfolioDto;
  private Portfolio portfolio;
  private Long userId = 1L;
  private String portfolioId = "portfolio123";

  private AutoCloseable autoCloseable;

  @BeforeEach
  void setUp() {
    autoCloseable = MockitoAnnotations.openMocks(this);
    portfolioDto = new PortfolioDto();
    portfolioDto.setUserId(userId);
    portfolioDto.setPortfolioType(CRYPTO);
    portfolioDto.setExchangeName("Binance");

    portfolio = new Portfolio();
    portfolio.setId(portfolioId);
    portfolio.setUserId(userId);
  }

  @AfterEach
  void tearDown() throws Exception {
    autoCloseable.close();
  }

  @Test
  void shouldSuccessfullySaveValidPortfolio() throws JsonProcessingException {
    // given
    var expectedJson =
        """
    {
      "id": 1,
      "username": "johnDo",
      "firstName": "john",
      "lastName": "do",
      "email": "john@do.pl",
      "birthday": "2000-01-01",
      "active": true
    }
    """;

    var testUserDto = testUserDto(true);
    when(portfolioDtoValidator.validatePortfolioDto(any())).thenReturn(List.of());
    when(userServiceClient.fetchUserData(userId)).thenReturn(testUserDto);
    when(portfolioRepository.findByUserIdAndPortfolioTypeAndExchange(userId, CRYPTO, "Binance"))
        .thenReturn(empty());
    when(objectMapper.writeValueAsString(testUserDto)).thenReturn(expectedJson);

    // when
    portfolioService.createPortfolio(portfolioDto);

    // then
    verify(portfolioRepository).save(any());
  }

  private UserDto testUserDto(boolean active) {
    return new UserDto(1L, "johnDo", "john", "do", "john@do.pl", LocalDate.of(2000, 1, 1), active);
  }

  @Test
  void shouldThrowExceptionWhenSavingInvalidPortfolio() {
    // given
    var validationErrorMsg = "ExchangeName cannot be null or empty";
    when(portfolioDtoValidator.validatePortfolioDto(any())).thenReturn(List.of(validationErrorMsg));

    // when
    var throwable =
        assertThrows(
            PortfolioDtoValidationException.class,
            () -> portfolioService.createPortfolio(portfolioDto));

    // then
    assertEquals(
        throwable.getMessage(),
        format("PortfolioDto contains validation errors: [%s]", validationErrorMsg));
  }

  @Test
  void shouldThrowExceptionWhenSavingPortfolioAndUserNotActive() throws JsonProcessingException {
    // given
    var expectedJson =
        """
        {
          "id": 1,
          "username": "johnDo",
          "firstName": "john",
          "lastName": "do",
          "email": "john@do.pl",
          "birthday": "2000-01-01",
          "active": false
        }
        """;

    var testUserDto = testUserDto(false);
    var validationErrorMsg = "User with id: [1] is not active";
    when(portfolioDtoValidator.validatePortfolioDto(any())).thenReturn(List.of());
    when(userServiceClient.fetchUserData(userId)).thenReturn(testUserDto);
    when(objectMapper.writeValueAsString(testUserDto)).thenReturn(expectedJson);

    // when
    var throwable =
        assertThrows(
            UserNotActiveException.class, () -> portfolioService.createPortfolio(portfolioDto));

    // then
    assertEquals(throwable.getMessage(), validationErrorMsg);
  }

  @Test
  void shouldThrowExceptionWhenSavingPortfolioAndPortfolioAlreadyExists()
      throws JsonProcessingException {
    // given
    var expectedJson =
        """
        {
          "id": 1,
          "username": "johnDo",
          "firstName": "john",
          "lastName": "do",
          "email": "john@do.pl",
          "birthday": "2000-01-01",
          "active": true
        }
        """;

    var testUserDto = testUserDto(true);
    when(portfolioDtoValidator.validatePortfolioDto(any())).thenReturn(List.of());
    when(userServiceClient.fetchUserData(userId)).thenReturn(testUserDto);
    when(objectMapper.writeValueAsString(testUserDto)).thenReturn(expectedJson);
    when(portfolioRepository.findByUserIdAndPortfolioTypeAndExchange(userId, CRYPTO, "Binance"))
        .thenReturn(Optional.of(portfolio));

    // when
    var throwable =
        assertThrows(
            PortfolioAlreadyPresentException.class,
            () -> portfolioService.createPortfolio(portfolioDto));

    // then
    assertTrue(throwable.getMessage().startsWith("Portfolio for user with id:"));
  }

  // TODO continue
}
