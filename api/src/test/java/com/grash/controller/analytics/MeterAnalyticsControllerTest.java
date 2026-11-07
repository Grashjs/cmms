package com.grash.controller.analytics;

import com.grash.controller.AbstractControllerTest;
import com.grash.dto.analytics.meters.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.service.MeterAnalyticsService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AnalyticsCacheKeyConfig.class)
@WebMvcTest(MeterAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeterAnalyticsControllerTest extends AbstractControllerTest {

    private static final String RANGE = "{\"start\":\"2026-10-01T03:00:00.000Z\",\"end\":\"2026-10-04T02:59:59.999Z\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterAnalyticsService meterAnalyticsService;
    @MockitoBean
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .features(new HashSet<>(Set.of(PlanFeatures.ANALYTICS)))
                .build();
        Company company = new Company();
        company.setId(1L);
        company.setSubscription(Subscription.builder().subscriptionPlan(plan).build());

        user = new User();
        user.setId(1L);
        user.setCompany(company);
        user.setEnabled(true);
        user.setUserSettings(new UserSettings());
        user.setSuperAccountRelations(new ArrayList<>());
        user.setRole(Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Admin")
                .viewPermissions(new HashSet<>(Set.of(PermissionEntity.ANALYTICS)))
                .build());

        setCurrentUser(user);
        when(userService.whoami(any())).thenReturn(user);
    }

    @Test
    void meters_ofTheUsersCompany() throws Exception {
        when(meterAnalyticsService.getMeters(1L)).thenReturn(List.of(MeterOption.builder()
                .id(9L).name("Hour meter").unit("h").cumulative(true).assetName("Compressor 1").build()));

        mockMvc.perform(get("/analytics/meters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].cumulative").value(true))
                .andExpect(jsonPath("$[0].assetName").value("Compressor 1"));
    }

    @Test
    void analytics_ofTheMeterOverThePeriod() throws Exception {
        when(meterAnalyticsService.getAnalytics(eq(1L), eq(9L), any(), any())).thenReturn(MeterAnalytics.builder()
                .meterId(9L)
                .interval(MeterInterval.DAY)
                .points(List.of(MeterAnalyticsPoint.builder().count(2).average(7.0).consumption(18.0).build()))
                .thresholds(List.of(MeterThreshold.builder().name("High")
                        .condition(WorkOrderMeterTriggerCondition.MORE_THAN).value(8).build()))
                .readings(2)
                .consumption(18.0)
                .build());

        mockMvc.perform(post("/analytics/meters/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value("DAY"))
                .andExpect(jsonPath("$.points[0].average").value(7.0))
                .andExpect(jsonPath("$.thresholds[0].condition").value("MORE_THAN"))
                .andExpect(jsonPath("$.consumption").value(18.0));

        verify(meterAnalyticsService).getAnalytics(1L, 9L, Date.from(Instant.parse("2026-10-01T03:00:00Z")),
                Date.from(Instant.parse("2026-10-04T02:59:59.999Z")));
    }

    @Test
    void withoutAnalyticsPermission_isForbidden() throws Exception {
        user.getRole().setViewPermissions(new HashSet<>());

        mockMvc.perform(get("/analytics/meters"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/analytics/meters/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RANGE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(meterAnalyticsService);
    }

    @Test
    void otherCompanyWithoutAccess_isForbidden() throws Exception {
        mockMvc.perform(get("/analytics/meters").param("companyId", "2"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(meterAnalyticsService);
    }

    @Test
    void meterOfAnotherCompany_isNotFound() throws Exception {
        when(meterAnalyticsService.getAnalytics(any(), any(), any(), any()))
                .thenThrow(new CustomException("Meter not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/analytics/meters/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RANGE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
