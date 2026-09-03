package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.RequestApproveDTO;
import com.grash.dto.RequestPostDTO;
import com.grash.dto.RequestShowDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.Request;
import com.grash.model.User;
import com.grash.model.UserSettings;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static com.grash.model.enums.RoleType.ROLE_SUPER_ADMIN;
import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class RequestControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestService requestService;
    @MockitoBean
    private RequestMapper requestMapper;
    @MockitoBean
    private WorkOrderMapper workOrderMapper;
    @MockitoBean
    private com.grash.service.UserService userService;

    private User clientUser;
    private User nonClientUser;
    private RequestShowDTO showDto;

    @BeforeEach
    void setUp() {
        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(com.grash.model.Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .build());
        clientUser.setEnabled(true);
        clientUser.setUserSettings(new UserSettings());

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setFirstName("Admin");
        nonClientUser.setLastName("User");
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setRole(com.grash.model.Role.builder()
                .id(2L)
                .roleType(ROLE_SUPER_ADMIN)
                .name("SuperAdmin")
                .build());
        nonClientUser.setEnabled(true);

        showDto = new RequestShowDTO();
        showDto.setId(1L);
        showDto.setTitle("Test Request");
        showDto.setCustomId("R000001");
    }

    private User restrictedClient() {
        User u = new User();
        u.setId(4L);
        u.setFirstName("Restricted");
        u.setLastName("User");
        u.setEmail("restricted@test.com");
        u.setRole(com.grash.model.Role.builder()
                .id(4L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Restricted")
                .viewPermissions(new HashSet<>())
                .build());
        u.setEnabled(true);
        u.setUserSettings(new UserSettings());
        return u;
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestMapper.toShowDto(any())).thenReturn(showDto);
            when(requestService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(requestService.findBySearchCriteria(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/requests/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void getPending_withRequestViewPermission_returnsPendingCount() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            com.grash.model.Company company = new com.grash.model.Company();
            company.setId(1L);
            clientUser.setCompany(company);
            when(requestService.countPending(any())).thenReturn(5);

            mockMvc.perform(get("/requests/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("5"));
        }

        @Test
        void getPending_withoutRequestPermission_returnsForbidden() throws Exception {
            User restricted = restrictedClient();
            setCurrentUser(restricted);
            when(userService.whoami(any())).thenReturn(restricted);
            restricted.setCompany(new com.grash.model.Company());

            mockMvc.perform(get("/requests/pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getById_returnsRequest() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.getById(1L, clientUser)).thenReturn(new Request());
            when(requestMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(get("/requests/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test Request"));
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Test\",\"customFields\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/requests/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void approve_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/requests/1/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void cancel_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/requests/1/cancel")
                            .param("reason", "no longer needed"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(delete("/requests/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.create(any(RequestPostDTO.class), eq(clientUser))).thenReturn(new Request());
            when(requestMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(post("/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Test\",\"customFields\":[]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customId").value("R000001"));
        }

        @Test
        void patch_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.patch(eq(1L), any(), eq(clientUser))).thenReturn(new Request());
            when(requestMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(patch("/requests/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        void approve_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            WorkOrder wo = new WorkOrder();
            wo.setId(10L);
            when(requestService.approve(eq(1L), any(), eq(clientUser))).thenReturn(wo);
            WorkOrderShowDTO woDto = new WorkOrderShowDTO();
            woDto.setId(10L);
            when(workOrderMapper.toShowDto(wo)).thenReturn(woDto);

            mockMvc.perform(patch("/requests/1/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        void cancel_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.cancel(eq(1L), eq("not needed"), eq(clientUser))).thenReturn(new Request());
            when(requestMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(patch("/requests/1/cancel")
                            .param("reason", "not needed"))
                    .andExpect(status().isOk());
        }

        @Test
        void delete_success() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            mockMvc.perform(delete("/requests/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.getById(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/requests/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.getById(1L, clientUser))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/requests/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_fromPortal_notFound_returns404() throws Exception {
            when(requestService.createFromPortal(any(), eq("uuid"), any()))
                    .thenThrow(new CustomException("Request portal not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(post("/requests/portal/uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Test\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void create_fromPortal_success() throws Exception {
            when(requestService.createFromPortal(any(), eq("uuid"), any())).thenReturn(new Request());
            when(requestMapper.toShowDto(any())).thenReturn(showDto);

            mockMvc.perform(post("/requests/portal/uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Test\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customId").value("R000001"));
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(requestService.patch(eq(99L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Request not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/requests/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_accessDenied_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                    .when(requestService).deleteByIdAndUser(eq(1L), eq(clientUser));

            mockMvc.perform(delete("/requests/1"))
                    .andExpect(status().isForbidden());
        }
    }
}
