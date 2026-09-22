package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.MeterMiniDTO;
import com.grash.dto.MeterShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterMapper;
import com.grash.model.Asset;
import com.grash.model.Company;
import com.grash.model.Meter;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserSettings;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.AssetService;
import com.grash.service.MeterService;
import com.grash.service.ReadingService;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

@WebMvcTest(MeterController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeterControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterService meterService;
    @MockitoBean
    private MeterMapper meterMapper;
    @MockitoBean
    private AssetService assetService;
    @MockitoBean
    private ReadingService readingService;
    @MockitoBean
    private UserService userService;

    private User clientUser;
    private User nonClientUser;
    private User restrictedUser;
    private Asset asset;
    private Meter meter;
    private MeterShowDTO showDto;
    private MeterMiniDTO miniDto;

    private static final String VALID_METER_BODY =
            "{\"name\":\"Meter1\",\"updateFrequency\":1,\"asset\":{\"id\":1}}";

    @BeforeEach
    void setUp() {
        Set<PermissionEntity> metersOnly = new HashSet<>(Collections.singletonList(PermissionEntity.METERS));
        Set<PermissionEntity> metersAndAssets = new HashSet<>(metersOnly);
        metersAndAssets.add(PermissionEntity.ASSETS);

        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(metersAndAssets)
                .createPermissions(metersOnly)
                .viewOtherPermissions(metersAndAssets)
                .editOtherPermissions(metersOnly)
                .deleteOtherPermissions(metersOnly)
                .build();

        Company company = new Company();
        company.setId(1L);

        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john@test.com");
        clientUser.setRole(clientRole);
        clientUser.setCompany(company);
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

        Role restrictedRole = Role.builder()
                .id(3L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Restricted Role")
                .viewPermissions(metersOnly)
                .build();

        restrictedUser = new User();
        restrictedUser.setId(3L);
        restrictedUser.setFirstName("Restricted");
        restrictedUser.setLastName("User");
        restrictedUser.setEmail("restricted@test.com");
        restrictedUser.setRole(restrictedRole);
        restrictedUser.setCompany(company);
        restrictedUser.setEnabled(true);

        asset = new Asset();
        asset.setId(1L);
        asset.setName("Compressor");
        asset.setCreatedBy(1L);

        meter = new Meter();
        meter.setId(1L);
        meter.setName("Meter1");
        meter.setUpdateFrequency(1);

        showDto = new MeterShowDTO();
        showDto.setId(1L);
        showDto.setName("Meter1");

        miniDto = new MeterMiniDTO();
        miniDto.setId(1L);
        miniDto.setName("Meter1");
    }

    private void mockCurrentUser(User user) {
        setCurrentUser(user);
        when(userService.whoami(any())).thenReturn(user);
    }

    private void mockShowMapping() {
        when(meterMapper.toShowDto(any(Meter.class), any(ReadingService.class))).thenReturn(showDto);
    }

    private void mockMiniMapping() {
        when(meterMapper.toMiniDto(any(Meter.class))).thenReturn(miniDto);
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(meterService.findBySearchCriteria(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/meters/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void getById_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.getById(1L, clientUser)).thenReturn(meter);
            mockShowMapping();

            mockMvc.perform(get("/meters/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByAsset_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findById(1L)).thenReturn(java.util.Optional.of(asset));
            when(meterService.findByAsset(1L)).thenReturn(Collections.singletonList(meter));
            mockShowMapping();

            mockMvc.perform(get("/meters/asset/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(post("/meters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_METER_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(patch("/meters/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMini_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(get("/meters/mini"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(delete("/meters/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.getById(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/meters/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.getById(1L, clientUser))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/meters/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.create(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/meters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_METER_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.patch(eq(99L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Meter not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/meters/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void patch_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.patch(eq(1L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN));

            mockMvc.perform(patch("/meters/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Meter not found", HttpStatus.NOT_FOUND))
                    .when(meterService).deleteByIdAndUser(99L, clientUser);

            mockMvc.perform(delete("/meters/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                    .when(meterService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/meters/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByAsset_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findById(99L)).thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/meters/asset/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getByAsset_accessDenied_returns403() throws Exception {
            mockCurrentUser(restrictedUser);
            Asset otherAsset = new Asset();
            otherAsset.setId(2L);
            otherAsset.setName("Other");
            otherAsset.setCreatedBy(99L);
            when(assetService.findById(2L)).thenReturn(java.util.Optional.of(otherAsset));

            mockMvc.perform(get("/meters/asset/2"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void search_returnsPage() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(meterService.findBySearchCriteria(any()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(meter)));
            mockShowMapping();

            mockMvc.perform(post("/meters/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Meter1"));
        }

        @Test
        void getById_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.getById(1L, clientUser)).thenReturn(meter);
            mockShowMapping();

            mockMvc.perform(get("/meters/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Meter1"));
        }

        @Test
        void create_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.create(any(), eq(clientUser))).thenReturn(meter);
            mockShowMapping();

            mockMvc.perform(post("/meters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_METER_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Meter1"));
        }

        @Test
        void patch_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.patch(eq(1L), any(), eq(clientUser))).thenReturn(meter);
            mockShowMapping();

            mockMvc.perform(patch("/meters/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Meter1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Meter1"));
        }

        @Test
        void getMini_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(meterService.findByCompany(1L)).thenReturn(Collections.singletonList(meter));
            mockMiniMapping();

            mockMvc.perform(get("/meters/mini"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Meter1"));
        }

        @Test
        void getByAsset_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findById(1L)).thenReturn(java.util.Optional.of(asset));
            when(meterService.findByAsset(1L)).thenReturn(Collections.singletonList(meter));
            mockShowMapping();

            mockMvc.perform(get("/meters/asset/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Meter1"));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            mockCurrentUser(clientUser);
            doNothing().when(meterService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/meters/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Deleted successfully"));
        }
    }
}