package com.sothrose.assetflow_portfolio_service.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDto {
  private Long userId;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private LocalDate birthday;
  private Boolean isActive;
}
