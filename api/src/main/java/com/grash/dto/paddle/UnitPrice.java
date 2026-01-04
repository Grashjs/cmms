package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UnitPrice {
    private String amount;

    @JsonProperty("currency_code")
    private String currencyCode;
}
