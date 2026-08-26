package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Authentication response containing access and refresh tokens")
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 5926468583035150707L;

    @Schema(description = "JWT access token for authenticated requests")
    private String accessToken;

    @Schema(description = "Opaque refresh token used to obtain a new access token. Rotated on every use.")
    private String refreshToken;

    @Schema(description = "Token type")
    private String tokenType;

    @Schema(description = "Absolute expiration timestamp of the access token")
    private Date expiresAt;

    public static AuthResponse of(AuthTokens tokens) {
        return new AuthResponse(tokens.getAccessToken(), tokens.getRefreshToken(), "Bearer",
                tokens.getAccessTokenExpiresAt());
    }
}
