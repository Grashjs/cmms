package com.grash.controller;

import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.exception.GlobalExceptionHandlerController;
import com.grash.mapper.FileMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.Status;
import com.grash.repository.ApiKeyRepository;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.security.OAuth2AuthenticationSuccessHandler;
import com.grash.security.OAuth2AuthenticationFailureHandler;
import com.grash.security.CustomUserDetailsService;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerController.class)
@EnableMethodSecurity(prePostEnabled = true)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkOrderService workOrderService;
    @MockBean
    private WorkOrderMapper workOrderMapper;
    @MockBean
    private UserService userService;
    @MockBean
    private AssetService assetService;
    @MockBean
    private LocationService locationService;
    @MockBean
    private PartService partService;
    @MockBean
    private FileMapper fileMapper;
    @MockBean
    private ApiKeyRepository apiKeyRepository;
    @MockBean
    private LicenseService licenseService;
    @MockBean
    private RateLimiterService rateLimiterService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    private User clientUser;
    private User nonClientUser;
    private WorkOrderShowDTO showDto;

    @BeforeEach
    void setUp() {
        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
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
        clientUser.setSuperAccountRelations(new ArrayList<>());
        clientUser.setUserSettings(new UserSettings());

        Role nonClientRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_SUPER_ADMIN)
                .name("SuperAdmin")
                .viewPermissions(new HashSet<>())
                .createPermissions(new HashSet<>())
                .viewOtherPermissions(new HashSet<>())
                .editOtherPermissions(new HashSet<>())
                .deleteOtherPermissions(new HashSet<>())
                .build();

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setFirstName("Admin");
        nonClientUser.setLastName("User");
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setRole(nonClientRole);
        nonClientUser.setEnabled(true);
        nonClientUser.setSuperAccountRelations(new ArrayList<>());

        showDto = new WorkOrderShowDTO();
        showDto.setId(1L);
        showDto.setTitle("Test WO");
        showDto.setStatus(Status.OPEN);
    }

    private void setCurrentUser(User user) {
        CustomUserDetail detail = CustomUserDetail.builder().user(user).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(detail, null, detail.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ═══════════════════════════════════════════════════════════════════
    // @PreAuthorize enforcement
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(workOrderService.getSearchCriteria(any(), any())).thenReturn(new com.grash.advancedsearch.SearchCriteria());
            when(workOrderService.findBySearchCriteria(any())).thenReturn(
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
    }

    // ═══════════════════════════════════════════════════════════════════
    // CustomException → HTTP status mapping
    // ═══════════════════════════════════════════════════════════════════
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
    }

    // ═══════════════════════════════════════════════════════════════════
    // X-Platform header passthrough
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // Routing and serialization
    // ═══════════════════════════════════════════════════════════════════
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
    }
}
