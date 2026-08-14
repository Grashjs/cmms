package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to renew an access token using a refresh token")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(description = "Opaque refresh token issued at sign in")
    private String refreshToken;
}
