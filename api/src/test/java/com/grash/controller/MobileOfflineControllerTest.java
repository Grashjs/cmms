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
}
