package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LineItem {
    private String id;

    @JsonProperty("price_id")
    private String priceId;

    private Integer quantity;

    private Totals totals;

    private Product product;

    @JsonProperty("tax_rate")
    private String taxRate;

    @JsonProperty("unit_totals")
    private Totals unitTotals;
}
