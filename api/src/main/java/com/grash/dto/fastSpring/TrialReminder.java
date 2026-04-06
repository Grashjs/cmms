package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring trial period reminder configuration")
public class TrialReminder {
    @Schema(description = "Time unit for the trial reminder interval")
    public String intervalUnit;
    @Schema(description = "Length of the trial reminder interval")
    public int intervalLength;
}
