package com.grash.utils;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.SelfHostedPlan;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Consts {
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final List<SelfHostedPlan> selfHostedPlans = Arrays.asList(
            SelfHostedPlan.builder()
                    .id("sh-professional-monthly")
                    .paddlePriceId("pri_01kdtyz3bx2s73wz2ffxr7k34r")
                    .name("Professional Atlas CMMS license")
                    .monthly(true)
                    .keygenPolicyId("5df4c975-8933-4c9f-89e0-2207365699a9")
                    .build(),
            SelfHostedPlan.builder()
                    .id("sh-professional-yearly")
                    .paddlePriceId("pri_01kdtz14k551qkpbqe6f6tsc4k")
                    .name("Professional Atlas CMMS license")
                    .monthly(false)
                    .keygenPolicyId("c168a294-7f62-47bc-a010-a26e8758b00c")
                    .build(),
            SelfHostedPlan.builder()
                    .id("sh-business-monthly")
                    .paddlePriceId("pri_01kdtz71r9xpppbnhmftxpz2ys")
                    .keygenPolicyId("7d057a02-02f9-474f-839e-5a84713ada36")
                    .monthly(true)
                    .name("Business Atlas CMMS license")
                    .build(),
            SelfHostedPlan.builder()
                    .id("sh-business-yearly")
                    .paddlePriceId("pri_01kdtz8pexz7s2s2desb82ygcq")
                    .keygenPolicyId("2c02b037-d9d2-4f3f-b305-d4845981a63c")
                    .monthly(false)
                    .name("Business Atlas CMMS license")
                    .build()
    );

    public static final Map<LicenseEntitlement, Integer> usageBasedLicenseLimits =
            new HashMap<>() {
                {
                    put(LicenseEntitlement.UNLIMITED_CHECKLISTS, 10);
                    put(LicenseEntitlement.UNLIMITED_ASSETS, 50);
                    put(LicenseEntitlement.UNLIMITED_PARTS, 100);
                    put(LicenseEntitlement.UNLIMITED_LOCATIONS, 10);
                    put(LicenseEntitlement.UNLIMITED_PM_SCHEDULES, 10);
                    put(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS, 30);
                    put(LicenseEntitlement.UNLIMITED_METERS, 10);
                    put(LicenseEntitlement.UNLIMITED_USERS, 5);
                }
            };
}
