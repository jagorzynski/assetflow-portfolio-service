package com.sothrose.assetflow_portfolio_service.client;

import com.sothrose.assetflow_portfolio_service.model.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserServiceClient {

  @Value("${user-service.user-by-id.path:/v1/assetflow/users/{userId}}")
  private String userByIdPath;

  private final WebClient userServiceWebClient;

  public UserDto fetchUserData(Long userId) {
    return userServiceWebClient
        .get()
        .uri(uriBuilder -> uriBuilder.path(userByIdPath).build(userId))
        .retrieve()
        .bodyToMono(UserDto.class)
        .block();
  }
}
