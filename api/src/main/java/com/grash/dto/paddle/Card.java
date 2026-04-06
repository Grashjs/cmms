package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle card payment details")
public class Card {
    @Schema(description = "Card type (e.g., visa, mastercard)")
    private String type;

    @Schema(description = "Last four digits of the card number")
    @JsonProperty("last4")
    private String last4;

    @Schema(description = "Card expiry month")
    @JsonProperty("expiry_month")
    private Integer expiryMonth;

    @Schema(description = "Card expiry year")
    @JsonProperty("expiry_year")
    private Integer expiryYear;

    @Schema(description = "Name of the cardholder")
    @JsonProperty("cardholder_name")
    private String cardholderName;
}
