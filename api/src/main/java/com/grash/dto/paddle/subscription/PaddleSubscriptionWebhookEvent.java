package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaddleSubscriptionWebhookEvent {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("occurred_at")
    private String occurredAt;

    @JsonProperty("data")
    private PaddleSubscriptionData data;
}

