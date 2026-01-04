package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Totals {
    private String subtotal;
    private String discount;
    private String tax;
    private String total;
    private String credit;
    private String balance;

    @JsonProperty("grand_total")
    private String grandTotal;

    private String fee;
    private String earnings;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("credit_to_balance")
    private String creditToBalance;
}
