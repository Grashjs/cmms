package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AdjustedTotals {
    private String subtotal;
    private String tax;
    private String total;

    @JsonProperty("grand_total")
    private String grandTotal;

    private String fee;
    private String earnings;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("retained_fee")
    private String retainedFee;
}
