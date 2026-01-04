package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Card {
    private String type;

    @JsonProperty("last4")
    private String last4;

    @JsonProperty("expiry_month")
    private Integer expiryMonth;

    @JsonProperty("expiry_year")
    private Integer expiryYear;

    @JsonProperty("cardholder_name")
    private String cardholderName;
}
