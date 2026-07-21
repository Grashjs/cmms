package com.grash.factory;

import com.grash.model.SubscriptionPlan;

import java.util.HashSet;

public final class SubscriptionPlanFactory {

    private SubscriptionPlanFactory() {
    }

    public static SubscriptionPlan createSubscriptionPlan() {
        return SubscriptionPlan.builder()
                .name("Test Plan")
                .monthlyCostPerUser(10.0)
                .yearlyCostPerUser(100.0)
                .code("TEST")
                .features(new HashSet<>())
                .build();
    }
}
