package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Paddle unit price override for specific countries")
public class UnitPriceOverride {
    @Schema(description = "List of country codes this override applies to")
    @JsonProperty("country_codes")
    private List<String> countryCodes;

    @Schema(description = "Overridden unit price for these countries")
    @JsonProperty("unit_price")
    private UnitPrice unitPrice;
}
