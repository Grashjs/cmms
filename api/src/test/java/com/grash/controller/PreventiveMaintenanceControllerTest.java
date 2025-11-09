package com.grash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenancePostDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Role;
import com.grash.model.Schedule;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PreventiveMaintenanceService;
import com.grash.service.ScheduleService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PreventiveMaintenanceControllerTest {

    @Mock
    private PreventiveMaintenanceService preventiveMaintenanceService;
    @Mock
    private UserService userService;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    @Mock
    private EntityManager em;

    @InjectMocks
    private PreventiveMaintenanceController preventiveMaintenanceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @ControllerAdvice
    static class CustomExceptionHandler {
        @ExceptionHandler(CustomException.class)
        public ResponseEntity<Object> handleCustomException(CustomException ex) {
            return new ResponseEntity<>(ex.getMessage(), ex.getHttpStatus());
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(preventiveMaintenanceController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private OwnUser createMockUser(RoleType roleType, Set<PermissionEntity> permissions) {
        OwnUser user = new OwnUser();
        Role role = new Role();
        role.setRoleType(roleType);
        role.setViewPermissions(permissions);
        user.setRole(role);
        Company company = new Company();
        company.setId(1L);
        user.setCompany(company);
        return user;
    }

    @Nested
    @DisplayName("Search Method Tests")
    class SearchTests {

        @Test
        @WithMockUser
        @DisplayName("should return page of preventive maintenances for client with permission")
        void search_whenUserIsClientWithPermission_shouldReturnPageOfDTOs() throws Exception {
            OwnUser user = createMockUser(RoleType.ROLE_CLIENT, Collections.singleton(PermissionEntity.PREVENTIVE_MAINTENANCES));
            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(user);

            Page<PreventiveMaintenanceShowDTO> page = new PageImpl<>(Collections.singletonList(new PreventiveMaintenanceShowDTO()));
            when(preventiveMaintenanceService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            mockMvc.perform(post("/preventive-maintenances/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SearchCriteria())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("should return forbidden for client without permission")
        void search_whenUserIsClientWithoutPermission_shouldReturnForbidden() throws Exception {
            OwnUser user = createMockUser(RoleType.ROLE_CLIENT, Collections.emptySet());
            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(user);

            mockMvc.perform(post("/preventive-maintenances/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SearchCriteria())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get By Id Method Tests")
    class GetByIdTests {

        @Test
        @WithMockUser
        @DisplayName("should return preventive maintenance when found")
        void getById_whenFound_shouldReturnDTO() throws Exception {
            PreventiveMaintenance pm = new PreventiveMaintenance();
            PreventiveMaintenanceShowDTO dto = new PreventiveMaintenanceShowDTO();
            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(new OwnUser());
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.of(pm));
            when(preventiveMaintenanceMapper.toShowDto(pm)).thenReturn(dto);

            mockMvc.perform(get("/preventive-maintenances/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("should return not found when preventive maintenance does not exist")
        void getById_whenNotFound_shouldReturnNotFound() throws Exception {
            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(new OwnUser());
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/preventive-maintenances/1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Create Method Tests")
    class CreateTests {

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should create preventive maintenance")
        void create_shouldCreatePreventiveMaintenance() throws Exception {
            OwnUser user = new OwnUser();
            PreventiveMaintenancePostDTO postDTO = new PreventiveMaintenancePostDTO();
            postDTO.setStartsOn(new Date());
            postDTO.setEndsOn(new Date());
            postDTO.setTitle("Test Title");
            postDTO.setFrequency(1);

            PreventiveMaintenance pm = new PreventiveMaintenance();
            Schedule schedule = new Schedule();
            pm.setSchedule(schedule);

            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(user);
            when(preventiveMaintenanceMapper.toModel(any(PreventiveMaintenancePostDTO.class))).thenReturn(pm);
            when(preventiveMaintenanceService.create(any(PreventiveMaintenance.class), any(OwnUser.class))).thenReturn(pm);
            when(scheduleService.save(any(Schedule.class))).thenReturn(schedule);
            when(preventiveMaintenanceMapper.toShowDto(any(PreventiveMaintenance.class))).thenReturn(new PreventiveMaintenanceShowDTO());

            mockMvc.perform(post("/preventive-maintenances")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postDTO)))
                    .andExpect(status().isOk());

            verify(em, times(2)).refresh(any());
            verify(scheduleService).scheduleWorkOrder(any(Schedule.class));
        }
    }

    @Nested
    @DisplayName("Patch Method Tests")
    class PatchTests {

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should patch preventive maintenance when found")
        void patch_whenFound_shouldPatch() throws Exception {
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();
            patchDTO.setTitle("Test Title");
            PreventiveMaintenance pm = new PreventiveMaintenance();

            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(new OwnUser());
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.of(pm));
            when(preventiveMaintenanceService.update(anyLong(), any(PreventiveMaintenancePatchDTO.class))).thenReturn(pm);
            when(preventiveMaintenanceMapper.toShowDto(pm)).thenReturn(new PreventiveMaintenanceShowDTO());

            mockMvc.perform(patch("/preventive-maintenances/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should return not found on patch when preventive maintenance does not exist")
        void patch_whenNotFound_shouldReturnNotFound() throws Exception {
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();
            patchDTO.setTitle("Test Title");
            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(new OwnUser());
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.empty());

            mockMvc.perform(patch("/preventive-maintenances/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete Method Tests")
    class DeleteTests {

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should delete preventive maintenance when found")
        void delete_whenFound_shouldDelete() throws Exception {
            PreventiveMaintenance pm = new PreventiveMaintenance();
            Schedule schedule = new Schedule();
            schedule.setId(1L);
            pm.setSchedule(schedule);

            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(new OwnUser());
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.of(pm));

            mockMvc.perform(delete("/preventive-maintenances/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(scheduleService).stopScheduleTimers(1L);
            verify(preventiveMaintenanceService).delete(1L);
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should return not found on delete when preventive maintenance does not exist")
        void delete_whenNotFound_shouldReturnNotFound() throws Exception {
            when(userService.whoami(any(HttpServletRequest.class))).thenReturn(new OwnUser());
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/preventive-maintenances/1"))
                    .andExpect(status().isNotFound());
        }
    }
}
