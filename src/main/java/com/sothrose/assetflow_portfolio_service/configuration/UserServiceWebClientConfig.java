package com.sothrose.assetflow_portfolio_service.configuration;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class UserServiceWebClientConfig {

  @Value("${user-service.base-url:http://localhost:8081}")
  private String userServiceBaseUrl;

  @Bean
  public WebClient userServiceWebClient(
      WebClient.Builder webClientBuilder,
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      RateLimiterRegistry rateLimiterRegistry,
      BulkheadRegistry bulkheadRegistry,
      TimeLimiterRegistry timeLimiterRegistry) {

    var circuitBreaker = circuitBreakerRegistry.circuitBreaker("userServiceUserData");
    var retry = retryRegistry.retry("retryUserServiceUserData");
    var rateLimiter = rateLimiterRegistry.rateLimiter("userServiceUserData");
    var bulkhead = bulkheadRegistry.bulkhead("bulkheadUserServiceUserData");
    var timeLimiter = timeLimiterRegistry.timeLimiter("userServiceUserData");

    return webClientBuilder
        .baseUrl(userServiceBaseUrl)
        .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .filter(
            (request, next) ->
                Mono.defer(() -> next.exchange(request))
                    .transformDeferred(RetryOperator.of(retry))
                    .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .transformDeferred(RateLimiterOperator.of(rateLimiter))
                    .transformDeferred(BulkheadOperator.of(bulkhead))
                    .onErrorResume(ex -> Mono.just(fallbackClientResponse(ex))))
        .build();
  }

  private ClientResponse fallbackClientResponse(Throwable ex) {
    log.warn(
        "Fallback method executed, returning default user data, exception: [{}]", ex.getMessage());

    var defaultResponse =
        """
        {
            "id": 0,
            "firstName": "Unknown",
            "lastName": "Unknown",
            "email": "Unknown",
            "username": "Unknown",
            "date": "%s",
            "isActive": false
        }
        """
            .formatted(LocalDate.now());

    return ClientResponse.create(HttpStatus.OK)
        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .body(defaultResponse)
        .build();
  }
}
