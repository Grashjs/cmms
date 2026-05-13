package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// Webhook DTOs
@Data
@Schema(description = "Paddle webhook event payload")
public class PaddleWebhookEvent {
    @Schema(description = "Unique event ID")

    private String eventId;

    @Schema(description = "Type of the webhook event")

    private String eventType;

    @Schema(description = "Timestamp when the event occurred (ISO 8601)")

    private String occurredAt;

    @Schema(description = "Unique notification ID")

    private String notificationId;

    @Schema(description = "Transaction data payload")
    private PaddleTransactionData data;
}

