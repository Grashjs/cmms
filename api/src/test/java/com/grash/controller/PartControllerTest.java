package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PartMiniDTO;
import com.grash.dto.PartShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartMapper;
import com.grash.model.Part;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserSettings;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PartService;
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

import static com.grash.utils.Helper.setCurrentUser;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(PartController.class)
@AutoConfigureMockMvc(addFilters = false)
class PartControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartService partService;
    @MockitoBean
    private PartMapper partMapper;
    @MockitoBean
    private UserService userService;

    private User clientUser;
    private User nonClientUser;
    private Part part;
    private PartShowDTO showDto;
    private PartMiniDTO miniDto;

    @BeforeEach
    void setUp() {
        Role clientRole = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)))
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)))
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

        part = new Part();
        part.setId(1L);
        part.setName("Part1");

        showDto = new PartShowDTO();
        showDto.setId(1L);
        showDto.setName("Part1");

        miniDto = new PartMiniDTO();
        miniDto.setId(1L);
        miniDto.setName("Part1");
    }

    private void mockCurrentUser(User user) {
        setCurrentUser(user);
        when(userService.whoami(any())).thenReturn(user);
    }

    private void mockShowMapping() {
        when(partMapper.toShowDto(any(Part.class))).thenReturn(showDto);
    }

    private void mockMiniMapping() {
        when(partMapper.toMiniDto(any(Part.class))).thenReturn(miniDto);
    }

    @Nested
    class AuthorizationTests {

        @Test
        void search_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(partService.findBySearchCriteria(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(post("/parts/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void getById_permitAll_noAuthRequired() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getById(1L, clientUser)).thenReturn(part);
            mockShowMapping();

            mockMvc.perform(get("/parts/1"))
                    .andExpect(status().isOk());
        }

        @Test
        void create_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(post("/parts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Part\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void restock_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(post("/parts/1/restock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":5}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(patch("/parts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getMini_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(get("/parts/mini"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_requiresRoleClient() throws Exception {
            mockCurrentUser(nonClientUser);

            mockMvc.perform(delete("/parts/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ExceptionMapping {

        @Test
        void getById_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getById(99L, clientUser))
                    .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/parts/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getById_accessDenied_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getById(1L, clientUser))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/parts/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.create(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/parts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Part\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void create_duplicateBarcode_returns406() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.create(any(), eq(clientUser)))
                    .thenThrow(new CustomException("Part with same barcode exists", HttpStatus.NOT_ACCEPTABLE));

            mockMvc.perform(post("/parts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Part\",\"barcode\":\"BAR\"}"))
                    .andExpect(status().isNotAcceptable());
        }

        @Test
        void restock_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Part not found", HttpStatus.NOT_FOUND))
                    .when(partService).restock(eq(99L), any(), eq(clientUser));

            mockMvc.perform(post("/parts/99/restock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":5}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void restock_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                    .when(partService).restock(eq(1L), any(), eq(clientUser));

            mockMvc.perform(post("/parts/1/restock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":5}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.patch(eq(99L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Part not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/parts/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void patch_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.patch(eq(1L), any(), eq(clientUser)))
                    .thenThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN));

            mockMvc.perform(patch("/parts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Patched\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_notFound_returns404() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Part not found", HttpStatus.NOT_FOUND))
                    .when(partService).deleteByIdAndUser(99L, clientUser);

            mockMvc.perform(delete("/parts/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_forbidden_returns403() throws Exception {
            mockCurrentUser(clientUser);
            doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                    .when(partService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/parts/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class RoutingAndSerialization {

        @Test
        void search_returnsPage() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getSearchCriteria(any(), any())).thenReturn(new SearchCriteria());
            when(partService.findBySearchCriteria(any()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(part)));
            mockShowMapping();

            mockMvc.perform(post("/parts/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filterFields\":[],\"pageNum\":0,\"pageSize\":10,\"sortField\":\"id\"," +
                                    "\"direction\":\"ASC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Part1"));
        }

        @Test
        void getById_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getById(1L, clientUser)).thenReturn(part);
            mockShowMapping();

            mockMvc.perform(get("/parts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Part1"));
        }

        @Test
        void create_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.create(any(), eq(clientUser))).thenReturn(part);
            mockShowMapping();

            mockMvc.perform(post("/parts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Part1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Part1"));
        }

        @Test
        void restock_returnsSuccessResponse() throws Exception {
            mockCurrentUser(clientUser);
            doNothing().when(partService).restock(eq(1L), any(), eq(clientUser));

            mockMvc.perform(post("/parts/1/restock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":5,\"description\":\"Restock\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Restocked successfully"));
        }

        @Test
        void patch_returnsShowDto() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.patch(eq(1L), any(), eq(clientUser))).thenReturn(part);
            mockShowMapping();

            mockMvc.perform(patch("/parts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Part1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Part1"));
        }

        @Test
        void getMini_returnsCollection() throws Exception {
            mockCurrentUser(clientUser);
            when(partService.getMini(clientUser)).thenReturn(Collections.singletonList(part));
            mockMiniMapping();

            mockMvc.perform(get("/parts/mini"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Part1"));
        }

        @Test
        void delete_returnsSuccessResponse() throws Exception {
            mockCurrentUser(clientUser);
            doNothing().when(partService).deleteByIdAndUser(1L, clientUser);

            mockMvc.perform(delete("/parts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Deleted successfully"));
        }
    }
}
