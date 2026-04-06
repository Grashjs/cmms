package com.grash.model;

import com.grash.model.enums.PlanFeatures;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subscription plan defining pricing and features")
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Plan name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = "Monthly cost per user", requiredMode = Schema.RequiredMode.REQUIRED)
    private double monthlyCostPerUser;

    @NotNull
    @Schema(description = "Yearly cost per user", requiredMode = Schema.RequiredMode.REQUIRED)
    private double yearlyCostPerUser;

    @Schema(description = "Plan code identifier")
    private String code;

    @ElementCollection(targetClass = PlanFeatures.class)
    @Schema(description = "Set of features included in this plan")
    private Set<PlanFeatures> features = new HashSet<>();

    @Schema(description = "Monthly Paddle price ID")
    private String monthlyPaddlePriceId;
    @Schema(description = "Yearly Paddle price ID")
    private String yearlyPaddlePriceId;

}


