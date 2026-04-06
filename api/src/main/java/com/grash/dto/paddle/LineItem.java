package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle line item in a transaction")
public class LineItem {
    @Schema(description = "Line item ID")
    private String id;

    @Schema(description = "Price ID associated with the line item")
    @JsonProperty("price_id")
    private String priceId;

    @Schema(description = "Quantity of the item")
    private Integer quantity;

    @Schema(description = "Totals for this line item")
    private Totals totals;

    @Schema(description = "Product details")
    private Product product;

    @Schema(description = "Tax rate applied")
    @JsonProperty("tax_rate")
    private String taxRate;

    @Schema(description = "Unit totals for this line item")
    @JsonProperty("unit_totals")
    private Totals unitTotals;
}
