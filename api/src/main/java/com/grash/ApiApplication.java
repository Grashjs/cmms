package com.grash;

import com.grash.dto.UserSignupRequest;
import com.grash.dto.license.LicensingState;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.UserRepository;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.*;

@SpringBootApplication
@RequiredArgsConstructor
@EnableCaching
@Slf4j
public class ApiApplication implements SmartInitializingSingleton {

    private final UserService userService;
    private final UserInvitationService userInvitationService;
    private final UserRepository userRepository;
    @Value("${superAdmin.role.name}")
    private String superAdminRole;
    private final RoleService roleService;
    private final CompanyService companyService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionService subscriptionService;
    private final ScheduleService scheduleService;
    private final LicenseService licenseService;

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

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

            checkLicenseUsersCount();

            log.info("Application initialization completed successfully");
        } catch (Exception e) {
            log.error("Application initialization failed", e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    private void initializeSuperAdmin() {
        // Find or create the super admin role
        Role savedSuperAdminRole = roleService.findByName(superAdminRole)
                .orElseGet(() -> {
                    log.info("Creating super admin role...");
                    Company company = companyService.create(new Company());
                    return roleService.create(Role.builder()
                            .name(superAdminRole)
                            .companySettings(company.getCompanySettings())
                            .code(RoleCode.ADMIN)
                            .roleType(RoleType.ROLE_SUPER_ADMIN)
                            .build());
                });

        if (userService.findByCompany(savedSuperAdminRole.getCompanySettings().getCompany().getId()).isEmpty()) {
            log.info("Creating super admin user...");
            UserSignupRequest signupRequest = getSuperAdminSignupRequest(savedSuperAdminRole);
            userInvitationService.create(new UserInvitation(signupRequest.getEmail(), savedSuperAdminRole));
            userService.signup(signupRequest);
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
                            PlanFeatures.IMPORT_CSV
                    )))
                    .yearlyCostPerUser(150).build());
        }

        if (!subscriptionPlanService.existByCode("BUSINESS")) {
            log.info("Creating BUSINESS subscription plan...");
            subscriptionPlanService.create(SubscriptionPlan.builder()
                    .code("BUSINESS")
                    .name("Business")
                    .monthlyCostPerUser(80)
                    .features(new HashSet<>(Arrays.asList(PlanFeatures.values())))
                    .yearlyCostPerUser(800).build());
        }
    }


    private void checkLicenseUsersCount() {
        LicensingState licensingState = licenseService.getLicensingState();
        if (licensingState.isHasLicense()) {
            if (userRepository.hasMorePaidUsersThan(licensingState.getUsersCount()))
                throw new RuntimeException("Cannot create more users than the license allows: " + licensingState.getUsersCount());
        }
    }

    @NotNull
    private static UserSignupRequest getSuperAdminSignupRequest(Role savedSuperAdminRole) {
        UserSignupRequest signupRequest = new UserSignupRequest();
        signupRequest.setRole(savedSuperAdminRole);
        signupRequest.setEmail("superadmin@test.com");
        signupRequest.setPassword("pls_change_me");
        signupRequest.setFirstName("Super");
        signupRequest.setLastName("Admin");
        signupRequest.setPhone("");
        signupRequest.setCompanyName("Super Admin");
        signupRequest.setEmployeesCount(3);
        signupRequest.setLanguage(Language.EN);
        return signupRequest;
    }
}