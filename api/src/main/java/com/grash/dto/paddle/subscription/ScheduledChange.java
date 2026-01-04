package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScheduledChange {
    @JsonProperty("action")
    private String action;

    @JsonProperty("effective_at")
    private String effectiveAt;

    @JsonProperty("resume_at")
    private String resumeAt;
}
