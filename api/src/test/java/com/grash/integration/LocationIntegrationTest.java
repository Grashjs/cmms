package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationPostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.*;
import com.grash.service.LocationService;
import com.grash.service.WebhookDispatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.grash.utils.Consts.usageBasedFreeLimits;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@Transactional
class LocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LocationRepository locationRepository;
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
    private LocationService locationService;

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
                .features(new HashSet<>())
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

        Set<PermissionEntity> locations = new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS));

        adminRole = Role.builder()
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .companySettings(settings)
                .createPermissions(new HashSet<>(locations))
                .viewPermissions(new HashSet<>(locations))
                .viewOtherPermissions(new HashSet<>(locations))
                .editOtherPermissions(new HashSet<>(locations))
                .deleteOtherPermissions(new HashSet<>(locations))
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

    private Location createLocation(String name) {
        Location location = new Location();
        location.setName(name);
        location.setCompany(company);
        location.setWorkers(new ArrayList<>());
        location.setTeams(new ArrayList<>());
        location.setCustomFieldValues(new ArrayList<>());
        return locationRepository.saveAndFlush(location);
    }

    private LocationPostDTO buildPostDTO(String name) {
        LocationPostDTO dto = new LocationPostDTO();
        dto.setName(name);
        dto.setCompany(company);
        return dto;
    }

    private LocationPatchDTO buildPatchDTO(String name) {
        LocationPatchDTO dto = new LocationPatchDTO();
        dto.setName(name);
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
        void create_persistsLocationWithCustomId() {
            Location result = locationService.create(buildPostDTO("Pump Room"), user);

            assertNotNull(result.getId());
            assertNotNull(result.getCustomId());
            assertTrue(result.getCustomId().startsWith("L"));

            em.clear();
            Location fromDb = locationRepository.findById(result.getId()).get();
            assertEquals("Pump Room", fromDb.getName());
            assertEquals(company.getId(), fromDb.getCompany().getId());
        }

        @Test
        void create_sequentialCustomIds() {
            Location first = locationService.create(buildPostDTO("First"), user);
            Location second = locationService.create(buildPostDTO("Second"), user);

            assertNotNull(first.getCustomId());
            assertNotNull(second.getCustomId());
            assertTrue(first.getCustomId().startsWith("L"));
            assertTrue(second.getCustomId().startsWith("L"));
            assertNotEquals(first.getCustomId(), second.getCustomId());
        }

        @Test
        void create_exceedsUsageLimit_throwsForbidden() {
            int limit = usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_LOCATIONS);
            for (int i = 0; i < limit; i++) {
                locationService.create(buildPostDTO("Limit " + i), user);
            }

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.create(buildPostDTO("Over Limit"), user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_dispatchesNewLocationWebhook() {
            locationService.create(buildPostDTO("Webhook"), user);

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company),
                    eq(WebhookEvent.NEW_LOCATION),
                    anyMap(),
                    eq("newLocation"),
                    any(),
                    isNull(), isNull(), isNull(), isNull(), isNull());
        }
    }

    @Nested
    class QueryTests {

        @Test
        void findByCompany_returnsAllForCompany() {
            createLocation("Query A");
            createLocation("Query B");

            Collection<Location> result = locationService.findByCompany(company.getId());

            assertTrue(result.size() >= 2);
            assertTrue(result.stream().allMatch(l -> l.getCompany().getId().equals(company.getId())));
        }

        @Test
        void findByIdAndCompany_scopedToCompany() {
            Location location = createLocation("Scoped");

            Optional<Location> found = locationService.findByIdAndCompany(location.getId(), company.getId());
            assertTrue(found.isPresent());

            Optional<Location> notFound = locationService.findByIdAndCompany(location.getId(), 99999L);
            assertFalse(notFound.isPresent());
        }

        @Test
        void findByIdsAndCompany_multipleIds() {
            Location l1 = createLocation("Batch 1");
            Location l2 = createLocation("Batch 2");

            List<Location> result = locationService.findByIdsAndCompany(
                    List.of(l1.getId(), l2.getId()), company.getId());

            assertEquals(2, result.size());
        }

        @Test
        void findByNameIgnoreCaseAndCompany_caseInsensitive() {
            createLocation("Compressor Room");

            List<Location> result = locationService.findByNameIgnoreCaseAndCompany("compressor room", company.getId());

            assertEquals(1, result.size());
            assertEquals("Compressor Room", result.get(0).getName());
        }

        @Test
        void findByCompanyForExport_returnsLocations() {
            createLocation("Export Building");

            Page<Location> result = locationService.findByCompanyForExport(
                    company.getId(), PageRequest.of(0, 10));

            assertFalse(result.isEmpty());
        }

        @Test
        void findByCompanyAndParentLocationNull_onlyTopLevelLocations() {
            Location parent = createLocation("Parent");
            Location child = createLocation("Child");
            child.setParentLocation(parent);
            locationRepository.saveAndFlush(child);

            Page<Location> result = locationService.findByCompany_IdAndParentLocationIsNull(
                    company.getId(), Pageable.unpaged());

            assertTrue(result.stream().anyMatch(l -> l.getId().equals(parent.getId())));
            assertFalse(result.stream().anyMatch(l -> l.getId().equals(child.getId())));
        }

        @Test
        void findLocationChildren_paginated() {
            Location parent = createLocation("Parent");
            Location child = createLocation("Child");
            child.setParentLocation(parent);
            locationRepository.saveAndFlush(child);

            Page<Location> result = locationService.findLocationChildren(parent.getId(), PageRequest.of(0, 10));

            assertEquals(1, result.getTotalElements());
            assertEquals(child.getId(), result.getContent().get(0).getId());
        }

        @Test
        void hasChildren_returnsTrueForParentWithChildren() {
            Location parent = createLocation("Parent");
            assertFalse(locationService.hasChildren(parent.getId()));

            Location child = createLocation("Child");
            child.setParentLocation(parent);
            locationRepository.saveAndFlush(child);

            assertTrue(locationService.hasChildren(parent.getId()));
        }
    }

    @Nested
    class AccessTests {

        @Test
        void getById_ownerCanView() {
            Location location = createLocation("Viewable");

            Location result = locationService.getById(location.getId(), user);

            assertEquals(location.getId(), result.getId());
        }

        @Test
        void getById_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getById(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void getById_viewDenied_throwsForbidden() {
            Location location = createLocation("Hidden");

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS)));
            User other = createCompanyUser("other@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getById(location.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getChildren_root_returnsTopLevelLocations() {
            Location parent = createLocation("Root");
            Location child = createLocation("Child");
            child.setParentLocation(parent);
            locationRepository.saveAndFlush(child);

            Collection<Location> result = locationService.getChildren(0L, user);

            assertTrue(result.stream().anyMatch(l -> l.getId().equals(parent.getId())));
            assertFalse(result.stream().anyMatch(l -> l.getId().equals(child.getId())));
        }

        @Test
        void getChildren_returnsChildLocations() {
            Location parent = createLocation("Parent");
            Location child = createLocation("Child");
            child.setParentLocation(parent);
            locationRepository.saveAndFlush(child);

            Collection<Location> result = locationService.getChildren(parent.getId(), user);

            assertEquals(1, result.size());
            assertEquals(child.getId(), result.iterator().next().getId());
        }

        @Test
        void getChildren_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getChildren(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void getChildren_noViewPermission_throwsForbidden() {
            Role noViewRole = createRole("No Locations", RoleCode.VIEW_ONLY, new HashSet<>());
            User noViewUser = createCompanyUser("noview@test.com", noViewRole);
            setCurrentUser(noViewUser);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getChildren(1L, noViewUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getChildrenPaginated_paginatesChildren() {
            Location parent = createLocation("Parent");
            Location c1 = createLocation("Child 1");
            c1.setParentLocation(parent);
            locationRepository.saveAndFlush(c1);
            Location c2 = createLocation("Child 2");
            c2.setParentLocation(parent);
            locationRepository.saveAndFlush(c2);

            Page<Location> result = locationService.getChildrenPaginated(parent.getId(), PageRequest.of(0, 1), user);

            assertEquals(2, result.getTotalElements());
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    class PatchTests {

        @Test
        void patch_updatesAndPersists() {
            Location location = createLocation("Original");

            LocationPatchDTO dto = buildPatchDTO("Updated");
            dto.setAddress("123 Main St");

            Location result = locationService.patch(location.getId(), dto, user);

            assertEquals("Updated", result.getName());
            assertEquals("123 Main St", result.getAddress());

            em.clear();
            Location fromDb = locationRepository.findById(location.getId()).get();
            assertEquals("Updated", fromDb.getName());
            assertEquals("123 Main St", fromDb.getAddress());
        }

        @Test
        void patch_notFound_throwsNotFound() {
            LocationPatchDTO dto = buildPatchDTO("Ghost");

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.patch(99999L, dto, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void patch_parentLocationCannotBeSelf_throwsNotAcceptable() {
            Location location = createLocation("Self");

            LocationPatchDTO dto = buildPatchDTO("Self");
            dto.setParentLocation(location);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.patch(location.getId(), dto, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void patch_notCreator_throwsForbidden() {
            Location location = createLocation("Protected");

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY, new HashSet<>());
            User other = createCompanyUser("noedit@test.com", limitedRole);
            setCurrentUser(other);

            LocationPatchDTO dto = buildPatchDTO("Hacked");

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.patch(location.getId(), dto, other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class SearchTests {

        @Test
        void findBySearchCriteria_filtersByName() {
            createLocation("Pump Alpha");
            createLocation("HVAC Beta");

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
            criteria.setDirection(Sort.Direction.DESC);

            Page<Location> result = locationService.findBySearchCriteria(criteria);

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
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = locationService.getSearchCriteria(user, criteria);

            boolean hasCompanyFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "company".equals(f.getField()) && f.getValue().equals(company.getId()));
            assertTrue(hasCompanyFilter);
        }

        @Test
        void getSearchCriteria_canViewOthersFalse_addsCreatedByFilter() {
            adminRole.getViewOtherPermissions().clear();
            roleRepository.save(adminRole);
            user.setRole(adminRole);
            userRepository.save(user);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = locationService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()) && f.getValue().equals(user.getId()));
            assertTrue(hasCreatedByFilter);
        }

        @Test
        void getSearchCriteria_accessDenied_throwsForbidden() {
            Role noViewRole = createRole("No Locations", RoleCode.VIEW_ONLY, new HashSet<>());
            User noViewUser = createCompanyUser("nosearch@test.com", noViewRole);
            setCurrentUser(noViewUser);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getSearchCriteria(noViewUser, criteria));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteByIdAndUser_removesFromDB() {
            Location location = createLocation("Delete Me");
            Long id = location.getId();

            locationService.deleteByIdAndUser(id, user);

            em.flush();
            em.clear();
            assertFalse(locationRepository.findById(id).isPresent());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.deleteByIdAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_notCreator_throwsForbidden() {
            Location location = createLocation("Protected");

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY, new HashSet<>());
            User other = createCompanyUser("nodelete@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.deleteByIdAndUser(location.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }
}