package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationMiniDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.LocationMapper;
import com.grash.model.Company;
import com.grash.model.Location;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.User;
import com.grash.model.UserSettings;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.LocationService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;
    @MockitoBean
    private LocationMapper locationMapper;
    @MockitoBean
    private UserService userService;

    private User clientUser;
    private User nonClientUser;
    private Location location;
    private LocationShowDTO showDto;
    private LocationMiniDTO miniDto;

    @BeforeEach
    void setUp() {
        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS)))
                .build();

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(clientRole);
        clientUser.setEnabled(true);
        clientUser.setUserSettings(new UserSettings());
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        Company company = new Company("ClientCo", 10, subscription);
        company.setId(1L);
        clientUser.setCompany(company);

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
        nonClientUser.setUserSettings(new UserSettings());

        location = new Location();
        location.setId(1L);
        location.setName("Warehouse A");

        showDto = new LocationShowDTO();
        showDto.setId(1L);
        showDto.setName("Warehouse A");

        miniDto = new LocationMiniDTO();
        miniDto.setId(1L);
        miniDto.setName("Warehouse A");
    }

    private void mockCurrentUser(User user) {
        setCurrentUser(user);
        when(userService.whoami(any())).thenReturn(user);
    }

    private void mockShowMapping() {
        when(locationMapper.toShowDto(any(Location.class), any(LocationService.class))).thenReturn(showDto);
    }

    private void mockMiniMapping() {
        when(locationMapper.toMiniDto(any(Location.class))).thenReturn(miniDto);
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(locationService.findBySearchCriteria(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/locations/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void getChildren_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildren(eq(1L), any(User.class)))
                    .thenReturn(Collections.singletonList(location));
            mockShowMapping();

            mockMvc.perform(get("/locations/children/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getChildrenPaginated_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildrenPaginated(eq(1L), any(), any(User.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(location)));
            mockShowMapping();

            mockMvc.perform(get("/locations/children/1/paginated"))
                    .andExpect(status().isOk());
        }

        @Test
        void getMiniPublic_noPreAuthorize_noAuthRequired() throws Exception {
            when(locationService.getMiniPublic(anyString(), any())).thenReturn(Collections.singletonList(location));
            mockMiniMapping();

            mockMvc.perform(get("/locations/public/mini/abc-123"))
                    .andExpect(status().isOk());
        }

        @Test
        void getById_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getById(1L, clientUser)).thenReturn(location);
            mockShowMapping();

            mockMvc.perform(get("/locations/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(post("/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Location\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(patch("/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMini_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(get("/locations/mini"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(delete("/locations/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getById(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/locations/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getById(1L, clientUser))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/locations/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getChildren_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildren(eq(99L), any(User.class)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/locations/children/99"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getChildren_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildren(eq(99L), any(User.class)))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/locations/children/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getChildrenPaginated_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildrenPaginated(eq(99L), any(), any(User.class)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/locations/children/99/paginated"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getChildrenPaginated_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildrenPaginated(eq(99L), any(), any(User.class)))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/locations/children/99/paginated"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void search_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getSearchCriteria(any(), any()))
                    .thenThrow(new CustomException("Access Denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/locations/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.create(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Location\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.patch(eq(99L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Location not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/locations/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void patch_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.patch(eq(1L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN));

            mockMvc.perform(patch("/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Location not found", HttpStatus.NOT_FOUND))
                    .when(locationService).deleteByIdAndUser(99L, clientUser);

            mockMvc.perform(delete("/locations/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                    .when(locationService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/locations/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMiniPublic_rateLimited_returns429() throws Exception {
            when(locationService.getMiniPublic(anyString(), any()))
                    .thenThrow(new CustomException("Rate limit exceeded. Try again later.",
                            HttpStatus.TOO_MANY_REQUESTS));

            mockMvc.perform(get("/locations/public/mini/abc-123"))
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void search_returnsPage() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(locationService.findBySearchCriteria(any()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(location)));
            mockShowMapping();

            mockMvc.perform(post("/locations/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Warehouse A"));
        }

        @Test
        void getChildren_returnsList() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildren(eq(1L), any(User.class)))
                    .thenReturn(Collections.singletonList(location));
            mockShowMapping();

            mockMvc.perform(get("/locations/children/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Warehouse A"));
        }

        @Test
        void getChildrenPaginated_returnsPage() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getChildrenPaginated(eq(1L), any(), any(User.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(location)));
            mockShowMapping();

            mockMvc.perform(get("/locations/children/1/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Warehouse A"));
        }

        @Test
        void getById_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.getById(1L, clientUser)).thenReturn(location);
            mockShowMapping();

            mockMvc.perform(get("/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Warehouse A"));
        }

        @Test
        void create_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.create(any(), eq(clientUser))).thenReturn(location);
            mockShowMapping();

            mockMvc.perform(post("/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Warehouse A\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Warehouse A"));
        }

        @Test
        void patch_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.patch(eq(1L), any(), eq(clientUser))).thenReturn(location);
            mockShowMapping();

            mockMvc.perform(patch("/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Warehouse A\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Warehouse A"));
        }

        @Test
        void getMini_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(locationService.findByCompany(any())).thenReturn(Collections.singletonList(location));
            mockMiniMapping();

            mockMvc.perform(get("/locations/mini"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Warehouse A"));
        }

        @Test
        void getMiniPublic_returnsCollection() throws Exception {
            when(locationService.getMiniPublic(anyString(), any())).thenReturn(Collections.singletonList(location));
            mockMiniMapping();

            mockMvc.perform(get("/locations/public/mini/abc-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Warehouse A"));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            mockCurrentUser(clientUser);
            doNothing().when(locationService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Deleted successfully"));
        }
    }
}