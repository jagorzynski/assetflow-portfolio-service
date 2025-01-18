package com.sothrose.assetflow_portfolio_service.repository;

import com.sothrose.assetflow_portfolio_service.model.AssetType;
import com.sothrose.assetflow_portfolio_service.model.Portfolio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {
  List<Portfolio> findAllByUserId(Long userId);

  Optional<Portfolio> findByIdAndUserId(String id, Long userId);

  Optional<Portfolio> findByUserIdAndPortfolioTypeAndExchange(
      Long userId, AssetType portfolioType, String exchange);
}
