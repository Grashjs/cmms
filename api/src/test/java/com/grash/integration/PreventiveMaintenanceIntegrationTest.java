package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenancePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.service.PreventiveMaintenanceService;
import com.grash.service.WebhookDispatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.grash.utils.Consts.usageBasedFreeLimits;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class PreventiveMaintenanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CompanySettingsRepository companySettingsRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private GeneralPreferencesRepository generalPreferencesRepository;
    @Autowired
    private EntityManager em;
    @Autowired
    private PreventiveMaintenanceService preventiveMaintenanceService;

    @MockitoBean
    private WebhookDispatchService webhookDispatchService;

    private Company company;
    private User user;
    private Role adminRole;

    @BeforeEach
    void setUpBase() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("Test Plan")
                .monthlyCostPerUser(10.0)
                .yearlyCostPerUser(100.0)
                .features(new HashSet<>(Collections.singletonList(PlanFeatures.PREVENTIVE_MAINTENANCE)))
                .build();
        plan = subscriptionPlanRepository.save(plan);

        Subscription subscription = Subscription.builder()
                .usersCount(5)
                .subscriptionPlan(plan)
                .build();
        subscription = subscriptionRepository.save(subscription);

        CompanySettings settings = new CompanySettings();
        settings = companySettingsRepository.save(settings);

        company = new Company("TestCompany", 10, subscription);
        company.setCompanySettings(settings);
        company = companyRepository.save(company);

        settings.setCompany(company);
        companySettingsRepository.save(settings);

        GeneralPreferences gp = settings.getGeneralPreferences();
        gp.setCurrency(currencyRepository.findFirstBy().get());
        gp.setDateFormat(DateFormat.MMDDYY);
        gp.setTimeZone("UTC");
        generalPreferencesRepository.save(gp);

        Set<PermissionEntity> pmPermissions = new HashSet<>(Collections.singletonList(
                PermissionEntity.PREVENTIVE_MAINTENANCES));

        adminRole = Role.builder()
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .companySettings(settings)
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .viewPermissions(new HashSet<>(pmPermissions))
                .viewOtherPermissions(new HashSet<>(pmPermissions))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .build();
        adminRole = roleRepository.save(adminRole);

        user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test.com");
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setRole(adminRole);
        user.setCompany(company);
        user.setEnabled(true);
        user.setSuperAccountRelations(new ArrayList<>());
        user.setUserSettings(new UserSettings());
        user = userRepository.save(user);

        setCurrentUser(user);
    }

    private PreventiveMaintenancePostDTO buildPostDTO(String name) {
        PreventiveMaintenancePostDTO dto = new PreventiveMaintenancePostDTO();
        dto.setName(name);
        dto.setTitle(name + " Title");
        dto.setStartsOn(new Date());
        dto.setFrequency(1);
        dto.setRecurrenceType(RecurrenceType.DAILY);
        dto.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
        dto.setDaysOfWeek(new ArrayList<>());
        dto.setCustomFields(new ArrayList<>());
        dto.setEstimatedDuration(1.0);
        dto.setPriority(Priority.NONE);
        dto.setAssignedTo(new ArrayList<>());
        dto.setCustomers(new ArrayList<>());
        dto.setFiles(new ArrayList<>());
        return dto;
    }

    private Role createRole(String name, RoleCode code, Set<PermissionEntity> viewPermissions) {
        Role role = Role.builder()
                .name(name)
                .roleType(RoleType.ROLE_CLIENT)
                .code(code)
                .companySettings(company.getCompanySettings())
                .createPermissions(new HashSet<>())
                .viewPermissions(viewPermissions)
                .viewOtherPermissions(new HashSet<>())
                .editOtherPermissions(new HashSet<>())
                .deleteOtherPermissions(new HashSet<>())
                .build();
        return roleRepository.save(role);
    }

    private User createCompanyUser(String email, Role role) {
        User u = new User();
        u.setFirstName("First");
        u.setLastName("Last");
        u.setEmail(email);
        u.setUsername(email);
        u.setPassword("encoded");
        u.setRole(role);
        u.setCompany(company);
        u.setEnabled(true);
        u.setSuperAccountRelations(new ArrayList<>());
        u.setUserSettings(new UserSettings());
        return userRepository.save(u);
    }

    @Nested
    class CreateTests {

        @Test
        void create_persistsPMWithCustomIdAndSchedule() {
            PreventiveMaintenance result = preventiveMaintenanceService.create(buildPostDTO("Pump PM"), user);

            assertNotNull(result.getId());
            assertNotNull(result.getCustomId());
            assertTrue(result.getCustomId().startsWith("PM"));
            assertNotNull(result.getSchedule());

            em.clear();
            PreventiveMaintenance fromDb = preventiveMaintenanceRepository.findById(result.getId()).get();
            assertEquals("Pump PM", fromDb.getName());
            assertEquals(company.getId(), fromDb.getCompany().getId());
            assertNotNull(fromDb.getSchedule());
            assertEquals(RecurrenceType.DAILY, fromDb.getSchedule().getRecurrenceType());
        }

        @Test
        void create_sequentialCustomIds() {
            PreventiveMaintenance first = preventiveMaintenanceService.create(buildPostDTO("First"), user);
            PreventiveMaintenance second = preventiveMaintenanceService.create(buildPostDTO("Second"), user);

            assertNotNull(first.getCustomId());
            assertNotNull(second.getCustomId());
            assertTrue(first.getCustomId().startsWith("PM"));
            assertTrue(second.getCustomId().startsWith("PM"));
            assertNotEquals(first.getCustomId(), second.getCustomId());
        }

        @Test
        void create_scheduleFieldsPersistedFromDTO() {
            PreventiveMaintenancePostDTO dto = buildPostDTO("Custom Schedule");
            dto.setFrequency(7);
            dto.setDueDateDelay(3);
            dto.setRecurrenceType(RecurrenceType.WEEKLY);
            dto.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
            dto.setDaysOfWeek(new ArrayList<>(List.of(0, 2, 4)));

            PreventiveMaintenance result = preventiveMaintenanceService.create(dto, user);

            em.clear();
            Schedule fromDb = scheduleRepository.findById(result.getSchedule().getId()).get();
            assertEquals(7, fromDb.getFrequency());
            assertEquals(3, fromDb.getDueDateDelay());
            assertEquals(RecurrenceType.WEEKLY, fromDb.getRecurrenceType());
            assertEquals(List.of(0, 2, 4), fromDb.getDaysOfWeek());
        }

        @Test
        void create_planLacksFeature_throwsForbidden() {
            SubscriptionPlan basicPlan = SubscriptionPlan.builder()
                    .name("Basic")
                    .monthlyCostPerUser(5.0)
                    .yearlyCostPerUser(50.0)
                    .features(new HashSet<>())
                    .build();
            basicPlan = subscriptionPlanRepository.save(basicPlan);

            Subscription basicSub = Subscription.builder()
                    .usersCount(5)
                    .subscriptionPlan(basicPlan)
                    .build();
            basicSub = subscriptionRepository.save(basicSub);

            company.setSubscription(basicSub);
            companyRepository.save(company);

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.create(buildPostDTO("No Feature"), user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_exceedsUsageLimit_throwsForbidden() {
            int limit = usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_PM_SCHEDULES);
            for (int i = 0; i < limit; i++) {
                preventiveMaintenanceService.create(buildPostDTO("Limit " + i), user);
            }

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.create(buildPostDTO("Over Limit"), user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class QueryTests {

        @Test
        void findByCompany_returnsAllForCompany() {
            preventiveMaintenanceService.create(buildPostDTO("Query A"), user);
            preventiveMaintenanceService.create(buildPostDTO("Query B"), user);

            Collection<PreventiveMaintenance> result = preventiveMaintenanceService.findByCompany(company.getId());

            assertTrue(result.size() >= 2);
            assertTrue(result.stream().allMatch(pm -> pm.getCompany().getId().equals(company.getId())));
        }

        @Test
        void findByIdAndCompany_scopedToCompany() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Scoped"), user);

            Optional<PreventiveMaintenance> found = preventiveMaintenanceService.findByIdAndCompany(pm.getId(),
                    company.getId());
            assertTrue(found.isPresent());
            assertEquals(pm.getId(), found.get().getId());

            Optional<PreventiveMaintenance> notFound = preventiveMaintenanceService.findByIdAndCompany(pm.getId(),
                    99999L);
            assertFalse(notFound.isPresent());
        }

        @Test
        void findByIdsAndCompany_multipleIds() {
            PreventiveMaintenance p1 = preventiveMaintenanceService.create(buildPostDTO("Batch 1"), user);
            PreventiveMaintenance p2 = preventiveMaintenanceService.create(buildPostDTO("Batch 2"), user);

            List<PreventiveMaintenance> result = preventiveMaintenanceService.findByIdsAndCompany(
                    List.of(p1.getId(), p2.getId()), company.getId());

            assertEquals(2, result.size());
        }

        @Test
        void findByCompanyForExport_eagerlyFetchesSchedule() {
            preventiveMaintenanceService.create(buildPostDTO("Export PM"), user);

            em.clear();
            Page<PreventiveMaintenance> result = preventiveMaintenanceService.findByCompanyForExport(
                    company.getId(), PageRequest.of(0, 10));

            assertFalse(result.isEmpty());
            PreventiveMaintenance fetched = result.getContent().stream()
                    .filter(pm -> "Export PM".equals(pm.getName()))
                    .findFirst()
                    .orElseThrow();
            assertNotNull(fetched.getSchedule());
        }

        @Test
        void getAll_returnsAll() {
            preventiveMaintenanceService.create(buildPostDTO("All 1"), user);

            Collection<PreventiveMaintenance> result = preventiveMaintenanceService.getAll();

            assertTrue(result.size() >= 1);
        }
    }

    @Nested
    class AccessTests {

        @Test
        void getById_ownerCanView() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Viewable"), user);

            PreventiveMaintenance result = preventiveMaintenanceService.getById(pm.getId(), user);

            assertEquals(pm.getId(), result.getId());
        }

        @Test
        void getById_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.getById(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void getById_viewDenied_throwsForbidden() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Hidden"), user);

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY, new HashSet<>());
            User other = createCompanyUser("noview@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.getById(pm.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class PatchTests {

        @Test
        void patch_updatesAndPersists() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Original"), user);

            PreventiveMaintenancePatchDTO dto = new PreventiveMaintenancePatchDTO();
            dto.setName("Updated PM");
            dto.setTitle("Updated Title");
            dto.setCustomFields(new ArrayList<>());

            PreventiveMaintenance result = preventiveMaintenanceService.patch(pm.getId(), dto, user);

            assertEquals("Updated PM", result.getName());

            em.clear();
            PreventiveMaintenance fromDb = preventiveMaintenanceRepository.findById(pm.getId()).get();
            assertEquals("Updated PM", fromDb.getName());
            assertEquals("Updated Title", fromDb.getTitle());
            assertFalse(fromDb.getSchedule().isDisabled());
        }

        @Test
        void patch_notFound_throwsNotFound() {
            PreventiveMaintenancePatchDTO dto = new PreventiveMaintenancePatchDTO();
            dto.setCustomFields(new ArrayList<>());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.patch(99999L, dto, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void patch_notEditor_throwsForbidden() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Protected"), user);

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)));
            User other = createCompanyUser("noedit@test.com", limitedRole);
            setCurrentUser(other);

            PreventiveMaintenancePatchDTO dto = new PreventiveMaintenancePatchDTO();
            dto.setName("Nope");
            dto.setCustomFields(new ArrayList<>());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.patch(pm.getId(), dto, other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteByIdAndUser_removesFromDB() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Delete Me"), user);
            Long id = pm.getId();

            preventiveMaintenanceService.deleteByIdAndUser(id, user);

            em.flush();
            em.clear();
            assertFalse(preventiveMaintenanceRepository.findById(id).isPresent());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.deleteByIdAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_notCreator_throwsForbidden() {
            PreventiveMaintenance pm = preventiveMaintenanceService.create(buildPostDTO("Protected"), user);

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)));
            User other = createCompanyUser("nodelete@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.deleteByIdAndUser(pm.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class SearchTests {

        @Test
        void findBySearchCriteriaWithEntityGraph_filtersByName() {
            preventiveMaintenanceService.create(buildPostDTO("Pump Alpha"), user);
            preventiveMaintenanceService.create(buildPostDTO("HVAC Beta"), user);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("name")
                    .value("Pump")
                    .operation("cn")
                    .values(new ArrayList<>())
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(org.springframework.data.domain.Sort.Direction.DESC);

            Page<PreventiveMaintenance> result =
                    preventiveMaintenanceService.findBySearchCriteriaWithEntityGraph(criteria);

            assertEquals(1, result.getTotalElements());
            assertEquals("Pump Alpha", result.getContent().get(0).getName());
        }

        @Test
        void getSearchCriteria_clientRole_filtersByCompany() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(org.springframework.data.domain.Sort.Direction.DESC);

            SearchCriteria result = preventiveMaintenanceService.getSearchCriteria(user, criteria);

            boolean hasCompanyFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "company".equals(f.getField()) && f.getValue().equals(company.getId()));
            assertTrue(hasCompanyFilter);
        }

        @Test
        void getSearchCriteria_accessDenied_throwsForbidden() {
            Role noViewRole = createRole("No PM", RoleCode.VIEW_ONLY, new HashSet<>());
            User noViewUser = createCompanyUser("nosearch@test.com", noViewRole);
            setCurrentUser(noViewUser);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(org.springframework.data.domain.Sort.Direction.DESC);

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.getSearchCriteria(noViewUser, criteria));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }
}
