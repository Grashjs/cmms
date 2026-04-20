package com.grash.dto;

import com.grash.model.enums.PlanFeatures;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing subscription plan")
public class SubscriptionPlanPatchDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Monthly cost per user")
    private double monthlyCostPerUser;

    @Schema(description = "Yearly cost per user")
    private double yearlyCostPerUser;

    @Schema(description = "Plan code")
    private String code;

    @Schema(description = "List of features included in the plan")
    private Collection<PlanFeatures> features;

}
