// CheckoutResponse.java
package com.grash.dto.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response containing checkout session information")
public class CheckoutResponse {
    @Schema(description = "Stripe checkout session ID")
    private String sessionId;
}