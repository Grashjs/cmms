package com.grash.dto.paddle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle payment terms configuration")
public class PaymentTerms {
    @Schema(description = "Billing interval (e.g., day, week, month, year)")
    private String interval;

    @Schema(description = "Frequency of billing within the interval")
    private String frequency;
}
