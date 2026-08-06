package com.grash;

import com.grash.dto.UserSignupRequest;
import com.grash.model.SubscriptionPlan;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.GeneralPreferencesRepository;
import com.grash.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// AGPLv3 fork contract tests for the idempotent PlanFeatures upsert.
@ExtendWith(MockitoExtension.class)
class ApplicationInitializerTest {

    @InjectMocks
    private ApplicationInitializer applicationInitializer;

    @Mock
    private UserService userService;
    @Mock
    private UserInvitationService userInvitationService;
    @Mock
    private GeneralPreferencesRepository generalPreferencesRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private CompanyService companyService;
    @Mock
    private SubscriptionPlanService subscriptionPlanService;
    @Mock
    private SubscriptionService subscriptionService;

    private SubscriptionPlan plan(String code, Set<PlanFeatures> features) {
        SubscriptionPlan p = new SubscriptionPlan();
        p.setId(code.hashCode() + 0L);
        p.setCode(code);
        p.setName(code);
        p.setFeatures(features != null ? new HashSet<>(features) : new HashSet<>());
        return p;
    }

    @Test
    void applyAgplPlanFeatures_incompletePlans_getAllFeaturesAndSaved() {
        Set<PlanFeatures> partial = new HashSet<>(Arrays.asList(PlanFeatures.FILE, PlanFeatures.METER));
        SubscriptionPlan free = plan("FREE", partial);
        SubscriptionPlan starter = plan("STARTER", partial);
        when(subscriptionPlanService.getAll()).thenReturn(List.of(free, starter));
        when(subscriptionPlanService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationInitializer.applyAgplPlanFeatures();

        Set<PlanFeatures> all = new HashSet<>(Arrays.asList(PlanFeatures.values()));
        assertEquals(all, free.getFeatures());
        assertEquals(all, starter.getFeatures());
        verify(subscriptionPlanService).create(free);
        verify(subscriptionPlanService).create(starter);
    }

    @Test
    void applyAgplPlanFeatures_alreadyComplete_noWrite() {
        Set<PlanFeatures> all = new HashSet<>(Arrays.asList(PlanFeatures.values()));
        SubscriptionPlan business = plan("BUSINESS", all);
        when(subscriptionPlanService.getAll()).thenReturn(List.of(business));

        applicationInitializer.applyAgplPlanFeatures();

        verify(subscriptionPlanService, never()).create(any());
    }

    @Test
    void applyAgplPlanFeatures_nullFeatures_filledAndSaved() {
        SubscriptionPlan p = plan("UPSTREAM_NEW", null);
        when(subscriptionPlanService.getAll()).thenReturn(List.of(p));
        when(subscriptionPlanService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationInitializer.applyAgplPlanFeatures();

        assertEquals(new HashSet<>(Arrays.asList(PlanFeatures.values())), p.getFeatures());
        verify(subscriptionPlanService).create(p);
    }

    @Test
    void applyAgplPlanFeatures_secondRunAfterConvergence_idempotent() {
        Set<PlanFeatures> partial = new HashSet<>(List.of(PlanFeatures.FILE));
        SubscriptionPlan p = plan("FREE", partial);
        when(subscriptionPlanService.getAll()).thenReturn(List.of(p));
        when(subscriptionPlanService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationInitializer.applyAgplPlanFeatures();
        Set<PlanFeatures> expected = new HashSet<>(p.getFeatures());

        // Second pass: features already match -> no save
        clearInvocations(subscriptionPlanService);
        when(subscriptionPlanService.getAll()).thenReturn(List.of(p));

        applicationInitializer.applyAgplPlanFeatures();

        assertEquals(expected, p.getFeatures());
        verify(subscriptionPlanService, never()).create(any());
    }
}