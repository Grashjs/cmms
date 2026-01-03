package com.grash.utils;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.SelfHostedPlan;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO use yaml
public class Consts {
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final List<SelfHostedPlan> selfHostedPlans = Arrays.asList(
            SelfHostedPlan.builder()
                    .id("sh-professional-monthly")
                    .paddlePriceId("pri_01kdv3d9rjmp9bjqw6dw70hpc4")
                    .name("Professional Atlas CMMS license")
                    .monthly(true)
                    .keygenPolicyId("5df4c975-8933-4c9f-89e0-2207365699a9")
                    .build(),
            SelfHostedPlan.builder()
                    .id("sh-business-monthly")
                    .paddlePriceId("pri_01kdv3fnrk04dcqkcey9h654x0")
                    .keygenPolicyId("c168a294-7f62-47bc-a010-a26e8758b00c")
                    .monthly(true)
                    .name("Business Atlas CMMS license")
                    .build()
    );

    public static final Map<LicenseEntitlement, Integer> usageBasedLicenseLimits =
            new HashMap<LicenseEntitlement, Integer>() {
                {
                    put(LicenseEntitlement.UNLIMITED_CHECKLIST, 10);
                    put(LicenseEntitlement.UNLIMITED_ASSETS, 100);
                    put(LicenseEntitlement.UNLIMITED_PARTS, 100);
                    put(LicenseEntitlement.UNLIMITED_LOCATIONS, 40);
                    put(LicenseEntitlement.UNLIMITED_PM_SCHEDULES, 20);
                    put(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS, 30);
                }
            };
}
