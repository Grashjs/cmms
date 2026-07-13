package com.grash.model;

import com.grash.model.enums.PlanFeatures;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private double monthlyCostPerUser;

    @NotNull
    private double yearlyCostPerUser;

    private String code;

    @ElementCollection(targetClass = PlanFeatures.class)
    private Set<PlanFeatures> features = new HashSet<>();

}
