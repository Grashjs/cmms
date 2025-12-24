package com.grash.dto.stripe;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripeWebhookData {
    @JsonProperty("object")
    private StripeCustomer customer;
}
