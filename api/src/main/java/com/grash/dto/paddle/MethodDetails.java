package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle payment method details")
public class MethodDetails {
    @Schema(description = "Payment method type")
    private String type;

    @Schema(description = "Card payment details")
    private Card card;

    @Schema(description = "South Korea local card details")
    @JsonProperty("south_korea_local_card")
    private Object southKoreaLocalCard;
}
