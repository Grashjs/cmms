// CheckoutRequest.java
package com.grash.dto.stripe;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String planId;
    private String planName;
    private Double price;
    private String email; // Optional: pre-fill customer email
}