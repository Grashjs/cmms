package com.grash;

import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.GeneralPreferencesRepository;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitializer implements SmartInitializingSingleton {

    private final UserService userService;
    private final InternalUserBootstrapService internalUserBootstrapService;
    private final GeneralPreferencesRepository generalPreferencesRepository;
    private final RoleService roleService;
    private final CompanyService companyService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final ScheduleService scheduleService;

    @Value("${cloud-version}")
    private boolean cloudVersion;

    @Override
    public void afterSingletonsInstantiated() {
        log.info("Starting application initialization...");

        try {
            log.info("Initializing super admin...");
            initializeSuperAdmin();

            log.info("Initializing subscription plans...");
            initializeSubscriptionPlans();

            log.info("Updating default roles...");
            roleService.updateDefaultRoles();

//            if (!cloudVersion) scheduleService.findActive().forEach(scheduleService::reScheduleWorkOrder);

            userService.checkUsageBasedLimit(0);


            log.info("Application initialization completed successfully");
        } catch (Exception e) {
            log.error("Application initialization failed", e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    private void initializeSuperAdmin() {
        // Find or create the super admin role
        Role savedSuperAdminRole = roleService.findByCodeAndRoleType(RoleCode.ADMIN, RoleType.ROLE_SUPER_ADMIN)
                .stream().findFirst().orElseGet(() -> {
                    log.info("Creating super admin role...");
                    Company company = companyService.create(new Company());
                    return internalUserBootstrapService.createPlatformRole(Role.builder()
                            .name("Super admin")
                            .companySettings(company.getCompanySettings())
                            .code(RoleCode.ADMIN)
                            .roleType(RoleType.ROLE_SUPER_ADMIN)
                            .build());
                });

        if (userService.findByCompany(savedSuperAdminRole.getCompanySettings().getCompany().getId()).isEmpty()) {
            log.info("Creating super admin user...");
            internalUserBootstrapService.createSuperAdministrator(savedSuperAdminRole);
        } else {
            log.info("Super admin user already exists");
        }
    }

    private void initializeSubscriptionPlans() {
        if (!subscriptionPlanService.existByCode("FREE")) {
            log.info("Creating FREE subscription plan...");
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("FREE")
                    .name("Free")
                    .monthlyCostPerUser(0)
                    .yearlyCostPerUser(0).build());
        }

        if (!subscriptionPlanService.existByCode("STARTER")) {
            log.info("Creating STARTER subscription plan...");
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("STARTER")
                    .name("Starter")
                    .features(new HashSet<>(Arrays.asList(
                            PlanFeatures.PREVENTIVE_MAINTENANCE,
                            PlanFeatures.CHECKLIST,
                            PlanFeatures.FILE,
                            PlanFeatures.METER,
                            PlanFeatures.ADDITIONAL_COST,
                            PlanFeatures.ADDITIONAL_TIME)))
                    .monthlyCostPerUser(10)
                    .yearlyCostPerUser(100).build());
        }

        if (!subscriptionPlanService.existByCode("PROFESSIONAL")) {
            log.info("Creating PROFESSIONAL subscription plan...");
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("PROFESSIONAL")
                    .name("Professional")
                    .monthlyCostPerUser(15)
                    .features(new HashSet<>(Arrays.asList(
                            PlanFeatures.PREVENTIVE_MAINTENANCE,
                            PlanFeatures.CHECKLIST,
                            PlanFeatures.FILE,
                            PlanFeatures.METER,
                            PlanFeatures.ADDITIONAL_COST,
                            PlanFeatures.ADDITIONAL_TIME,
                            PlanFeatures.REQUEST_CONFIGURATION,
                            PlanFeatures.SIGNATURE,
                            PlanFeatures.ANALYTICS,
                            PlanFeatures.IMPORT_CSV,
                            PlanFeatures.REQUEST_PORTAL
                    )))
                    .yearlyCostPerUser(150).build());
        }

        if (!subscriptionPlanService.existByCode("BUSINESS")) {
            log.info("Creating BUSINESS subscription plan...");
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("BUSINESS")
                    .name("Business")
                    .monthlyCostPerUser(40)
                    .features(new HashSet<>(Arrays.asList(PlanFeatures.values())))
                    .yearlyCostPerUser(400).build());
        }
    }


}
