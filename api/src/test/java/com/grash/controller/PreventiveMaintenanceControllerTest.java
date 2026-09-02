package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.dto.workOrder.WorkOrderMiniDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PreventiveMaintenanceService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreventiveMaintenanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PreventiveMaintenanceControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreventiveMaintenanceService preventiveMaintenanceService;
    @MockitoBean
    private PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    @MockitoBean
    private WorkOrderMapper workOrderMapper;
    @MockitoBean
    private UserService userService;

    private User clientUser;
    private User nonClientUser;
    private PreventiveMaintenanceShowDTO showDto;

    @BeforeEach
    void setUp() {
        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .build();

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(clientRole);
        clientUser.setEnabled(true);
        clientUser.setUserSettings(new UserSettings());
        clientUser.setSuperAccountRelations(new ArrayList<>());

        Role nonClientRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_SUPER_ADMIN)
                .name("SuperAdmin")
                .build();

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setFirstName("Admin");
        nonClientUser.setLastName("User");
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setRole(nonClientRole);
        nonClientUser.setEnabled(true);

        showDto = new PreventiveMaintenanceShowDTO();
        showDto.setId(1L);
        showDto.setName("Test PM");
        showDto.setCustomId("PM000001");
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(preventiveMaintenanceService.findBySearchCriteriaWithEntityGraph(any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/preventive-maintenances/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void getById_permitAll() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            PreventiveMaintenance pm = new PreventiveMaintenance();
            pm.setId(1L);
            when(preventiveMaintenanceService.getById(1L, clientUser)).thenReturn(pm);
            when(preventiveMaintenanceMapper.toShowDto(pm)).thenReturn(showDto);

            mockMvc.perform(get("/preventive-maintenances/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test PM"));
        }

        @Test
        void getRecentWorkOrders_permitAll() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo = new WorkOrder();
            wo.setId(10L);
            when(preventiveMaintenanceService.getRecentWorkOrders(1L, clientUser))
                    .thenReturn(Collections.singletonList(wo));
            WorkOrderMiniDTO miniDto = new WorkOrderMiniDTO();
            miniDto.setId(10L);
            when(workOrderMapper.toMiniDto(wo)).thenReturn(miniDto);

            mockMvc.perform(get("/preventive-maintenances/1/recent-work-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(10));
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/preventive-maintenances")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New PM\",\"title\":\"Title\",\"frequency\":1," +
                                    "\"recurrenceType\":\"DAILY\",\"recurrenceBasedOn\":\"SCHEDULED_DATE\"," +
                                    "\"customFields\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/preventive-maintenances/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(delete("/preventive-maintenances/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void triggerWorkOrder_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/preventive-maintenances/1/trigger-work-order"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.getById(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/preventive-maintenances/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.getById(1L, clientUser))
                    .thenThrow(new CustomException("Access Denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/preventive-maintenances/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_forbidden_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.create(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Access Denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/preventive-maintenances")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"PM\",\"title\":\"Title\",\"frequency\":1," +
                                    "\"recurrenceType\":\"DAILY\",\"recurrenceBasedOn\":\"SCHEDULED_DATE\"," +
                                    "\"customFields\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.patch(eq(99L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/preventive-maintenances/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doThrow(new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND))
                    .when(preventiveMaintenanceService).deleteByIdAndUser(99L, clientUser);

            mockMvc.perform(delete("/preventive-maintenances/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void triggerWorkOrder_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.triggerWorkOrder(99L, clientUser))
                    .thenThrow(new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(post("/preventive-maintenances/99/trigger-work-order"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void create_returnsShowDto() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            PreventiveMaintenance pm = new PreventiveMaintenance();
            pm.setId(1L);
            when(preventiveMaintenanceService.create(any(), eq(clientUser))).thenReturn(pm);
            when(preventiveMaintenanceMapper.toShowDto(pm)).thenReturn(showDto);

            mockMvc.perform(post("/preventive-maintenances")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New PM\",\"title\":\"Title\",\"frequency\":1," +
                                    "\"recurrenceType\":\"DAILY\",\"recurrenceBasedOn\":\"SCHEDULED_DATE\"," +
                                    "\"customFields\":[]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test PM"))
                    .andExpect(jsonPath("$.customId").value("PM000001"));
        }

        @Test
        void patch_returnsShowDto() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            PreventiveMaintenance patched = new PreventiveMaintenance();
            patched.setId(1L);
            when(preventiveMaintenanceService.patch(eq(1L), any(), eq(clientUser))).thenReturn(patched);
            when(preventiveMaintenanceMapper.toShowDto(patched)).thenReturn(showDto);

            mockMvc.perform(patch("/preventive-maintenances/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated PM\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test PM"));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doNothing().when(preventiveMaintenanceService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/preventive-maintenances/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void triggerWorkOrder_returnsMiniDto() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo = new WorkOrder();
            wo.setId(10L);
            when(preventiveMaintenanceService.triggerWorkOrder(1L, clientUser)).thenReturn(wo);
            WorkOrderMiniDTO miniDto = new WorkOrderMiniDTO();
            miniDto.setId(10L);
            when(workOrderMapper.toMiniDto(wo)).thenReturn(miniDto);

            mockMvc.perform(post("/preventive-maintenances/1/trigger-work-order"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        void getRecentWorkOrders_returnsMiniDtoList() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo1 = new WorkOrder();
            wo1.setId(10L);
            WorkOrder wo2 = new WorkOrder();
            wo2.setId(11L);
            when(preventiveMaintenanceService.getRecentWorkOrders(1L, clientUser))
                    .thenReturn(List.of(wo1, wo2));
            WorkOrderMiniDTO miniDto1 = new WorkOrderMiniDTO();
            miniDto1.setId(10L);
            WorkOrderMiniDTO miniDto2 = new WorkOrderMiniDTO();
            miniDto2.setId(11L);
            when(workOrderMapper.toMiniDto(wo1)).thenReturn(miniDto1);
            when(workOrderMapper.toMiniDto(wo2)).thenReturn(miniDto2);

            mockMvc.perform(get("/preventive-maintenances/1/recent-work-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(10))
                    .andExpect(jsonPath("$[1].id").value(11));
        }

        @Test
        void search_returnsEmptyPage() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(preventiveMaintenanceService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(preventiveMaintenanceService.findBySearchCriteriaWithEntityGraph(any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/preventive-maintenances/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }
}
