package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle billing period date range")
public class BillingPeriod {
    @Schema(description = "Start date and time of the billing period (ISO 8601)")
    @JsonProperty("starts_at")
    private String startsAt;

    @Schema(description = "End date and time of the billing period (ISO 8601)")
    @JsonProperty("ends_at")
    private String endsAt;
}
