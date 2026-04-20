package com.grash.dto.paddle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle trial period configuration")
public class TrialPeriod {
    @Schema(description = "Trial interval (e.g., day, week, month)")
    private String interval;

    @Schema(description = "Trial frequency within the interval")
    private String frequency;
}
