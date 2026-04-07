// CheckoutRequest.java
package com.grash.dto.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "Request for initiating a checkout session")
public class CheckoutRequest {
    @Schema(description = "Subscription plan ID (e.g., professional-monthly)")
    @NotNull
    private String planId;
    
    @Schema(description = "User email")
    private String email;
    
    @Schema(description = "User ID")
    private Long userId;
    
    @Schema(description = "Quantity of licenses/seats")
    private Integer quantity;
}
