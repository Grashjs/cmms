package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring payment reminder configuration")
public class PaymentReminder {
    @Schema(description = "Time unit for the reminder interval")
    public String intervalUnit;
    @Schema(description = "Length of the reminder interval")
    public int intervalLength;
}
