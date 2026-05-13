package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle adjusted totals for a transaction")
public class AdjustedTotals {
    @Schema(description = "Subtotal amount before tax")
    private String subtotal;

    @Schema(description = "Tax amount")
    private String tax;

    @Schema(description = "Total amount including tax")
    private String total;

    @Schema(description = "Grand total after adjustments")

    private String grandTotal;

    @Schema(description = "Processing fee")
    private String fee;

    @Schema(description = "Earnings after fees")
    private String earnings;

    @Schema(description = "Three-letter currency code (e.g., USD)")

    private String currencyCode;

    @Schema(description = "Retained fee amount")

    private String retainedFee;
}
