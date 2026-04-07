package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Paddle price object")
public class Price {
    @Schema(description = "Price ID")
    private String id;

    @Schema(description = "Price name")
    private String name;

    @Schema(description = "Price type (e.g., standard, custom)")
    private String type;

    @Schema(description = "Price description")
    private String description;

    @Schema(description = "Associated product ID")
    @JsonProperty("product_id")
    private String productId;

    @Schema(description = "Billing cycle configuration")
    @JsonProperty("billing_cycle")
    private BillingCycle billingCycle;

    @Schema(description = "Trial period configuration")
    @JsonProperty("trial_period")
    private TrialPeriod trialPeriod;

    @Schema(description = "Tax mode (e.g., account_setting, external)")
    @JsonProperty("tax_mode")
    private String taxMode;

    @Schema(description = "Unit price amount and currency")
    @JsonProperty("unit_price")
    private UnitPrice unitPrice;

    @Schema(description = "Unit price overrides for specific countries")
    @JsonProperty("unit_price_overrides")
    private List<UnitPriceOverride> unitPriceOverrides;

    @Schema(description = "Price status (e.g., active, archived)")
    private String status;

    @Schema(description = "Quantity constraints")
    private Quantity quantity;

    @Schema(description = "Custom key-value data")
    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @Schema(description = "Price creation timestamp (ISO 8601)")
    @JsonProperty("created_at")
    private String createdAt;

    @Schema(description = "Price last updated timestamp (ISO 8601)")
    @JsonProperty("updated_at")
    private String updatedAt;
}
