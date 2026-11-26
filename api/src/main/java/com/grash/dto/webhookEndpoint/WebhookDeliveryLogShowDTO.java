package com.grash.dto.webhookEndpoint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Delivery attempt of an outgoing webhook")
public class WebhookDeliveryLogShowDTO {
    private Long id;
    private Long webhookEndpointId;
    private String eventType;
    private String payload;
    @Schema(description = "HTTP status returned by the receiver, null if there was no response")
    private Integer responseStatus;
    private String responseBody;
    private Long durationMs;
    private boolean success;
    private String errorMessage;
    private Instant createdAt;
}
