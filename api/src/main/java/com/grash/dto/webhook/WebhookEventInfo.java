package com.grash.dto.webhook;

import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Information about a specific webhook event type")
public class WebhookEventInfo {
    @Schema(description = "The webhook event identifier", example = "WORK_ORDER_STATUS_CHANGE")
    private WebhookEvent eventType;

    @Schema(description = "Human-readable description of the event", example = "Triggered when a work order status is changed")
    private String description;

    @Schema(description = "Example payload sent when this event occurs")
    private Object examplePayload;

    @Schema(description = "Filterable fields for this event type")
    private Object filterableFields;
}
