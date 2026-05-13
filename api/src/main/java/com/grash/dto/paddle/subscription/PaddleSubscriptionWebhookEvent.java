package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle subscription webhook event payload")
public class PaddleSubscriptionWebhookEvent {
    @Schema(description = "Unique event ID")
    @JsonProperty("event_id")
    private String eventId;

    @Schema(description = "Type of the webhook event")
    @JsonProperty("event_type")
    private String eventType;

    @Schema(description = "Timestamp when the event occurred (ISO 8601)")
    @JsonProperty("occurred_at")
    private String occurredAt;

    @Schema(description = "Subscription data payload")
    @JsonProperty("data")
    private PaddleSubscriptionData data;
}

