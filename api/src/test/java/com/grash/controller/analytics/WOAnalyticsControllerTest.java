package com.grash.controller.analytics;

import com.grash.controller.AbstractControllerTest;
import com.grash.dto.DateRange;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.WorkOrderAudRepository;
import com.grash.service.*;
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

import java.util.*;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AnalyticsCacheKeyConfig.class)
@WebMvcTest(WOAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class WOAnalyticsControllerTest extends AbstractControllerTest {

    private static final String TEAM_FILTER = "{\"filterFields\":[{\"field\":\"team\",\"operation\":\"in\"," +
            "\"value\":\"\",\"values\":[7]}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderService workOrderService;
    @MockitoBean
    private WorkOrderAudRepository workOrderAudRepository;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private LaborService laborService;
    @MockitoBean
    private WorkOrderCategoryService workOrderCategoryService;
    @MockitoBean
    private AssetService assetService;
    @MockitoBean
    private CompanyService companyService;
    @MockitoBean
    private AnalyticsFilterService analyticsFilterService;

    @BeforeEach
    void setUp() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .features(new HashSet<>(Set.of(PlanFeatures.ANALYTICS)))
                .build();
        Company company = new Company();
        company.setId(1L);
        company.setSubscription(Subscription.builder().subscriptionPlan(plan).build());

        User user = new User();
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
        when(analyticsFilterService.resolve(any(), any())).thenReturn(AnalyticsFilter.NONE);
    }

    @Test
    void completeOverview_countsOnlyFilteredWorkOrders() throws Exception {
        AnalyticsFilter highOnly = new AnalyticsFilter(Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(Priority.HIGH));
        when(analyticsFilterService.resolve(eq(1L), any())).thenReturn(highOnly);
        when(workOrderService.findByCompanyAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(complete(Priority.HIGH), complete(Priority.LOW), complete(Priority.HIGH)));

        mockMvc.perform(post("/analytics/work-orders/complete/overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\":\"2026-01-01T00:00:00.000Z\",\"end\":\"2026-02-01T00:00:00.000Z\"," +
                                "\"filterFields\":[{\"field\":\"priority\",\"operation\":\"in\",\"value\":\"\"," +
                                "\"values\":[\"HIGH\"],\"enumName\":\"PRIORITY\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.complete").value(2));

        verify(analyticsFilterService).resolve(eq(1L), argThat((DateRange range) ->
                range.getFilterFields().size() == 1 && "priority".equals(range.getFilterFields().get(0).getField())));
    }

    @Test
    void completeByWeek_get_stillWorksWithoutBody() throws Exception {
        mockMvc.perform(get("/analytics/work-orders/complete/counts/week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        verify(analyticsFilterService).resolve(eq(1L), isNull());
    }

    @Test
    void completeByWeek_post_passesTheFilterFields() throws Exception {
        mockMvc.perform(post("/analytics/work-orders/complete/counts/week")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEAM_FILTER))
                .andExpect(status().isOk());

        verify(analyticsFilterService).resolve(eq(1L), argThat((DateRange range) ->
                range.getStart() == null && "team".equals(range.getFilterFields().get(0).getField())));
    }

    @Test
    void completeTimeByWeek_post_passesTheFilterFields() throws Exception {
        mockMvc.perform(post("/analytics/work-orders/complete/time/week")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEAM_FILTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        verify(analyticsFilterService).resolve(eq(1L), argThat((DateRange range) ->
                "team".equals(range.getFilterFields().get(0).getField())));
    }

    @Test
    void invalidFilter_returnsBadRequest() throws Exception {
        when(analyticsFilterService.resolve(any(), any()))
                .thenThrow(new CustomException("Unsupported analytics filter: primaryUser", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/analytics/work-orders/complete/overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\":\"2026-01-01T00:00:00.000Z\",\"end\":\"2026-02-01T00:00:00.000Z\"," +
                                "\"filterFields\":[{\"field\":\"primaryUser\",\"operation\":\"in\",\"value\":\"\"," +
                                "\"values\":[1]}]}"))
                .andExpect(status().isBadRequest());
    }

    private static WorkOrder complete(Priority priority) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle("WO");
        workOrder.setStatus(Status.COMPLETE);
        workOrder.setPriority(priority);
        workOrder.setCreatedAt(new Date(0));
        workOrder.setCompletedOn(new Date(3_600_000L));
        return workOrder;
    }
}
