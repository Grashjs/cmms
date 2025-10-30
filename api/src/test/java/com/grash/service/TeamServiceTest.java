package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TeamMapper;
import com.grash.model.Company;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.Team;
import com.grash.model.enums.NotificationType;
import com.grash.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private CompanyService companyService;
    @Mock
    private TeamMapper teamMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EntityManager em;
    @Mock
    private MessageSource messageSource;
    @Mock
    private AssetService assetService;
    @Mock
    private LocationService locationService;
    @Mock
    private UserService userService;

    @InjectMocks
    private TeamService teamService;

    private Team team;
    private TeamPatchDTO teamPatchDTO;
    private Company company;
    private OwnUser user1;
    private OwnUser user2;
    private Locale locale;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        user1 = new OwnUser();
        user1.setId(10L);
        user1.setEmail("user1@example.com");

        user2 = new OwnUser();
        user2.setId(20L);
        user2.setEmail("user2@example.com");

        team = new Team();
        team.setId(1L);
        team.setName("Test Team");
        team.setCompany(company);
        team.setUsers(new ArrayList<>(Arrays.asList(user1, user2)));

        teamPatchDTO = new TeamPatchDTO();
        teamPatchDTO.setName("Updated Team");
        teamPatchDTO.setUsers(new HashSet<>(Collections.singletonList(user1)));

        locale = Locale.ENGLISH;

        // Inject lazy dependencies manually for testing
        teamService.setDeps(assetService, locationService);
    }

    @Nested
    @DisplayName("Team Creation Tests")
    class CreateTests {
        @Test
        @DisplayName("Should create a new team successfully")
        void create_success() {
            when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(team);
            doNothing().when(em).refresh(any(Team.class));

            Team createdTeam = teamService.create(team);

            assertNotNull(createdTeam);
            assertEquals(team.getName(), createdTeam.getName());
            verify(teamRepository, times(1)).saveAndFlush(team);
            verify(em, times(1)).refresh(team);
        }
    }

    @Nested
    @DisplayName("Team Update Tests")
    class UpdateTests {
        @Test
        @DisplayName("Should update an existing team successfully")
        void update_success() {
            when(teamRepository.existsById(anyLong())).thenReturn(true);
            when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));
            when(teamMapper.updateTeam(any(Team.class), any(TeamPatchDTO.class))).thenReturn(team);
            when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(team);
            doNothing().when(em).refresh(any(Team.class));

            Team updatedTeam = teamService.update(1L, teamPatchDTO);

            assertNotNull(updatedTeam);
            assertEquals(team.getName(), updatedTeam.getName());
            verify(teamRepository, times(1)).existsById(1L);
            verify(teamRepository, times(1)).findById(1L);
            verify(teamMapper, times(1)).updateTeam(team, teamPatchDTO);
            verify(teamRepository, times(1)).saveAndFlush(team);
            verify(em, times(1)).refresh(team);
        }

        @Test
        @DisplayName("Should throw CustomException when team to update is not found")
        void update_notFound() {
            when(teamRepository.existsById(anyLong())).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> teamService.update(1L, teamPatchDTO));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
            verify(teamRepository, times(1)).existsById(1L);
            verify(teamRepository, never()).findById(anyLong());
            verify(teamMapper, never()).updateTeam(any(Team.class), any(TeamPatchDTO.class));
            verify(teamRepository, never()).saveAndFlush(any(Team.class));
            verify(em, never()).refresh(any(Team.class));
        }
    }

    @Nested
    @DisplayName("Team Retrieval Tests")
    class RetrievalTests {
        @Test
        @DisplayName("Should return all teams")
        void getAll_success() {
            List<Team> teams = Arrays.asList(team, new Team());
            when(teamRepository.findAll()).thenReturn(teams);

            Collection<Team> result = teamService.getAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(teamRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should find team by ID when present")
        void findById_found() {
            when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));

            Optional<Team> result = teamService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(team.getName(), result.get().getName());
            verify(teamRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return empty optional when team by ID is not found")
        void findById_notFound() {
            when(teamRepository.findById(anyLong())).thenReturn(Optional.empty());

            Optional<Team> result = teamService.findById(1L);

            assertFalse(result.isPresent());
            verify(teamRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should find teams by company ID")
        void findByCompany_success() {
            List<Team> teams = Arrays.asList(team);
            when(teamRepository.findByCompany_Id(anyLong())).thenReturn(teams);

            Collection<Team> result = teamService.findByCompany(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(teamRepository, times(1)).findByCompany_Id(1L);
        }

        @Test
        @DisplayName("Should find teams by user ID")
        void findByUser_success() {
            List<Team> teams = Arrays.asList(team);
            when(teamRepository.findByUsers_Id(anyLong())).thenReturn(teams);

            Collection<Team> result = teamService.findByUser(10L);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(teamRepository, times(1)).findByUsers_Id(10L);
        }

        @Test
        @DisplayName("Should find team by name ignore case and company ID")
        void findByNameIgnoreCaseAndCompany_success() {
            when(teamRepository.findByNameIgnoreCaseAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.of(team));

            Optional<Team> result = teamService.findByNameIgnoreCaseAndCompany("test team", 1L);

            assertTrue(result.isPresent());
            assertEquals(team.getName(), result.get().getName());
            verify(teamRepository, times(1)).findByNameIgnoreCaseAndCompany_Id("test team", 1L);
        }

        @Test
        @DisplayName("Should return empty optional when team by name ignore case and company ID is not found")
        void findByNameIgnoreCaseAndCompany_notFound() {
            when(teamRepository.findByNameIgnoreCaseAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.empty());

            Optional<Team> result = teamService.findByNameIgnoreCaseAndCompany("nonexistent team", 1L);

            assertFalse(result.isPresent());
            verify(teamRepository, times(1)).findByNameIgnoreCaseAndCompany_Id("nonexistent team", 1L);
        }
    }

    @Nested
    @DisplayName("Team Deletion Tests")
    class DeleteTests {
        @Test
        @DisplayName("Should delete a team successfully")
        void delete_success() {
            doNothing().when(teamRepository).deleteById(anyLong());

            teamService.delete(1L);

            verify(teamRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Team Notification Tests")
    class NotificationTests {
        @Test
        @DisplayName("Should send notifications for new team members")
        void notify_success() {
            lenient().when(messageSource.getMessage(eq("new_team"), any(), eq(locale))).thenReturn("New Team");
            lenient().when(messageSource.getMessage(eq("notification_team_added"), any(Object[].class), eq(locale))).thenReturn("Team Test Team added");
            doNothing().when(notificationService).createMultiple(anyList(), eq(true), anyString());

            teamService.notify(team, locale);

            verify(messageSource, times(1)).getMessage(eq("new_team"), any(), eq(locale));
            verify(messageSource, times(1)).getMessage(eq("notification_team_added"), any(Object[].class), eq(locale));
            verify(notificationService, times(1)).createMultiple(
                    argThat(notifications -> notifications.size() == 2 &&
                            notifications.stream().allMatch(n -> n.getNotificationType().equals(NotificationType.TEAM) && n.getResourceId().equals(team.getId()))),
                    eq(true), eq("New Team"));
        }

        @Test
        @DisplayName("Should not send notifications if team has no users")
        void notify_noUsers() {
            team.setUsers(new ArrayList<>());
            when(messageSource.getMessage(eq("new_team"), any(), eq(locale))).thenReturn("New Team");
            when(messageSource.getMessage(eq("notification_team_added"), any(Object[].class), eq(locale))).thenReturn("Team Test Team added");

            teamService.notify(team, locale);

            verify(messageSource, times(1)).getMessage(eq("new_team"), any(), eq(locale));
            verify(messageSource, times(1)).getMessage(eq("notification_team_added"), any(Object[].class), eq(locale));
            verify(notificationService, times(1)).createMultiple(argThat(List::isEmpty), eq(true), anyString());
        }

        @Test
        @DisplayName("Should send notifications for newly added team members during patch update")
        void patchNotify_success() {
            Team oldTeam = new Team();
            oldTeam.setId(1L);
            oldTeam.setName("Old Team");
            oldTeam.setUsers(new ArrayList<>(Collections.singletonList(user1)));

            Team newTeam = new Team();
            newTeam.setId(1L);
            newTeam.setName("New Team");
            newTeam.setUsers(new ArrayList<>(Arrays.asList(user1, user2)));

            when(messageSource.getMessage(eq("new_team"), any(), eq(locale))).thenReturn("New Team");
            when(messageSource.getMessage(eq("notification_team_added"), any(Object[].class), eq(locale))).thenReturn("Team New Team added");
            doNothing().when(notificationService).createMultiple(anyList(), eq(true), anyString());

            teamService.patchNotify(oldTeam, newTeam, locale);

            verify(messageSource, times(1)).getMessage(eq("new_team"), any(), eq(locale));
            verify(messageSource, times(1)).getMessage(eq("notification_team_added"), any(Object[].class), eq(locale));
            verify(notificationService, times(1)).createMultiple(
                    argThat(notifications -> notifications.size() == 1 &&
                            notifications.stream().allMatch(n -> n.getUser().equals(user2) && n.getNotificationType().equals(NotificationType.TEAM) && n.getResourceId().equals(newTeam.getId()))),
                    eq(true), eq("New Team"));
        }

        @Test
        @DisplayName("Should not send notifications if no new users are added during patch update")
        void patchNotify_noNewUsers() {
            Team oldTeam = new Team();
            oldTeam.setId(1L);
            oldTeam.setName("Old Team");
            oldTeam.setUsers(new ArrayList<>(Arrays.asList(user1, user2)));

            Team newTeam = new Team();
            newTeam.setId(1L);
            newTeam.setName("New Team");
            newTeam.setUsers(new ArrayList<>(Arrays.asList(user1, user2)));

            when(messageSource.getMessage(eq("new_team"), any(), eq(locale))).thenReturn("New Team");
            when(messageSource.getMessage(eq("notification_team_added"), any(Object[].class), eq(locale))).thenReturn("Team New Team added");

            teamService.patchNotify(oldTeam, newTeam, locale);

            verify(messageSource, times(1)).getMessage(eq("new_team"), any(), eq(locale));
            verify(messageSource, times(1)).getMessage(eq("notification_team_added"), any(Object[].class), eq(locale));
            verify(notificationService, times(1)).createMultiple(argThat(List::isEmpty), eq(true), anyString());
        }

        @Test
        @DisplayName("Should not send notifications if new team has no users during patch update")
        void patchNotify_newTeamNoUsers() {
            Team oldTeam = new Team();
            oldTeam.setId(1L);
            oldTeam.setName("Old Team");
            oldTeam.setUsers(new ArrayList<>(Arrays.asList(user1, user2)));

            Team newTeam = new Team();
            newTeam.setId(1L);
            newTeam.setName("New Team");
            newTeam.setUsers(new ArrayList<>());

            teamService.patchNotify(oldTeam, newTeam, locale);
        }
    }

    @Nested
    @DisplayName("Team in Company Check Tests")
    class IsTeamInCompanyTests {
        @Test
        @DisplayName("Should return true when team is in company and optional is true (team is null)")
        void isTeamInCompany_optionalTrue_teamNull() {
            assertTrue(teamService.isTeamInCompany(null, 1L, true));
        }

        @Test
        @DisplayName("Should return true when team is in company and optional is true (team found and matches company)")
        void isTeamInCompany_optionalTrue_teamFoundAndMatches() {
            when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));
            assertTrue(teamService.isTeamInCompany(team, 1L, true));
        }

        @Test
        @DisplayName("Should return false when team is in company and optional is true (team found but different company)")
        void isTeamInCompany_optionalTrue_teamFoundButDifferentCompany() {
            Company otherCompany = new Company();
            otherCompany.setId(2L);
            team.setCompany(otherCompany);
            when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));
            assertFalse(teamService.isTeamInCompany(team, 1L, true));
        }

        @Test
        @DisplayName("Should return false when team is in company and optional is true (team not found)")
        void isTeamInCompany_optionalTrue_teamNotFound() {
            when(teamRepository.findById(anyLong())).thenReturn(Optional.empty());
            assertFalse(teamService.isTeamInCompany(team, 1L, true));
        }

        @Test
        @DisplayName("Should return true when team is in company and optional is false (team found and matches company)")
        void isTeamInCompany_optionalFalse_teamFoundAndMatches() {
            when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));
            assertTrue(teamService.isTeamInCompany(team, 1L, false));
        }

        @Test
        @DisplayName("Should return false when team is in company and optional is false (team found but different company)")
        void isTeamInCompany_optionalFalse_teamFoundButDifferentCompany() {
            Company otherCompany = new Company();
            otherCompany.setId(2L);
            team.setCompany(otherCompany);
            when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));
            assertFalse(teamService.isTeamInCompany(team, 1L, false));
        }

        @Test
        @DisplayName("Should return false when team is in company and optional is false (team not found)")
        void isTeamInCompany_optionalFalse_teamNotFound() {
            when(teamRepository.findById(anyLong())).thenReturn(Optional.empty());
            assertFalse(teamService.isTeamInCompany(team, 1L, false));
        }
    }

    @Nested
    @DisplayName("Team Search Criteria Tests")
    class FindBySearchCriteriaTests {
        @Test
        @DisplayName("Should return a page of TeamShowDTOs based on search criteria")
        void findBySearchCriteria_success() {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setPageNum(0);
            searchCriteria.setPageSize(10);
            searchCriteria.setSortField("name");
            searchCriteria.setDirection(org.springframework.data.domain.Sort.Direction.ASC);
            searchCriteria.setFilterFields(Collections.emptyList());

            Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.Direction.ASC, "name");
            Page<Team> teamPage = new PageImpl<>(Collections.singletonList(team), pageable, 1);
            TeamShowDTO teamShowDTO = new TeamShowDTO();
            teamShowDTO.setId(team.getId());
            teamShowDTO.setName(team.getName());
            Page<TeamShowDTO> teamShowDTOPage = new PageImpl<>(Collections.singletonList(teamShowDTO), pageable, 1);

            lenient().when(((org.springframework.data.jpa.repository.JpaSpecificationExecutor<Team>) teamRepository).findAll(isNull(), any(Pageable.class))).thenReturn(teamPage);
            when(teamMapper.toShowDto(any(Team.class))).thenReturn(teamShowDTO);

            Page<TeamShowDTO> result = teamService.findBySearchCriteria(searchCriteria);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(teamShowDTO.getName(), result.getContent().get(0).getName());
            verify((org.springframework.data.jpa.repository.JpaSpecificationExecutor<Team>) teamRepository, times(1)).findAll(isNull(), eq(pageable));
        }
    }
}