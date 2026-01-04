package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TransactionDetails {
    private Totals totals;

    @JsonProperty("adjusted_totals")
    private AdjustedTotals adjustedTotals;

    @JsonProperty("payout_totals")
    private PayoutTotals payoutTotals;

    @JsonProperty("line_items")
    private List<LineItem> lineItems;

    @JsonProperty("tax_rates_used")
    private List<TaxRateUsed> taxRatesUsed;
}
