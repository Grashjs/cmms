package com.grash.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripeWebhookRequest {
    private String id;
    private String type;
    private StripeWebhookData data;
}

