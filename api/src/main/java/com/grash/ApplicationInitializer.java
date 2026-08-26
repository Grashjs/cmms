package com.grash;

import com.grash.dto.UserSignupRequest;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.GeneralPreferencesRepository;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
    private final UserInvitationService userInvitationService;
    private final GeneralPreferencesRepository generalPreferencesRepository;
    @Value("${self-hosted.unlock-premium:false}")
    private boolean unlockPremium;
    private final RoleService roleService;
    private final CompanyService companyService;
    private final SubscriptionPlanService subscriptionPlanService;

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

            userService.checkUsageBasedLimit(0);

            generalPreferencesRepository.updateTemporaryTimeZones(ZoneId.systemDefault().getId());

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
                    return roleService.create(Role.builder()
                            .name("Super admin")
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

        if (unlockPremium) {
            ensureFreePlanUnlocked();
        }
    }

    /**
     * Self-hosted unlock: every company defaults to the FREE plan, so granting FREE all
     * PlanFeatures unlocks every plan-gated premium feature (API_ACCESS, WORKFLOW,
     * WEBHOOK, IMPORT_CSV, ...) instance-wide. Idempotent on every boot. Note: this
     * mutates persisted plan data, so turning the flag back off does not re-lock FREE —
     * reset its features manually if you need upstream FREE behaviour again.
     */
    private void ensureFreePlanUnlocked() {
        subscriptionPlanService.findByCode("FREE").ifPresent(freePlan -> {
            Set<PlanFeatures> allFeatures = new HashSet<>(Arrays.asList(PlanFeatures.values()));
            if (!allFeatures.equals(freePlan.getFeatures())) {
                freePlan.setFeatures(allFeatures);
                subscriptionPlanService.create(freePlan);
                log.info("Self-hosted unlock: FREE plan granted all {} premium features", allFeatures.size());
            }
        });
    }


    /**
     * Upstream hardcodes "pls_change_me" here. That password is in the public upstream
     * source, /auth/signin is not covered by the nginx block, and nothing in the code
     * forces it to be changed - so every instance started against a fresh database was
     * reachable by anyone who had read the repository.
     * <p>
     * The password is generated instead, and logged once at WARN. Logging it is
     * deliberate: without it a fresh instance has no way in at all. It also means the
     * credential lives only in the boot log of the very first start, not in the source.
     * Sign in once, change it, then disable the account - and do not delete it, see
     * CLAUDE.md, "Known upstream issue: the default super admin".
     */
    @NotNull
    private static UserSignupRequest getSuperAdminSignupRequest(Role savedSuperAdminRole) {
        String initialPassword = UUID.randomUUID().toString();
        log.warn("Created the default super admin with a generated password: {} - sign in " +
                "once, change it, then disable the account. It is shown only here, and only " +
                "on the boot that created it.", initialPassword);

        UserSignupRequest signupRequest = new UserSignupRequest();
        signupRequest.setRole(savedSuperAdminRole);
        signupRequest.setEmail("superadmin@test.com");
        signupRequest.setPassword(initialPassword);
        signupRequest.setFirstName("Super");
        signupRequest.setLastName("Admin");
        signupRequest.setPhone("");
        signupRequest.setCompanyName("Super Admin");
        signupRequest.setEmployeesCount(3);
        signupRequest.setLanguage(Language.EN);
        return signupRequest;
    }
}
