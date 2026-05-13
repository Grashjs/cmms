package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle subscription webhook event payload")
public class PaddleSubscriptionWebhookEvent {
    @Schema(description = "Unique event ID")

    private String eventId;

    @Schema(description = "Type of the webhook event")

    private String eventType;

    @Schema(description = "Timestamp when the event occurred (ISO 8601)")

    private String occurredAt;

    @Schema(description = "Subscription data payload")

    private PaddleSubscriptionData data;
}

