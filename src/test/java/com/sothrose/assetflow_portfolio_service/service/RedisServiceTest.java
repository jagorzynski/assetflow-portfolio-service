package com.sothrose.assetflow_portfolio_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisServiceTest {
  @Mock private RedisTemplate<String, String> portfolioRedisTemplate;
  @Mock private CircuitBreakerRegistry circuitBreakerRegistry;
  @Mock private RetryRegistry retryRegistry;
  @Mock private BulkheadRegistry bulkheadRegistry;
  @Mock private CircuitBreaker circuitBreaker;
  @Mock private Retry retry;
  @Mock private Bulkhead bulkhead;
  @Mock private ValueOperations<String, String> valueOperations;

  private RedisService redisService;

  private AutoCloseable autoCloseable;

  @BeforeEach
  void setUp() throws Exception {
    autoCloseable = MockitoAnnotations.openMocks(this);
    when(circuitBreakerRegistry.circuitBreaker("redisService")).thenReturn(circuitBreaker);
    when(retryRegistry.retry("retryRedisService")).thenReturn(retry);
    when(bulkheadRegistry.bulkhead("bulkheadRedisService")).thenReturn(bulkhead);

    redisService =
        new RedisService(
            portfolioRedisTemplate, circuitBreakerRegistry, retryRegistry, bulkheadRegistry);

    when(portfolioRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(circuitBreaker.decorateCallable(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(retry.decorateCallable(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(bulkhead.executeCallable(any()))
        .thenAnswer(
            invocation -> {
              Callable<?> callable = invocation.getArgument(0);
              return callable.call();
            });
  }

  @AfterEach
  void tearDown() throws Exception {
    autoCloseable.close();
  }

  @Test
  void shouldCorrectlyReturnCachedValue() {
    // given
    var key = "testKey";
    var expectedValue = "testValue";
    when(valueOperations.get(key)).thenReturn(expectedValue);

    // when
    var result = redisService.getValue(key);

    // then
    assertEquals(expectedValue, result);
    verify(valueOperations, times(1)).get(key);
  }

  @Test
  void shouldReturnFallbackValueWhenErrorOccurred() {
    // given
    var key = "testKey";
    when(valueOperations.get(key)).thenThrow(new RedisConnectionFailureException("Redis is down"));

    // when
    var result = redisService.getValue(key);

    // then
    assertEquals("no_value", result);
    verify(valueOperations, times(1)).get(key);
  }

  @Test
  void shouldCorrectlySetValueInCache() {
    // given
    var key = "testKey";
    var value = "testValue";
    doNothing().when(valueOperations).set(key, value, 2, TimeUnit.MINUTES);

    // when
    redisService.setValue(key, value);

    // then
    verify(valueOperations, times(1)).set(key, value, 2, TimeUnit.MINUTES);
  }

  @Test
  void setValue_shouldHandleException_whenRedisFails() {
    // given
    var key = "testKey";
    var value = "testValue";

    // when
    doThrow(new RedisConnectionFailureException("Redis is down"))
        .when(valueOperations)
        .set(key, value, 2, TimeUnit.MINUTES);

    // then
    assertDoesNotThrow(() -> redisService.setValue(key, value));
    verify(valueOperations, times(1)).set(key, value, 2, TimeUnit.MINUTES);
  }
}
