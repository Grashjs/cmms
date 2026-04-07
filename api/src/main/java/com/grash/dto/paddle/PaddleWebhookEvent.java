package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// Webhook DTOs
@Data
@Schema(description = "Paddle webhook event payload")
public class PaddleWebhookEvent {
    @Schema(description = "Unique event ID")
    @JsonProperty("event_id")
    private String eventId;

    @Schema(description = "Type of the webhook event")
    @JsonProperty("event_type")
    private String eventType;

    @Schema(description = "Timestamp when the event occurred (ISO 8601)")
    @JsonProperty("occurred_at")
    private String occurredAt;

    @Schema(description = "Unique notification ID")
    @JsonProperty("notification_id")
    private String notificationId;

    @Schema(description = "Transaction data payload")
    private PaddleTransactionData data;
}

