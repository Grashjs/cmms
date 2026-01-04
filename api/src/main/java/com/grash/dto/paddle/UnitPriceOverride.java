package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UnitPriceOverride {
    @JsonProperty("country_codes")
    private List<String> countryCodes;

    @JsonProperty("unit_price")
    private UnitPrice unitPrice;
}
