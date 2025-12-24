package com.grash.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripeCustomer {
    private String id;
    private String email;
    private Map<String, String> metadata;
}
