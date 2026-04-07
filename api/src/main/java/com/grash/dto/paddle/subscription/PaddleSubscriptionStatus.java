package com.grash.dto.paddle.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paddle subscription status (active, canceled, past_due, paused, trialing)")
public enum PaddleSubscriptionStatus {
    active,
    canceled,
    past_due,
    paused,
    trialing
}
