package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring subscription cancellation settings")
public class CancellationSetting {
    @Schema(description = "Cancellation type or reason")
    public String cancellation;
    @Schema(description = "Time unit for cancellation interval (e.g., day, week, month)")
    public String intervalUnit;
    @Schema(description = "Length of the cancellation interval")
    public int intervalLength;
}
