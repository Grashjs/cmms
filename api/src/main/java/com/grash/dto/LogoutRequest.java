package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to sign out the current device")
public class LogoutRequest {

    @Schema(description = "Refresh token of the session to close. When omitted, every session of the user is revoked " +
            "(legacy behaviour).")
    private String refreshToken;
}
