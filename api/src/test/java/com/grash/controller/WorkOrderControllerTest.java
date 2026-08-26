package com.grash.controller;

import com.grash.dto.CalendarEvent;
import com.grash.dto.FileShowDTO;
import com.grash.dto.WorkOrderBaseMiniDTO;
import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.FileMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.Status;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkOrderControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderService workOrderService;
    @MockitoBean
    private WorkOrderMapper workOrderMapper;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AssetService assetService;
    @MockitoBean
    private LocationService locationService;
    @MockitoBean
    private PartService partService;
    @MockitoBean
    private FileMapper fileMapper;
    private User clientUser;
    private User nonClientUser;
    private WorkOrderShowDTO showDto;

    @BeforeEach
    void setUp() {
        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Arrays.asList(
                        PermissionEntity.WORK_ORDERS,
                        PermissionEntity.ASSETS,
                        PermissionEntity.LOCATIONS,
                        PermissionEntity.PARTS_AND_MULTIPARTS)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .viewOtherPermissions(new HashSet<>(Arrays.asList(
                        PermissionEntity.WORK_ORDERS,
                        PermissionEntity.ASSETS,
                        PermissionEntity.LOCATIONS,
                        PermissionEntity.PARTS_AND_MULTIPARTS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .build();

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(clientRole);
        clientUser.setEnabled(true);
        clientUser.setUserSettings(new UserSettings());

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

        showDto = new WorkOrderShowDTO();
        showDto.setId(1L);
        showDto.setTitle("Test WO");
        showDto.setStatus(Status.OPEN);
    }

    private User restrictedClient() {
        Role restrictedRole = Role.builder()
                .id(4L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Restricted Client")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .build();
        User restrictedUser = new User();
        restrictedUser.setId(4L);
        restrictedUser.setFirstName("Restricted");
        restrictedUser.setLastName("User");
        restrictedUser.setEmail("restricted@test.com");
        restrictedUser.setRole(restrictedRole);
        restrictedUser.setEnabled(true);
        restrictedUser.setUserSettings(new UserSettings());
        return restrictedUser;
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.getSearchCriteria(any(), any())).thenReturn(new com.grash.advancedsearch.SearchCriteria());
            when(workOrderService.findBySearchCriteriaWithEntityGraph(any())).thenReturn(
                    new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/work-orders/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void searchMini_permitAll_noAuthRequired() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.getSearchCriteria(any(), any())).thenReturn(new com.grash.advancedsearch.SearchCriteria());
            when(workOrderService.findBySearchCriteria(any())).thenReturn(
                    new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/work-orders/search/mini")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Test\",\"customFields\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/work-orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void changeStatus_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/work-orders/1/change-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(delete("/work-orders/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getPDF_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(get("/work-orders/report/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getPDFWithConfig_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/work-orders/report/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void sendReport_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/work-orders/1/report/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customers\":[{\"id\":1}],\"message\":\"hi\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByAsset_permitAll() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            Asset asset = new Asset();
            asset.setId(1L);
            when(assetService.findById(1L)).thenReturn(Optional.of(asset));
            when(workOrderService.findByAsset(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/work-orders/asset/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByLocation_permitAll() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            Location loc = new Location();
            loc.setId(1L);
            when(locationService.findById(1L)).thenReturn(Optional.of(loc));
            when(workOrderService.findByLocation(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/work-orders/location/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByAsset_accessDenied_whenNotViewable() throws Exception {
            User restrictedUser = restrictedClient();
            setCurrentUser(restrictedUser);
            when(userService.whoami(any())).thenReturn(restrictedUser);
            Asset asset = new Asset();
            asset.setId(1L);
            when(assetService.findById(1L)).thenReturn(Optional.of(asset));

            mockMvc.perform(get("/work-orders/asset/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByLocation_accessDenied_whenNotViewable() throws Exception {
            User restrictedUser = restrictedClient();
            setCurrentUser(restrictedUser);
            when(userService.whoami(any())).thenReturn(restrictedUser);
            Location loc = new Location();
            loc.setId(1L);
            when(locationService.findById(1L)).thenReturn(Optional.of(loc));

            mockMvc.perform(get("/work-orders/location/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByPart_accessDenied_whenNotViewable() throws Exception {
            User restrictedUser = restrictedClient();
            setCurrentUser(restrictedUser);
            when(userService.whoami(any())).thenReturn(restrictedUser);
            Part part = new Part();
            part.setId(1L);
            when(partService.findById(1L)).thenReturn(Optional.of(part));

            mockMvc.perform(get("/work-orders/part/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getById_permitAll() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo = new WorkOrder();
            wo.setId(1L);
            when(workOrderService.checkAccessToWorkOrderId(1L, clientUser)).thenReturn(wo);
            when(workOrderMapper.toShowDto(wo)).thenReturn(showDto);

            mockMvc.perform(get("/work-orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test WO"));
        }

        @Test
        void getEvents_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/work-orders/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"start\":\"2024-01-01T00:00:00\",\"end\":\"2024-12-31T23:59:59\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void addFiles_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/work-orders/files/1/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void removeFile_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(delete("/work-orders/files/1/1/remove"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getUrgentCount_success() throws Exception {
            Role clientWithRequestsRole = Role.builder()
                    .id(3L)
                    .roleType(RoleType.ROLE_CLIENT)
                    .name("Client With Requests")
                    .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                    .build();
            User urgentClient = new User();
            urgentClient.setId(3L);
            urgentClient.setFirstName("Jane");
            urgentClient.setLastName("Doe");
            urgentClient.setEmail("jane@test.com");
            urgentClient.setRole(clientWithRequestsRole);
            urgentClient.setEnabled(true);
            urgentClient.setUserSettings(new UserSettings());

            setCurrentUser(urgentClient);
            when(userService.whoami(any())).thenReturn(urgentClient);
            when(workOrderService.countUrgent(urgentClient)).thenReturn(5);

            mockMvc.perform(get("/work-orders/urgent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("5"));
        }

        @Test
        void getUrgentCount_forbidden() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            mockMvc.perform(get("/work-orders/urgent"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.checkAccessToWorkOrderId(99L, clientUser))
                    .thenThrow(new CustomException("Work Order not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/work-orders/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.checkAccessToWorkOrderId(1L, clientUser))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/work-orders/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByAsset_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(assetService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/work-orders/asset/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getByLocation_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(locationService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/work-orders/location/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void sendReport_badRequest_returns400() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo = new WorkOrder();
            wo.setId(1L);
            when(workOrderService.checkAccessToWorkOrderId(1L, clientUser)).thenReturn(wo);
            doThrow(new CustomException("No customers with email addresses found", HttpStatus.BAD_REQUEST))
                    .when(workOrderService).sendReport(eq(1L), any(), eq(clientUser));

            mockMvc.perform(post("/work-orders/1/report/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customers\":[{\"id\":1}],\"message\":\"test\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_forbidden_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.createByUser(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"WO\",\"customFields\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByPart_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(partService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/work-orders/part/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PlatformHeaderPassthrough {

        @Test
        void changeStatus_passesXPlatformHeaderToService() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.changeStatus(any(), eq(1L), eq(clientUser), eq("ios"))).thenReturn(new WorkOrder());
            when(workOrderMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(patch("/work-orders/1/change-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}")
                            .header("X-Platform", "ios"))
                    .andExpect(status().isOk());

            verify(workOrderService).changeStatus(any(WorkOrderChangeStatusDTO.class), eq(1L), eq(clientUser), eq(
                    "ios"));
        }

        @Test
        void changeStatus_nullPlatformHeader() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.changeStatus(any(), eq(1L), eq(clientUser), isNull())).thenReturn(new WorkOrder());
            when(workOrderMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(patch("/work-orders/1/change-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isOk());

            verify(workOrderService).changeStatus(any(WorkOrderChangeStatusDTO.class), eq(1L), eq(clientUser),
                    isNull());
        }

        @Test
        void changeStatus_androidPlatform() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.changeStatus(any(), eq(1L), eq(clientUser), eq("android"))).thenReturn(new WorkOrder());
            when(workOrderMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(patch("/work-orders/1/change-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"OPEN\"}")
                            .header("X-Platform", "android"))
                    .andExpect(status().isOk());

            verify(workOrderService).changeStatus(any(), eq(1L), eq(clientUser), eq("android"));
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void create_returnsShowDto() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo = new WorkOrder();
            wo.setId(1L);
            when(workOrderService.createByUser(any(), eq(clientUser))).thenReturn(wo);
            when(workOrderMapper.toShowDto(wo)).thenReturn(showDto);

            mockMvc.perform(post("/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"New WO\",\"customFields\":[]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test WO"));
        }

        @Test
        void patch_returnsShowDto() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder patched = new WorkOrder();
            patched.setId(1L);
            when(workOrderService.patch(eq(1L), any(), eq(clientUser))).thenReturn(patched);
            when(workOrderMapper.toShowDto(patched)).thenReturn(showDto);

            mockMvc.perform(patch("/work-orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Patched\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test WO"));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doNothing().when(workOrderService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/work-orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void getPDF_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.generateReport(eq(1L), eq(clientUser), any())).thenReturn("https://signed-url.pdf");

            mockMvc.perform(get("/work-orders/report/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("https://signed-url.pdf"));
        }

        @Test
        void getPDFWithConfig_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.generateReport(eq(1L), eq(clientUser), any())).thenReturn("https://signed-url.pdf");

            mockMvc.perform(post("/work-orders/report/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void sendReport_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            mockMvc.perform(post("/work-orders/1/report/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customers\":[{\"id\":1}],\"message\":\"test\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void getByPart_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            Part part = new Part();
            part.setId(1L);
            when(partService.findById(1L)).thenReturn(Optional.of(part));
            WorkOrder wo = new WorkOrder();
            wo.setId(1L);
            when(workOrderService.getWorkOrdersByPart(1L)).thenReturn(Collections.singletonList(wo));
            when(workOrderMapper.toShowDto(wo)).thenReturn(showDto);

            mockMvc.perform(get("/work-orders/part/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test WO"));
        }

        @Test
        void getEvents_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrderBaseMiniDTO miniDto = new WorkOrderBaseMiniDTO();
            miniDto.setId(1L);
            miniDto.setTitle("Event WO");
            CalendarEvent<WorkOrderBaseMiniDTO> event = new CalendarEvent<>("workOrder", miniDto, new Date());
            when(workOrderService.getEvents(any(), isNull(), eq(clientUser)))
                    .thenReturn(Collections.singletonList(event));

            mockMvc.perform(post("/work-orders/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"start\":\"2024-01-01T00:00:00\",\"end\":\"2024-12-31T23:59:59\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("workOrder"));
        }

        @Test
        void addFilesToWorkOrder_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            File file = new File();
            file.setId(1L);
            file.setName("test.pdf");
            when(workOrderService.addFiles(eq(1L), any(), eq(clientUser)))
                    .thenReturn(Collections.singletonList(file));
            FileShowDTO fileShowDto = new FileShowDTO();
            fileShowDto.setId(1L);
            fileShowDto.setName("test.pdf");
            when(fileMapper.toShowDto(file)).thenReturn(fileShowDto);

            mockMvc.perform(patch("/work-orders/files/1/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[{\"name\":\"test.pdf\",\"path\":\"/files/test.pdf\"}]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("test.pdf"));
        }

        @Test
        void removeFileFromWorkOrder_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            File file = new File();
            file.setId(1L);
            file.setName("removed.pdf");
            when(workOrderService.removeFile(eq(1L), eq(1L), eq(clientUser)))
                    .thenReturn(Collections.singletonList(file));
            FileShowDTO fileShowDto = new FileShowDTO();
            fileShowDto.setId(1L);
            fileShowDto.setName("removed.pdf");
            when(fileMapper.toShowDto(file)).thenReturn(fileShowDto);

            mockMvc.perform(delete("/work-orders/files/1/1/remove"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("removed.pdf"));
        }
    }
}
