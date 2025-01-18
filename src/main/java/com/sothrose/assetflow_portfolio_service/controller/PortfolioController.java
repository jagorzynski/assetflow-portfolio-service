package com.sothrose.assetflow_portfolio_service.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.sothrose.assetflow_portfolio_service.model.*;
import com.sothrose.assetflow_portfolio_service.service.PortfolioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/v1/assetflow/portfolios")
public class PortfolioController {

  private final PortfolioService portfolioService;

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  public void createPortfolio(@RequestBody PortfolioDto portfolioDto) {
    portfolioService.createPortfolio(portfolioDto);
  }

  @GetMapping(path = "/{portfolioId}", produces = APPLICATION_JSON_VALUE)
  public PortfolioDto getPortfolioById(@PathVariable String portfolioId) {
    return portfolioService.fetchPortfolioById(portfolioId);
  }

  @GetMapping(path = "/all/{userId}", produces = APPLICATION_JSON_VALUE)
  public List<PortfolioDto> getAllPortfoliosForUser(@PathVariable Long userId) {
    return portfolioService.fetchAllPortfoliosForUser(userId);
  }

  @DeleteMapping(path = "/{portfolioId}")
  public void deletePortfolioById(@PathVariable String portfolioId) {
    portfolioService.deletePortfolioById(portfolioId);
  }

  @PostMapping(path = "/deposit", consumes = APPLICATION_JSON_VALUE)
  public ProcessingStatus processDeposit(@RequestBody DepositDto depositDto) {
    return portfolioService.deposit(depositDto);
  }

  @PostMapping(path = "/withdraw", consumes = APPLICATION_JSON_VALUE)
  public ProcessingStatus processWithdraw(@RequestBody WithdrawDto withdrawDto) {
    return portfolioService.withdraw(withdrawDto);
  }

  @PostMapping(path = "/trade", consumes = APPLICATION_JSON_VALUE)
  public ProcessingStatus processTrade(@RequestBody TradeDto tradeDto) {
    return portfolioService.processTrade(tradeDto);
  }
}
