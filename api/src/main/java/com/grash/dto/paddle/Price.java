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

    private String productId;

    @Schema(description = "Billing cycle configuration")

    private BillingCycle billingCycle;

    @Schema(description = "Trial period configuration")

    private TrialPeriod trialPeriod;

    @Schema(description = "Tax mode (e.g., account_setting, external)")

    private String taxMode;

    @Schema(description = "Unit price amount and currency")

    private UnitPrice unitPrice;

    @Schema(description = "Unit price overrides for specific countries")

    private List<UnitPriceOverride> unitPriceOverrides;

    @Schema(description = "Price status (e.g., active, archived)")
    private String status;

    @Schema(description = "Quantity constraints")
    private Quantity quantity;

    @Schema(description = "Custom key-value data")

    private Map<String, String> customData;

    @Schema(description = "Price creation timestamp (ISO 8601)")

    private String createdAt;

    @Schema(description = "Price last updated timestamp (ISO 8601)")

    private String updatedAt;
}
