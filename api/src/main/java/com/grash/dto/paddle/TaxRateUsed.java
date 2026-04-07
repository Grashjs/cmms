package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle tax rate and totals used in a transaction")
public class TaxRateUsed {
    @Schema(description = "Tax rate applied")
    @JsonProperty("tax_rate")
    private String taxRate;

    @Schema(description = "Totals for this tax rate")
    private Totals totals;
}
