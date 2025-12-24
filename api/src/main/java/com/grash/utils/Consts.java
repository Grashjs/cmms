package com.grash.utils;

import com.grash.dto.license.SelfHostedPlan;
import com.grash.service.StripeService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

//TODO use yaml
public class Consts {
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String FRONT_TUTORIAL_LINK = "#";
    public static final int WISHES_LIMIT = 7;
    public static List<SelfHostedPlan> selfHostedPlans = Arrays.asList(
            SelfHostedPlan.builder()
                    .id("sh-professional-monthly")
                    .pricePerUser(BigDecimal.valueOf(15))
                    .name("Professional Atlas CMMS license")
                    .monthly(true)
                    .keygenPolicyId("5df4c975-8933-4c9f-89e0-2207365699a9")
                    .build(),
            SelfHostedPlan.builder()
                    .id("sh-enterprise-monthly")
                    .pricePerUser(BigDecimal.valueOf(100))
                    .keygenPolicyId("c168a294-7f62-47bc-a010-a26e8758b00c")
                    .monthly(true)
                    .name("Enterprise Atlas CMMS license")
                    .build()
    );
}
