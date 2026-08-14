package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class AuthTokens {

    private String accessToken;

    private String refreshToken;

    private Date accessTokenExpiresAt;
}
