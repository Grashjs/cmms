package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetMiniDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetMapper;
import com.grash.model.Asset;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserSettings;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.AssetService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssetControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;
    @MockitoBean
    private AssetMapper assetMapper;
    @MockitoBean
    private UserService userService;

    private User clientUser;
    private User nonClientUser;
    private Asset asset;
    private AssetShowDTO showDto;
    private AssetMiniDTO miniDto;

    @BeforeEach
    void setUp() {
        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
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

        asset = new Asset();
        asset.setId(1L);
        asset.setName("Compressor");

        showDto = new AssetShowDTO();
        showDto.setId(1L);
        showDto.setName("Compressor");
        showDto.setStatus(AssetStatus.OPERATIONAL);

        miniDto = new AssetMiniDTO();
        miniDto.setId(1L);
        miniDto.setName("Compressor");
    }

    private void mockCurrentUser(User user) {
        setCurrentUser(user);
        when(userService.whoami(any())).thenReturn(user);
    }

    private void mockShowMapping() {
        when(assetMapper.toShowDto(any(Asset.class), any(AssetService.class))).thenReturn(showDto);
        when(assetMapper.toShowDto(any(Asset.class), anySet())).thenReturn(showDto);
    }

    private void mockMiniMapping() {
        when(assetMapper.toMiniDto(any(Asset.class))).thenReturn(miniDto);
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(assetService.findBySearchCriteria(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/assets/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByNfcId_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getByNfcIdAndCompany(anyString(), any(User.class))).thenReturn(asset);
            mockMiniMapping();

            mockMvc.perform(get("/assets/nfc").param("nfcId", "NFC-001"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByBarcode_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getByBarcodeAndCompany(anyString(), any(User.class))).thenReturn(asset);
            mockMiniMapping();

            mockMvc.perform(get("/assets/barcode").param("data", "BAR-001"))
                    .andExpect(status().isOk());
        }

        @Test
        void getById_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.checkAccessToAssetId(1L, clientUser)).thenReturn(asset);
            mockShowMapping();

            mockMvc.perform(get("/assets/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByLocation_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findByLocationAndUser(1L, clientUser)).thenReturn(Collections.singletonList(asset));
            mockShowMapping();

            mockMvc.perform(get("/assets/location/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getByPart_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findByPartAndUser(1L, clientUser)).thenReturn(Collections.singletonList(asset));
            mockShowMapping();

            mockMvc.perform(get("/assets/part/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getChildren_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findChildren(eq(1L), any(User.class), any(Pageable.class)))
                    .thenReturn(Collections.singletonList(asset));
            mockShowMapping();

            mockMvc.perform(get("/assets/children/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void getChildrenPaginated_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findChildrenPaginated(eq(1L), any(User.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));
            mockShowMapping();

            mockMvc.perform(get("/assets/children/1/paginated"))
                    .andExpect(status().isOk());
        }

        @Test
        void getMiniPublic_noPreAuthorize_noAuthRequired() throws Exception {
            when(assetService.findMiniPublic(anyString(), isNull(), any())).thenReturn(Collections.singletonList(asset));
            mockMiniMapping();

            mockMvc.perform(get("/assets/public/mini/abc-123"))
                    .andExpect(status().isOk());
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(post("/assets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Asset\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(patch("/assets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMini_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(get("/assets/mini"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(delete("/assets/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.checkAccessToAssetId(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/assets/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.checkAccessToAssetId(1L, clientUser))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/assets/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByNfcId_licenseForbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getByNfcIdAndCompany(anyString(), any(User.class)))
                    .thenThrow(new CustomException("You need a license to scan an asset", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/assets/nfc").param("nfcId", "NFC-001"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByBarcode_licenseForbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getByBarcodeAndCompany(anyString(), any(User.class)))
                    .thenThrow(new CustomException("You need a license to scan an asset", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/assets/barcode").param("data", "BAR-001"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getByLocation_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findByLocationAndUser(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/assets/location/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getByPart_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findByPartAndUser(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/assets/part/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getChildren_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findChildren(eq(99L), any(User.class), any(Pageable.class)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/assets/children/99"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getChildrenPaginated_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findChildrenPaginated(eq(99L), any(User.class), any(Pageable.class)))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/assets/children/99/paginated"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void create_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.createByUser(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/assets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Asset\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.patch(eq(99L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Asset not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/assets/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void patch_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.patch(eq(1L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN));

            mockMvc.perform(patch("/assets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Asset not found", HttpStatus.NOT_FOUND))
                    .when(assetService).deleteByIdAndUser(99L, clientUser);

            mockMvc.perform(delete("/assets/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                    .when(assetService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/assets/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMiniPublic_rateLimited_returns429() throws Exception {
            when(assetService.findMiniPublic(anyString(), isNull(), any()))
                    .thenThrow(new CustomException("Rate limit exceeded. Try again later.",
                            HttpStatus.TOO_MANY_REQUESTS));

            mockMvc.perform(get("/assets/public/mini/abc-123"))
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void search_returnsPage() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(assetService.findBySearchCriteria(any()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(showDto)));

            mockMvc.perform(post("/assets/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Compressor"));
        }

        @Test
        void getById_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.checkAccessToAssetId(1L, clientUser)).thenReturn(asset);
            mockShowMapping();

            mockMvc.perform(get("/assets/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Compressor"));
        }

        @Test
        void getByNfcId_returnsMiniDto() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getByNfcIdAndCompany("NFC-001", clientUser)).thenReturn(asset);
            mockMiniMapping();

            mockMvc.perform(get("/assets/nfc").param("nfcId", "NFC-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Compressor"));
        }

        @Test
        void getByBarcode_returnsMiniDto() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.getByBarcodeAndCompany("BAR-001", clientUser)).thenReturn(asset);
            mockMiniMapping();

            mockMvc.perform(get("/assets/barcode").param("data", "BAR-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Compressor"));
        }

        @Test
        void getByLocation_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findByLocationAndUser(1L, clientUser)).thenReturn(Collections.singletonList(asset));
            mockShowMapping();

            mockMvc.perform(get("/assets/location/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Compressor"));
        }

        @Test
        void getByPart_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findByPartAndUser(1L, clientUser)).thenReturn(Collections.singletonList(asset));
            mockShowMapping();

            mockMvc.perform(get("/assets/part/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Compressor"));
        }

        @Test
        void getChildren_returnsList() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findChildren(eq(1L), any(User.class), any(Pageable.class)))
                    .thenReturn(Collections.singletonList(asset));
            mockShowMapping();

            mockMvc.perform(get("/assets/children/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Compressor"));
        }

        @Test
        void getChildrenPaginated_returnsPage() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findChildrenPaginated(eq(1L), any(User.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));
            mockShowMapping();

            mockMvc.perform(get("/assets/children/1/paginated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Compressor"));
        }

        @Test
        void create_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.createByUser(any(), eq(clientUser))).thenReturn(asset);
            mockShowMapping();

            mockMvc.perform(post("/assets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Compressor\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Compressor"));
        }

        @Test
        void patch_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.patch(eq(1L), any(), eq(clientUser))).thenReturn(asset);
            mockShowMapping();

            mockMvc.perform(patch("/assets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Compressor\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Compressor"));
        }

        @Test
        void getMini_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findMini(isNull(), eq(clientUser))).thenReturn(Collections.singletonList(asset));
            mockMiniMapping();

            mockMvc.perform(get("/assets/mini"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Compressor"));
        }

        @Test
        void getMini_withLocationId_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(assetService.findMini(1L, clientUser)).thenReturn(Collections.singletonList(asset));
            mockMiniMapping();

            mockMvc.perform(get("/assets/mini").param("locationId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Compressor"));
        }

        @Test
        void getMiniPublic_returnsCollection() throws Exception {
            when(assetService.findMiniPublic(eq("abc-123"), isNull(), any())).thenReturn(Collections.singletonList(asset));
            mockMiniMapping();

            mockMvc.perform(get("/assets/public/mini/abc-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Compressor"));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            mockCurrentUser(clientUser);
            doNothing().when(assetService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/assets/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Deleted successfully"));
        }
    }
}
