package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "Payload for registering a push notification token")
public class PushTokenPayload {
    @Schema(description = "Push notification token")
    @NotNull
    private String token;
}

