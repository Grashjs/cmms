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
    @JsonProperty("grand_total")
    private String grandTotal;

    @Schema(description = "Processing fee")
    private String fee;

    @Schema(description = "Earnings after fees")
    private String earnings;

    @Schema(description = "Three-letter currency code (e.g., USD)")
    @JsonProperty("currency_code")
    private String currencyCode;

    @Schema(description = "Retained fee amount")
    @JsonProperty("retained_fee")
    private String retainedFee;
}
