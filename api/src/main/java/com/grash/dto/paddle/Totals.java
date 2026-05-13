package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle monetary totals for a transaction or line item")
public class Totals {
    @Schema(description = "Subtotal amount before tax and discounts")
    private String subtotal;

    @Schema(description = "Discount amount")
    private String discount;

    @Schema(description = "Tax amount")
    private String tax;

    @Schema(description = "Total amount")
    private String total;

    @Schema(description = "Credit amount")
    private String credit;

    @Schema(description = "Balance amount")
    private String balance;

    @Schema(description = "Grand total")

    private String grandTotal;

    @Schema(description = "Processing fee")
    private String fee;

    @Schema(description = "Earnings after fees")
    private String earnings;

    @Schema(description = "Three-letter currency code (e.g., USD)")

    private String currencyCode;

    @Schema(description = "Credit amount applied to balance")

    private String creditToBalance;
}
