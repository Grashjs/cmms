package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle unit price amount and currency")
public class UnitPrice {
    @Schema(description = "Price amount in minor currency units (e.g., cents)")
    private String amount;

    @Schema(description = "Three-letter currency code (e.g., USD)")
    @JsonProperty("currency_code")
    private String currencyCode;
}
