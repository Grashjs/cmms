package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle transaction item")
public class TransactionItem {
    @Schema(description = "Price ID for the item")
    @JsonProperty("price_id")
    private String priceId;

    @Schema(description = "Price details")
    private Price price;

    @Schema(description = "Item quantity")
    private Integer quantity;

    @Schema(description = "Proration type (e.g., full, partial)")
    private String proration;
}
