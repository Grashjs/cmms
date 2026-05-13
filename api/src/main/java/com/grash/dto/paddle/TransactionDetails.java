package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Paddle transaction details with totals and line items")
public class TransactionDetails {
    @Schema(description = "Transaction totals")
    private Totals totals;

    @Schema(description = "Adjusted totals after corrections")
    @JsonProperty("adjusted_totals")
    private AdjustedTotals adjustedTotals;

    @Schema(description = "Payout totals")
    @JsonProperty("payout_totals")
    private PayoutTotals payoutTotals;

    @Schema(description = "Line items in the transaction")
    @JsonProperty("line_items")
    private List<LineItem> lineItems;

    @Schema(description = "Tax rates applied in the transaction")
    @JsonProperty("tax_rates_used")
    private List<TaxRateUsed> taxRatesUsed;
}
