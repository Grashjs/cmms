package com.grash.factory;

import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;

import java.util.Date;

public final class SubscriptionFactory {

    private SubscriptionFactory() {
    }

    public static Subscription createSubscription(SubscriptionPlan plan) {
        return Subscription.builder()
                .usersCount(10)
                .monthly(true)
                .activated(true)
                .subscriptionPlan(plan)
                .startsOn(new Date())
                .build();
    }
}
