package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;

@Data
@Schema(description = "FastSpring webhook payload containing a list of events")
public class WebhookPayload<T> {
    @Schema(description = "List of FastSpring webhook events")
    public ArrayList<Event<T>> events;
}
