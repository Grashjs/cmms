package com.grash.dto.paddle.subscription;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateSubscriptionRequest {

    @NotNull
    private String planId;

    @Min(1)
    @NotNull
    private Integer quantity;
}