package com.grash.controller.analytics;

import com.grash.controller.AbstractControllerTest;
import com.grash.dto.DateRange;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.AnalyticsSummaryService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.grash.utils.Helper.setCurrentUser;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsSummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsSummaryControllerTest extends AbstractControllerTest {

    private static final String BODY = "{\"start\":\"2026-03-01T03:00:00.000Z\",\"end\":\"2026-04-01T02:59:59" +
            ".999Z\",\"filterFields\":[{\"field\":\"team\",\"operation\":\"in\",\"value\":\"\",\"values\":[7]}]}";
    private static final byte[] PDF = "%PDF-1.7 summary".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsSummaryService analyticsSummaryService;
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
    void pdf_isDownloadedAsAnAttachment() throws Exception {
        when(analyticsSummaryService.renderPdf(eq(user), eq(1L), any())).thenReturn(PDF);

        mockMvc.perform(post("/analytics/summary/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("maintenance-summary-")))
                .andExpect(content().bytes(PDF));

        verify(analyticsSummaryService).renderPdf(eq(user), eq(1L), argThat((DateRange range) ->
                range.getStart() != null && "team".equals(range.getFilterFields().get(0).getField())));
    }

    @Test
    void withoutAnalyticsPermission_isForbidden() throws Exception {
        user.getRole().setViewPermissions(new HashSet<>());

        mockMvc.perform(post("/analytics/summary/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(analyticsSummaryService);
    }

    @Test
    void otherCompanyWithoutAccess_isForbidden() throws Exception {
        mockMvc.perform(post("/analytics/summary/pdf")
                        .param("companyId", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(analyticsSummaryService);
    }

    @Test
    void invalidRange_isAJsonError() throws Exception {
        when(analyticsSummaryService.renderPdf(any(), any(), any()))
                .thenThrow(new CustomException("A valid start and end are required", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/analytics/summary/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
                        .content("{\"filterFields\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }
}
