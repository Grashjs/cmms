package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring overdue payment tracking information")
public class PaymentOverdue {
    @Schema(description = "Time unit for the overdue interval")
    public String intervalUnit;
    @Schema(description = "Length of the overdue interval")
    public int intervalLength;
    @Schema(description = "Total overdue amount")
    public int total;
    @Schema(description = "Number of overdue reminders sent")
    public int sent;
}
