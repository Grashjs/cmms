package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// Webhook DTOs
@Data
public class PaddleWebhookEvent {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("occurred_at")
    private String occurredAt;

    @JsonProperty("notification_id")
    private String notificationId;

    private PaddleTransactionData data;
}

