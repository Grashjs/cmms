package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring payment method details")
public class Payment {
    @Schema(description = "Payment method type (e.g., creditcard, paypal)")
    public String type;
    @Schema(description = "Last digits of the credit card used")
    public String cardEnding;
}
