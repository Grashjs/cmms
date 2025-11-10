package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.TeamMiniDTO;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TeamMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.Team;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.TeamService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @InjectMocks
    private TeamController teamController;

    @Mock
    private TeamService teamService;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private UserService userService;

    @Mock
    private EntityManager em;

    @Mock
    private HttpServletRequest request;

    private OwnUser clientUser;
    private OwnUser adminUser;
    private Team team;
    private TeamShowDTO teamShowDTO;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        clientUser = new OwnUser();
        clientUser.setId(1L);
        clientUser.setCompany(company);
        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientUser.setRole(clientRole);

        adminUser = new OwnUser();
        adminUser.setId(2L);
        adminUser.setCompany(company);
        Role adminRole = new Role();
        adminRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        adminUser.setRole(adminRole);

        team = new Team();
        team.setId(1L);
        team.setCreatedBy(clientUser.getId());

        teamShowDTO = new TeamShowDTO();
        teamShowDTO.setId(1L);
    }

    private void grantPermissions(Role role, Set<PermissionEntity> view, Set<PermissionEntity> create, Set<PermissionEntity> deleteOther) {
        role.setViewPermissions(view);
        role.setCreatePermissions(create);
        role.setDeleteOtherPermissions(deleteOther);
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {
        @Test
        @DisplayName("search_asClientWithPermission_shouldFilterByCompanyAndSucceed")
        void search_asClientWithPermission_shouldFilterByCompanyAndSucceed() {
            grantPermissions(clientUser.getRole(), Collections.singleton(PermissionEntity.PEOPLE_AND_TEAMS), Collections.emptySet(), Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            Page<TeamShowDTO> page = new PageImpl<>(Collections.singletonList(teamShowDTO));
            when(teamService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            ResponseEntity<Page<TeamShowDTO>> response = teamController.search(new SearchCriteria(), request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            verify(teamService).findBySearchCriteria(any(SearchCriteria.class));
        }

        @Test
        @DisplayName("search_asClientWithoutPermission_shouldThrowForbidden")
        void search_asClientWithoutPermission_shouldThrowForbidden() {
            grantPermissions(clientUser.getRole(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> teamController.search(new SearchCriteria(), request));
            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("search_asAdmin_shouldNotFilterByCompanyAndSucceed")
        void search_asAdmin_shouldNotFilterByCompanyAndSucceed() {
            when(userService.whoami(request)).thenReturn(adminUser);
            Page<TeamShowDTO> page = new PageImpl<>(Collections.singletonList(teamShowDTO));
            when(teamService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            ResponseEntity<Page<TeamShowDTO>> response = teamController.search(new SearchCriteria(), request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(teamService).findBySearchCriteria(any(SearchCriteria.class));
        }
    }

    @Nested
    @DisplayName("Get Mini Tests")
    class GetMiniTests {
        @Test
        @DisplayName("getMini_asClient_shouldReturnMiniDTOs")
        void getMini_asClient_shouldReturnMiniDTOs() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findByCompany(clientUser.getCompany().getId())).thenReturn(Collections.singletonList(team));
            when(teamMapper.toMiniDto(team)).thenReturn(new TeamMiniDTO());

            Collection<TeamMiniDTO> result = teamController.getMini(request);

            assertEquals(1, result.size());
            verify(teamService).findByCompany(clientUser.getCompany().getId());
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {
        @Test
        @DisplayName("getById_whenTeamExists_shouldReturnTeamShowDTO")
        void getById_whenTeamExists_shouldReturnTeamShowDTO() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findById(1L)).thenReturn(Optional.of(team));
            when(teamMapper.toShowDto(team)).thenReturn(teamShowDTO);

            TeamShowDTO result = teamController.getById(1L, request);

            assertNotNull(result);
            assertEquals(team.getId(), result.getId());
        }

        @Test
        @DisplayName("getById_whenTeamDoesNotExist_shouldThrowNotFound")
        void getById_whenTeamDoesNotExist_shouldThrowNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> teamController.getById(1L, request));
            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {
        @Test
        @DisplayName("create_asClientWithPermission_shouldCreateAndNotify")
        void create_asClientWithPermission_shouldCreateAndNotify() {
            grantPermissions(clientUser.getRole(), Collections.emptySet(), Collections.singleton(PermissionEntity.PEOPLE_AND_TEAMS), Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.create(any(Team.class))).thenReturn(team);
            when(teamMapper.toShowDto(team)).thenReturn(teamShowDTO);

            TeamShowDTO result = teamController.create(new Team(), request);

            assertNotNull(result);
            verify(teamService).create(any(Team.class));
            verify(teamService).notify(any(Team.class), any());
        }

        @Test
        @DisplayName("create_asClientWithoutPermission_shouldThrowForbidden")
        void create_asClientWithoutPermission_shouldThrowForbidden() {
            grantPermissions(clientUser.getRole(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> teamController.create(new Team(), request));
            assertEquals("Access denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {
        @Test
        @DisplayName("patch_whenTeamExists_shouldUpdateAndNotify")
        void patch_whenTeamExists_shouldUpdateAndNotify() {
            TeamPatchDTO patchDTO = new TeamPatchDTO();
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findById(1L)).thenReturn(Optional.of(team));
            when(teamService.update(eq(1L), any(TeamPatchDTO.class))).thenReturn(team);
            when(teamMapper.toShowDto(team)).thenReturn(teamShowDTO);

            TeamShowDTO result = teamController.patch(patchDTO, 1L, request);

            assertNotNull(result);
            verify(em).detach(team);
            verify(teamService).update(eq(1L), any(TeamPatchDTO.class));
            verify(teamService).patchNotify(any(Team.class), any(Team.class), any());
        }

        @Test
        @DisplayName("patch_whenTeamDoesNotExist_shouldThrowNotFound")
        void patch_whenTeamDoesNotExist_shouldThrowNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> teamController.patch(new TeamPatchDTO(), 1L, request));
            assertEquals("Team not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {
        @Test
        @DisplayName("delete_whenTeamDoesNotExist_shouldThrowNotFound")
        void delete_whenTeamDoesNotExist_shouldThrowNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> teamController.delete(1L, request));
            assertEquals("Team not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("delete_asNonCreatorWithoutDeleteOtherPermission_shouldThrowForbidden")
        void delete_asNonCreatorWithoutDeleteOtherPermission_shouldThrowForbidden() {
            OwnUser anotherUser = new OwnUser();
            anotherUser.setId(99L);
            anotherUser.setRole(new Role());
            anotherUser.getRole().setRoleType(RoleType.ROLE_CLIENT);
            grantPermissions(anotherUser.getRole(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

            when(userService.whoami(request)).thenReturn(anotherUser);
            when(teamService.findById(1L)).thenReturn(Optional.of(team));

            CustomException exception = assertThrows(CustomException.class, () -> teamController.delete(1L, request));
            assertEquals("Forbidden", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("delete_asCreator_shouldDeleteSuccessfully")
        void delete_asCreator_shouldDeleteSuccessfully() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(teamService.findById(1L)).thenReturn(Optional.of(team));

            ResponseEntity response = teamController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(teamService).delete(1L);
        }

        @Test
        @DisplayName("delete_asClientWithDeleteOtherPermission_shouldDeleteSuccessfully")
        void delete_asClientWithDeleteOtherPermission_shouldDeleteSuccessfully() {
            OwnUser anotherUser = new OwnUser();
            anotherUser.setId(99L);
            anotherUser.setRole(new Role());
            anotherUser.getRole().setRoleType(RoleType.ROLE_CLIENT);
            Set<PermissionEntity> deleteOther = new HashSet<>();
            deleteOther.add(PermissionEntity.PEOPLE_AND_TEAMS);
            grantPermissions(anotherUser.getRole(), Collections.emptySet(), Collections.emptySet(), deleteOther);

            when(userService.whoami(request)).thenReturn(anotherUser);
            when(teamService.findById(1L)).thenReturn(Optional.of(team));

            ResponseEntity response = teamController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(teamService).delete(1L);
        }
    }
}
