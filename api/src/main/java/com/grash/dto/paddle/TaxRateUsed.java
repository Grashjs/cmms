package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaxRateUsed {
    @JsonProperty("tax_rate")
    private String taxRate;

    private Totals totals;
}
