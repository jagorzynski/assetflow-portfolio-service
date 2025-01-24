package com.sothrose.assetflow_portfolio_service.service;

import static java.util.concurrent.TimeUnit.MINUTES;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisService {
  public static final String REDIS_SERVICE = "redisService";
  public static final String RETRY_REDIS_SERVICE = "retryRedisService";
  public static final String BULKHEAD_REDIS_SERVICE = "bulkheadRedisService";
  private final RedisTemplate<String, String> portfolioRedisTemplate;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;
  private final Bulkhead bulkhead;

  public RedisService(
      RedisTemplate<String, String> portfolioRedisTemplate,
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      BulkheadRegistry bulkheadRegistry) {

    this.portfolioRedisTemplate = portfolioRedisTemplate;
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(REDIS_SERVICE);
    this.retry = retryRegistry.retry(RETRY_REDIS_SERVICE);
    this.bulkhead = bulkheadRegistry.bulkhead(BULKHEAD_REDIS_SERVICE);
  }

  public String getValue(String key) {
    Callable<String> callable = () -> portfolioRedisTemplate.opsForValue().get(key);
    Callable<String> resilientCallable =
        circuitBreaker.decorateCallable(retry.decorateCallable(callable));

    try {
      return bulkhead.executeCallable(resilientCallable);
    } catch (Exception ex) {
      log.warn(
          "Error occurred when fetching key [{}] from redis cache, using fallback value. Error: [{}]",
          key,
          ex.getMessage(),
          ex);
      return handleRedisFailure(ex, key);
    }
  }

  public void setValue(String key, String value) {
    Callable<Void> callable =
        () -> {
          // FIXME do i want config here?
          portfolioRedisTemplate.opsForValue().set(key, value,2, MINUTES);
          return null;
        };

    Callable<Void> resilientCallable =
        circuitBreaker.decorateCallable(retry.decorateCallable(callable));

    try {
      bulkhead.executeCallable(resilientCallable);
    } catch (Exception ex) {
      log.warn(
          "Error occurred when saving key [{}] in redis cache, value [{}]. Error: [{}]",
          key,
          value,
          ex.getMessage(),
          ex);
    }
  }

  private String handleRedisFailure(Throwable ex, String key) {
    if (ex instanceof RedisConnectionFailureException) {
      log.warn("Redis connection failure for key [{}], using fallback value.", key);
    } else {
      log.warn(
          "Unknown Redis failure for key [{}], using fallback value. Error: [{}]",
          key,
          ex.getMessage());
    }
    return "no_value";
  }
}
