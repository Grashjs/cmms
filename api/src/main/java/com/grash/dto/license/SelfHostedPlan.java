package com.grash.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Self-hosted subscription plan configuration with pricing and license policy")
public class SelfHostedPlan {
    @Schema(description = "Unique identifier for the plan")
    private String id;
    @Schema(description = "Paddle price ID for subscription billing")
    private String paddlePriceId;
    @Schema(description = "Display name of the subscription plan")
    private String name;
    @Schema(description = "Whether this is a monthly billing plan")
    private boolean monthly;
    @Schema(description = "Keygen policy ID associated with this plan for license validation")
    private String keygenPolicyId;
}