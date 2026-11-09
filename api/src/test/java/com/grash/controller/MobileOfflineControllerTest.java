package com.grash.controller;

import com.grash.dto.offline.*;
import com.grash.model.*;
import com.grash.model.enums.RoleType;
import com.grash.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MobileOfflineController.class)
@AutoConfigureMockMvc(addFilters = false)
class MobileOfflineControllerTest extends AbstractControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean MobileOfflineService offline;
    @MockitoBean UserService users;
    User user;

    @BeforeEach void setup() {
        Company company = new Company(); company.setId(52L);
        Role role = new Role(); role.setRoleType(RoleType.ROLE_CLIENT);
        user = new User(); user.setId(34L); user.setEmail("offline@test.local"); user.setRole(role); user.setCompany(company);
        setCurrentUser(user);
        when(users.whoami(any(HttpServletRequest.class), eq(false))).thenReturn(user);
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void eligibilityUsesFreshAccountAndReturnsFixedLimits() throws Exception {
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        when(offline.eligibility(user)).thenReturn(new MobileOfflineEligibility(true, true, 52L, 34L, now,
                now.plusSeconds(7 * 86400), MobileOfflineLimits.PILOT));
        mvc.perform(get("/mobile-offline/eligibility")).andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(52)).andExpect(jsonPath("$.limits.preparedWorkOrders").value(25));
        verify(users).whoami(any(HttpServletRequest.class), eq(false));
    }

    @Test void nonClientCannotAccessOfflineContract() throws Exception {
        user.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN); setCurrentUser(user);
        mvc.perform(get("/mobile-offline/eligibility")).andExpect(status().isForbidden());
        verifyNoInteractions(offline);
    }

    @Test void missingOperationVersionAndInvalidUuidAreRejectedBeforeMutation() throws Exception {
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationId\":\"" + UUID.randomUUID() + "\",\"workOrderId\":10,\"taskId\":7,\"type\":\"SET_TASK_VALUE\",\"value\":\"B\",\"occurredAt\":\"2026-11-03T12:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/mobile-offline/preparations/10").contentType(MediaType.APPLICATION_JSON)
                .content("{\"deviceId\":\"not-a-uuid\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(offline);
    }

    @Test void receiptBatchAbove100IsRejectedBeforeService() throws Exception {
        String ids = IntStream.range(0, 101).mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        mvc.perform(post("/mobile-offline/receipts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationIds\":[" + ids + "]}")).andExpect(status().isBadRequest());
        verifyNoInteractions(offline);
    }

    @Test void deletionPassesOnlyAuthenticatedOwnerToService() throws Exception {
        UUID device = UUID.randomUUID();
        mvc.perform(delete("/mobile-offline/preparations/10").param("deviceId", device.toString()))
                .andExpect(status().isOk());
        verify(offline).removePreparation(10, device, user);
    }

    @Test void notesAndCommentsUseTheSameContractWithCompactCommentReceipts() throws Exception {
        UUID note = UUID.randomUUID(); UUID comment = UUID.randomUUID();
        when(offline.submit(argThat(op -> op != null && op.type() == MobileOfflineOperation.Type.SET_TASK_NOTES), eq(user), isNull()))
                .thenReturn(new MobileOfflineResult(note, MobileOfflineResult.Status.APPLIED, "APPLIED", null, Instant.now(), 3L, null, null));
        when(offline.submit(argThat(op -> op != null && op.type() == MobileOfflineOperation.Type.CREATE_COMMENT), eq(user), isNull()))
                .thenReturn(new MobileOfflineResult(comment, MobileOfflineResult.Status.APPLIED, "APPLIED", null, Instant.now(), null, 99L, MobileOfflineResult.Visibility.VISIBLE));
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationId\":\"" + note + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,\"taskId\":7,\"type\":\"SET_TASK_NOTES\",\"baseVersion\":3,\"notes\":\"\",\"occurredAt\":\"2026-11-04T12:00:00Z\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.chainBaseVersion").value(3));
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operationId\":\"" + comment + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,\"type\":\"CREATE_COMMENT\",\"content\":\"field comment\",\"occurredAt\":\"2026-11-04T12:00:00Z\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.commentId").value(99))
                .andExpect(jsonPath("$.visibility").value("VISIBLE")).andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test void ambiguousFieldsAndCommentDependenciesAreStructuralErrors() throws Exception {
        String common = "{\"operationId\":\"" + UUID.randomUUID() + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,\"occurredAt\":\"2026-11-04T12:00:00Z\",";
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                .content(common + "\"taskId\":7,\"type\":\"SET_TASK_NOTES\",\"baseVersion\":3,\"notes\":\"notes\",\"value\":\"unsent value\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                .content(common + "\"type\":\"CREATE_COMMENT\",\"content\":\"comment\",\"predecessorOperationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(offline);
    }
    @Test void binaryTransferAcceptsMobileMultipartStringMetadataAndReturnsCompactReceipt() throws Exception {
        UUID id = UUID.randomUUID();
        when(offline.uploadFile(eq(id), any(), any(), eq(user))).thenReturn(new MobileOfflineResult(id,
                MobileOfflineResult.Status.APPLIED, "APPLIED", null, Instant.now(), null, null,
                MobileOfflineResult.Visibility.VISIBLE, 123L, null, null, null));
        var file = new org.springframework.mock.web.MockMultipartFile("file", "field.pdf", "application/pdf", new byte[]{1, 2, 3});
        mvc.perform(multipart("/mobile-offline/files/" + id).file(file).param("metadata",
                "{\"operationId\":\"" + id + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,"
                        + "\"target\":\"WORK_ORDER\",\"name\":\"field.pdf\",\"mimeType\":\"application/pdf\","
                        + "\"size\":3,\"sha256\":\"abc\",\"fileType\":\"OTHER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fileId").value(123))
                .andExpect(jsonPath("$.content").doesNotExist());
        verify(offline).uploadFile(eq(id), argThat(metadata -> metadata != null && metadata.workOrderId() == 10), any(), eq(user));
    }

    @Test void fileDownloadMetadataAlwaysChecksTheAuthenticatedWorkOrderContext() throws Exception {
        when(offline.downloadFile(10, 123, user)).thenReturn(new MobileOfflineFileDownload(123, "doc.pdf",
                "application/pdf", 12, "https://storage.test/signed"));
        mvc.perform(get("/mobile-offline/preparations/10/files/123")).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(12));
        verify(offline).downloadFile(10, 123, user);
    }

    @Test void timerStopCarriesItsExactOriginalStartThroughTheExistingContract() throws Exception {
        UUID startId = UUID.randomUUID(); UUID period = UUID.randomUUID(); UUID stopId = UUID.randomUUID();
        String start = "{\"operationId\":\"" + startId + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,"
                + "\"type\":\"START_TIMER\",\"occurredAt\":\"2026-11-05T10:00:00Z\",\"time\":{\"periodId\":\"" + period
                + "\",\"startedAt\":\"2026-11-05T10:00:00Z\",\"timerStatusRevision\":0}}";
        String stop = "{\"operationId\":\"" + stopId + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,"
                + "\"type\":\"STOP_TIMER\",\"occurredAt\":\"2026-11-05T10:30:00Z\",\"time\":{\"periodId\":\"" + period
                + "\",\"startedAt\":\"2026-11-05T10:00:00Z\",\"endedAt\":\"2026-11-05T10:30:00Z\",\"startOperation\":" + start + "}}";
        when(offline.submit(any(), eq(user), isNull())).thenReturn(new MobileOfflineResult(stopId, MobileOfflineResult.Status.APPLIED,
                "TIMER_PERIOD_RECORDED", null, Instant.now()));
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON).content(stop))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("TIMER_PERIOD_RECORDED"));
        verify(offline).submit(argThat(op -> op != null && op.type() == MobileOfflineOperation.Type.STOP_TIMER
                && op.time().startOperation().operationId().equals(startId) && op.time().periodId().equals(period)), eq(user), isNull());
        clearInvocations(offline);
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                        .content(stop.replace("\"startOperation\":" + start, "\"startOperation\":" + start.replace("\"userId\":34", "\"userId\":35"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(offline);
    }

    @Test void manualTimeCannotSubmitAClientCumulativeTargetOrAStartDependency() throws Exception {
        String common = "{\"operationId\":\"" + UUID.randomUUID() + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,"
                + "\"type\":\"CREATE_MANUAL_TIME\",\"occurredAt\":\"2026-11-05T12:00:00Z\",\"time\":{\"periodId\":\"" + UUID.randomUUID()
                + "\",\"startedAt\":\"2026-11-05T10:00:00Z\",\"endedAt\":\"2026-11-05T10:30:00Z\",\"laborId\":100}}";
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON).content(common))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(offline);
    }

    @Test void statusOperationCarriesRequiredGuardsDependenciesAndPlatform() throws Exception {
        UUID id = UUID.randomUUID(); UUID stop = UUID.randomUUID();
        when(offline.submit(any(), eq(user), eq("android"))).thenReturn(new MobileOfflineResult(id,
                MobileOfflineResult.Status.APPLIED, "STATUS_CHANGED", null, Instant.now()));
        String body = "{\"operationId\":\"" + id + "\",\"companyId\":52,\"userId\":34,\"workOrderId\":10,"
                + "\"type\":\"CHANGE_WORK_ORDER_STATUS\",\"occurredAt\":\"2026-11-06T10:30:00Z\","
                + "\"statusChange\":{\"status\":\"ON_HOLD\",\"baseRevision\":3,\"activeTimerRuns\":[],\"dependencyOperationIds\":[\"" + stop + "\"]}}";
        mvc.perform(post("/mobile-offline/operations").header("X-Platform", "android").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("STATUS_CHANGED"));
        verify(offline).submit(argThat(op -> op.statusChange().baseRevision() == 3
                && op.statusChange().dependencyOperationIds().equals(List.of(stop))), eq(user), eq("android"));
        clearInvocations(offline);
        mvc.perform(post("/mobile-offline/operations").contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("\"activeTimerRuns\":[],", ""))).andExpect(status().isBadRequest());
        verifyNoInteractions(offline);
    }

}
