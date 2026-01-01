package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Price {
    private String id;

    private String name;

    private String type;

    private String description;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("billing_cycle")
    private BillingCycle billingCycle;

    @JsonProperty("trial_period")
    private TrialPeriod trialPeriod;

    @JsonProperty("tax_mode")
    private String taxMode;

    @JsonProperty("unit_price")
    private UnitPrice unitPrice;

    @JsonProperty("unit_price_overrides")
    private List<UnitPriceOverride> unitPriceOverrides;

    private String status;

    private Quantity quantity;

    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
