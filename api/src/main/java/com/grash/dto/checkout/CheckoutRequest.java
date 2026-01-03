// CheckoutRequest.java
package com.grash.dto.checkout;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CheckoutRequest {
    @NotNull
    private String planId;
    private String email;
    private Long userId;
    private Integer quantity;
}