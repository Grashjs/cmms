// CheckoutResponse.java
package com.grash.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private String sessionId;
    private String url;
}