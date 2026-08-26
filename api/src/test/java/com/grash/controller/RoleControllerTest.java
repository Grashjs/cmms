package com.grash.controller;

import com.grash.dto.RolePatchDTO;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.service.RoleService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private UserService userService;

    private Company company;
    private User clientUser;
    private User clientUserWithoutPermission;
    private User nonClientUser;
    private Role companyRole;
    private Role defaultRole;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.getCompanySettings().setId(10L);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(1L);
        plan.setFeatures(new HashSet<>(Set.of(PlanFeatures.ROLE)));
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setSubscriptionPlan(plan);
        company.setSubscription(subscription);

        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.SETTINGS)))
                .build();

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
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.SETTINGS)))
                .build();

        nonClientUser = new User();
        nonClientUser.setId(2L);
        nonClientUser.setFirstName("Admin");
        nonClientUser.setLastName("User");
        nonClientUser.setEmail("admin@test.com");
        nonClientUser.setRole(nonClientRole);
        nonClientUser.setEnabled(true);

        companyRole = Role.builder()
                .id(4L)
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .name("Custom Role")
                .companySettings(company.getCompanySettings())
                .build();

        defaultRole = Role.builder()
                .id(5L)
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.TECHNICIAN)
                .name("Technician")
                .build();
    }

    @Nested
    class AuthorizationTests {

        @Test
        void create_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(post("/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Role\",\"roleType\":\"ROLE_CLIENT\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(patch("/roles/4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);

            mockMvc.perform(delete("/roles/4"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getAll_clientWithoutSettings_returns403() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);

            mockMvc.perform(get("/roles"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getById_clientWithoutSettings_returns403() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);
            when(roleService.findById(4L)).thenReturn(Optional.of(companyRole));

            mockMvc.perform(get("/roles/4"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_clientWithoutSettingsPermission_returns403() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);

            mockMvc.perform(post("/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Role\",\"roleType\":\"ROLE_CLIENT\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_planWithoutRoleFeature_returns403() throws Exception {
            company.getSubscription().getSubscriptionPlan().setFeatures(new HashSet<>(Set.of(PlanFeatures.ANALYTICS)));
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);

            mockMvc.perform(post("/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Role\",\"roleType\":\"ROLE_CLIENT\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_roleFromAnotherCompany_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(4L)).thenReturn(Optional.of(roleOfAnotherCompany()));

            mockMvc.perform(patch("/roles/4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_roleFromAnotherCompany_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(4L)).thenReturn(Optional.of(roleOfAnotherCompany()));

            mockMvc.perform(delete("/roles/4"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/roles/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_notBelongsToCompany_returns403() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(4L)).thenReturn(Optional.of(roleOfAnotherCompany()));

            mockMvc.perform(get("/roles/4"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(patch("/roles/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_notFound_returns404() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/roles/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_withoutSettingsPermission_returns403() throws Exception {
            setCurrentUser(clientUserWithoutPermission);
            when(userService.whoami(any())).thenReturn(clientUserWithoutPermission);

            mockMvc.perform(delete("/roles/4"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void getAll_nonClient_returnsAllRoles() throws Exception {
            setCurrentUser(nonClientUser);
            when(userService.whoami(any())).thenReturn(nonClientUser);
            when(roleService.getAll()).thenReturn(Arrays.asList(companyRole, defaultRole));

            mockMvc.perform(get("/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(4))
                    .andExpect(jsonPath("$[1].id").value(5));

            verify(roleService).getAll();
        }

        @Test
        void getAll_clientWithSettings_filtersByCompany() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findByCompany(1L)).thenReturn(Collections.singletonList(companyRole));

            mockMvc.perform(get("/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(4))
                    .andExpect(jsonPath("$[0].name").value("Custom Role"));

            verify(roleService).findByCompany(1L);
        }

        @Test
        void getById_returnsRole() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(4L)).thenReturn(Optional.of(companyRole));

            mockMvc.perform(get("/roles/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.name").value("Custom Role"))
                    .andExpect(jsonPath("$.code").value("USER_CREATED"));
        }

        @Test
        void getById_defaultRole_belongsToAllCompanies() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(5L)).thenReturn(Optional.of(defaultRole));

            mockMvc.perform(get("/roles/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.name").value("Technician"));
        }

        @Test
        void create_returnsCreatedRole() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.create(any(Role.class))).thenReturn(companyRole);

            mockMvc.perform(post("/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Custom Role\",\"roleType\":\"ROLE_CLIENT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.name").value("Custom Role"));

            ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
            verify(roleService).create(captor.capture());
            Role captured = captor.getValue();
            assertEquals(RoleCode.USER_CREATED, captured.getCode());
            assertEquals(RoleType.ROLE_CLIENT, captured.getRoleType());
            assertTrue(captured.isPaid());
            assertSame(company.getCompanySettings(), captured.getCompanySettings());
        }

        @Test
        void patch_returnsUpdatedRole() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(4L)).thenReturn(Optional.of(companyRole));
            when(roleService.update(eq(4L), any(RolePatchDTO.class))).thenReturn(companyRole);

            mockMvc.perform(patch("/roles/4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Renamed Role\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.name").value("Custom Role"));

            verify(roleService).update(eq(4L), any(RolePatchDTO.class));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            setCurrentUser(clientUser);
            when(userService.whoami(any())).thenReturn(clientUser);
            when(roleService.findById(4L)).thenReturn(Optional.of(companyRole));
            doNothing().when(roleService).delete(4L);

            mockMvc.perform(delete("/roles/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Deleted successfully"));

            verify(roleService).delete(4L);
        }
    }

    private Role roleOfAnotherCompany() {
        Company otherCompany = new Company();
        otherCompany.getCompanySettings().setId(99L);
        return Role.builder()
                .id(4L)
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .name("Other Company Role")
                .companySettings(otherCompany.getCompanySettings())
                .build();
    }
}
