package com.grash.controller;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.UserInvitationService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private UserInvitationService userInvitationService;

    private User clientUser;
    private User clientUserWithoutPermission;
    private User nonClientUser;
    private User targetUser;
    private User disabledUser;
    private UserResponseDTO userResponseDto;
    private UserMiniDTO userMiniDto;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PEOPLE_AND_TEAMS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PEOPLE_AND_TEAMS)))
                .build();
        clientRole.getViewPermissions().add(PermissionEntity.SETTINGS);

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(clientRole);
        clientUser.setEnabled(true);
        clientUser.setCompany(company);

        Role restrictedRole = Role.builder()
                .id(3L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Restricted Client")
                .viewPermissions(new HashSet<>())
                .editOtherPermissions(new HashSet<>())
                .build();

        clientUserWithoutPermission = new User();
        clientUserWithoutPermission.setId(3L);
        clientUserWithoutPermission.setFirstName("Restricted");
        clientUserWithoutPermission.setLastName("User");
        clientUserWithoutPermission.setEmail("restricted@test.com");
        clientUserWithoutPermission.setRole(restrictedRole);
        clientUserWithoutPermission.setEnabled(true);
        clientUserWithoutPermission.setCompany(company);

        Role nonClientRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_SUPER_ADMIN)
                .name("Super Admin")
                .build();

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setFirstName("Admin");
        nonClientUser.setLastName("User");
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setRole(nonClientRole);
        nonClientUser.setEnabled(true);

        targetUser = new User();
        targetUser.setId(4L);
        targetUser.setFirstName("Target");
        targetUser.setLastName("User");
        targetUser.setEmail("target@test.com");
        targetUser.setCompany(company);

        disabledUser = new User();
        disabledUser.setId(5L);
        disabledUser.setFirstName("Disabled");
        disabledUser.setLastName("User");
        disabledUser.setEmail("disabled@test.com");
        disabledUser.setCompany(company);
        disabledUser.setEnabledInSubscription(false);

        userResponseDto = new UserResponseDTO();
        userResponseDto.setId(1);
        userResponseDto.setFirstName("John");
        userResponseDto.setLastName("Doe");
        userResponseDto.setEmail("john@test.com");

        userMiniDto = UserMiniDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Nested
    class AuthorizationTests {

        @Test
        void getLastWeekInvitations_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(get("/users/invitations/last-week"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMini_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(get("/users/mini"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMiniDisabled_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(get("/users/mini/disabled"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patchRole_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/users/1/role?role=1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void disable_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/users/1/disable"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void enable_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/users/1/enable"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void softDelete_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/users/soft-delete/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void search_clientWithoutPeopleAndTeams_returns403() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);

            mockMvc.perform(post("/users/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getLastWeekInvitations_withoutSettings_returns403() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);

            mockMvc.perform(get("/users/invitations/last-week"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notOwnerWithoutPermission_returns406() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);
            when(userService.findByIdAndCompany(4L, 1L)).thenReturn(Optional.of(targetUser));

            mockMvc.perform(patch("/users/4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Hacked\"}"))
                    .andExpect(status().isNotAcceptable());
        }

        @Test
        void enable_withoutEditPermission_returns406() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);

            mockMvc.perform(patch("/users/4/enable"))
                    .andExpect(status().isNotAcceptable());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByIdAndCompany(99L, 1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/users/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_companyMismatch_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            Company otherCompany = new Company();
            otherCompany.setId(99L);
            targetUser.setCompany(otherCompany);

            when(userService.findByIdAndCompany(4L, 1L)).thenReturn(Optional.of(targetUser));

            mockMvc.perform(get("/users/4"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns406() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByIdAndCompany(99L, 1L)).thenReturn(Optional.empty());

            mockMvc.perform(patch("/users/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"NewName\"}"))
                    .andExpect(status().isNotAcceptable());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @SuppressWarnings("unchecked")
        @Test
        void search_returnsPage() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            List<User> userList = Collections.singletonList(clientUser);
            Page<User> userPage = new PageImpl<>(userList);

            when(userService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(userPage);
            when(userMapper.toResponseDtoWithShift(clientUser)).thenReturn(userResponseDto);

            mockMvc.perform(post("/users/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("john@test.com"));
        }

        @SuppressWarnings("unchecked")
        @Test
        void search_clientWithPermission_filtersByCompany() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            List<User> userList = Collections.singletonList(clientUser);
            Page<User> userPage = new PageImpl<>(userList);

            when(userService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(userPage);
            when(userMapper.toResponseDtoWithShift(clientUser)).thenReturn(userResponseDto);

            mockMvc.perform(post("/users/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("john@test.com"));
        }

        @SuppressWarnings("unchecked")
        @Test
        void search_enabledOnlyFalse_doesNotAddEnabledFilter() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            Page<User> emptyPage = new PageImpl<>(Collections.emptyList());
            when(userService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(emptyPage);

            mockMvc.perform(post("/users/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}")
                            .param("enabledOnly", "false"))
                    .andExpect(status().isOk());
        }

        @Test
        void invite_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.inviteUsers(any(UserInvitationDTO.class), eq(clientUser)))
                    .thenReturn(new SuccessResponse(true, "Users have been invited"));

            mockMvc.perform(post("/users/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":{\"id\":1},\"emails\":[\"new@test.com\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Users have been invited"));
        }

        @Test
        void getLastWeekInvitations_returnsCollection() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            List<UserInvitationMiniDTO> invitations = Collections.singletonList(
                    new UserInvitationMiniDTO("invited@test.com", 1L, "Technician"));
            when(userInvitationService.getDistinctByCompanyInLastWeek(1L)).thenReturn(invitations);

            mockMvc.perform(get("/users/invitations/last-week"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("invited@test.com"))
                    .andExpect(jsonPath("$[0].roleName").value("Technician"));
        }

        @Test
        void getMini_returnsWorkers() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findWorkersByCompany(1L)).thenReturn(Collections.singletonList(clientUser));
            when(userMapper.toMiniDto(clientUser)).thenReturn(userMiniDto);

            mockMvc.perform(get("/users/mini"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }

        @Test
        void getMini_withRequesters_returnsAll() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByCompany(1L)).thenReturn(Collections.singletonList(clientUser));
            when(userMapper.toMiniDto(clientUser)).thenReturn(userMiniDto);

            mockMvc.perform(get("/users/mini")
                            .param("withRequesters", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }

        @Test
        void getMiniDisabled_returnsCollection() throws Exception {
            UserMiniDTO disabledMiniDto = UserMiniDTO.builder()
                    .id(5L)
                    .firstName("Disabled")
                    .lastName("User")
                    .build();

            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByCompany(1L)).thenReturn(Collections.singletonList(disabledUser));
            when(userMapper.toMiniDto(disabledUser)).thenReturn(disabledMiniDto);

            mockMvc.perform(get("/users/mini/disabled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("Disabled"));
        }

        @Test
        void patch_selfEdit_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByIdAndCompany(1L, 1L)).thenReturn(Optional.of(clientUser));
            when(userService.update(eq(1L), any(UserPatchDTO.class))).thenReturn(clientUser);
            when(userMapper.toResponseDto(clientUser)).thenReturn(userResponseDto);

            mockMvc.perform(patch("/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Johnny\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void patch_withPermission_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByIdAndCompany(4L, 1L)).thenReturn(Optional.of(targetUser));
            when(userService.update(eq(4L), any(UserPatchDTO.class))).thenReturn(targetUser);
            when(userMapper.toResponseDto(targetUser)).thenReturn(userResponseDto);

            mockMvc.perform(patch("/users/4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Updated\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void getById_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByIdAndCompany(4L, 1L)).thenReturn(Optional.of(targetUser));
            when(userMapper.toResponseDtoWithShift(targetUser)).thenReturn(userResponseDto);

            mockMvc.perform(get("/users/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void patchRole_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.patchUserRole(4L, 2L, clientUser)).thenReturn(targetUser);
            when(userMapper.toResponseDto(targetUser)).thenReturn(userResponseDto);

            mockMvc.perform(patch("/users/4/role?role=2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void disable_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.disableUser(4L, clientUser)).thenReturn(targetUser);
            when(userMapper.toResponseDto(targetUser)).thenReturn(userResponseDto);

            mockMvc.perform(patch("/users/4/disable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void enable_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.findByIdAndCompany(4L, 1L)).thenReturn(Optional.of(targetUser));
            when(userService.enableUser(targetUser)).thenReturn(targetUser);
            when(userMapper.toResponseDto(targetUser)).thenReturn(userResponseDto);

            mockMvc.perform(patch("/users/4/enable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }

        @Test
        void softDelete_returnsUserResponseDTO() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(userService.softDeleteUser(4L, clientUser)).thenReturn(targetUser);
            when(userMapper.toResponseDto(targetUser)).thenReturn(userResponseDto);

            mockMvc.perform(patch("/users/soft-delete/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@test.com"));
        }
    }
}
