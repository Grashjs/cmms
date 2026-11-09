package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.RoleType;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LaborController.class)
@AutoConfigureMockMvc(addFilters = false)
class LaborControllerTest extends AbstractControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean LaborService labors;
    @MockitoBean UserService users;
    @MockitoBean WorkOrderService workOrders;
    User user;
    Labor labor;

    @BeforeEach void setup() {
        user = new User(); user.setId(34L); user.setRole(Role.builder().roleType(RoleType.ROLE_CLIENT).build());
        setCurrentUser(user);
        when(users.whoami(any())).thenReturn(user);
        WorkOrder wo = spy(new WorkOrder()); wo.setId(10L);
        lenient().doReturn(true).when(wo).canBeEditedBy(user);
        labor = new Labor(); labor.setId(100L); labor.setWorkOrder(wo); labor.setStartedAt(new Date()); labor.setVersion(4);
        lenient().when(labors.findById(100L)).thenReturn(Optional.of(labor));
    }

    @Test void stopCarriesTheDisplayedTargetGenerationAndVersion() throws Exception {
        UUID run = UUID.randomUUID();
        when(labors.controlTimer(10L, false, 100L, run, 4L, user)).thenReturn(labor);
        mvc.perform(post("/labors/work-order/10").param("start", "false").param("laborId", "100")
                        .param("timerRunId", run.toString()).param("version", "4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(4));
        verify(labors).controlTimer(10L, false, 100L, run, 4L, user);
    }

    @Test void startKeepsItsConnectedEndpointWithoutAddingAClientVersion() throws Exception {
        when(labors.controlTimer(10L, true, null, null, null, user)).thenReturn(labor);
        mvc.perform(post("/labors/work-order/10")).andExpect(status().isOk());
        verify(labors).controlTimer(10L, true, null, null, null, user);
    }

    @Test void correctionPassesTheVersionCapturedByTheFormAndReportsConflict() throws Exception {
        when(labors.update(eq(100L), any(), eq(user)))
                .thenThrow(new CustomException("LABOR_VERSION_CONFLICT", HttpStatus.CONFLICT));
        mvc.perform(patch("/labors/100").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":3,\"duration\":1800,\"includeToTotalTime\":true}"))
                .andExpect(status().isConflict());
        verify(labors).update(eq(100L), argThat(patch -> patch.getVersion() == 3 && patch.getDuration() == 1800), eq(user));
    }

    @Test void deletionCarriesItsObservedVersionAndMissingVersionIsNotSilentlyFilled() throws Exception {
        mvc.perform(delete("/labors/100").param("version", "4")).andExpect(status().isOk());
        verify(labors).delete(100L, 4L, user);
        doThrow(new CustomException("LABOR_VERSION_REQUIRED", HttpStatus.CONFLICT)).when(labors).delete(100L, null, user);
        mvc.perform(delete("/labors/100")).andExpect(status().isConflict());
        verify(labors).delete(100L, null, user);
    }

    @Test void existingClientRoleRequirementStillGuardsCorrections() throws Exception {
        user.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN); setCurrentUser(user);
        mvc.perform(patch("/labors/100").contentType(MediaType.APPLICATION_JSON).content("{\"version\":4}"))
                .andExpect(status().isForbidden());
        verify(labors, never()).update(anyLong(), any(), any());
    }
}
