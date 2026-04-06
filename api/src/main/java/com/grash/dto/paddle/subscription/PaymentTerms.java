package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle subscription payment terms configuration")
public class PaymentTerms {
    @Schema(description = "Billing interval (e.g., day, week, month, year)")
    @JsonProperty("interval")
    private String interval;

    @Schema(description = "Frequency of billing within the interval")
    @JsonProperty("frequency")
    private Integer frequency;
}
