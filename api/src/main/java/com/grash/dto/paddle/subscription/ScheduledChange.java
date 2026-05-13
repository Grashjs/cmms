package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle subscription scheduled change details")
public class ScheduledChange {
    @Schema(description = "Change action (e.g., cancel, pause, resume)")

    private String action;

    @Schema(description = "Date and time when the change takes effect (ISO 8601)")

    private String effectiveAt;

    @Schema(description = "Date and time when the subscription resumes (ISO 8601)")

    private String resumeAt;
}
