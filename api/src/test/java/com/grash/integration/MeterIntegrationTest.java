package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.MeterPatchDTO;
import com.grash.dto.imports.MeterImportDTO;
import com.grash.dto.MeterPostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.service.MeterService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.grash.utils.Consts.usageBasedFreeLimits;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = "license-key=")
@Transactional
class MeterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeterRepository meterRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private MeterCategoryRepository meterCategoryRepository;
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
    private MeterService meterService;

    private Company company;
    private User user;
    private Role adminRole;

    @BeforeEach
    void setUpBase() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("Test Plan")
                .monthlyCostPerUser(10.0)
                .yearlyCostPerUser(100.0)
                .features(new HashSet<>(Collections.singletonList(PlanFeatures.METER)))
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

        Set<PermissionEntity> viewPermissions = new HashSet<>(Arrays.asList(
                PermissionEntity.METERS, PermissionEntity.ASSETS, PermissionEntity.LOCATIONS));

        adminRole = Role.builder()
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .companySettings(settings)
                .createPermissions(new HashSet<>(Arrays.asList(PermissionEntity.METERS, PermissionEntity.ASSETS)))
                .viewPermissions(new HashSet<>(viewPermissions))
                .viewOtherPermissions(new HashSet<>(viewPermissions))
                .editOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.METERS, PermissionEntity.ASSETS)))
                .deleteOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.METERS, PermissionEntity.ASSETS)))
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

    private Asset createAsset(String name) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setCompany(company);
        asset.setStatus(AssetStatus.OPERATIONAL);
        asset.setCreatedBy(user.getId());
        asset.setFiles(new ArrayList<>());
        asset.setCustomers(new ArrayList<>());
        asset.setTeams(new ArrayList<>());
        asset.setParts(new ArrayList<>());
        asset.setCustomFieldValues(new ArrayList<>());
        return assetRepository.saveAndFlush(asset);
    }

    private Location createLocation(String name) {
        Location location = new Location();
        location.setName(name);
        location.setCompany(company);
        return locationRepository.saveAndFlush(location);
    }

    private MeterPostDTO buildPostDTO(String name, Asset asset) {
        MeterPostDTO dto = new MeterPostDTO();
        dto.setName(name);
        dto.setUnit("kWh");
        dto.setUpdateFrequency(1);
        dto.setAsset(asset);
        return dto;
    }

    private MeterPatchDTO buildPatchDTO(String name) {
        MeterPatchDTO dto = new MeterPatchDTO();
        dto.setName(name);
        dto.setUnit("L");
        dto.setUpdateFrequency(1);
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
        void create_persistsMeterWithRelations() {
            Asset asset = createAsset("Meter Asset");
            Location location = createLocation("Meter Location");
            MeterCategory category = new MeterCategory("Electric", company.getCompanySettings());
            category = meterCategoryRepository.save(category);
            User operator = createCompanyUser("operator@test.com", adminRole);

            MeterPostDTO dto = buildPostDTO("Energy Meter", asset);
            dto.setLocation(location);
            dto.setMeterCategory(category);
            dto.setUsers(List.of(operator));

            Meter result = meterService.create(dto, user);

            assertNotNull(result.getId());
            assertEquals("Energy Meter", result.getName());
            assertEquals("kWh", result.getUnit());
            assertEquals(1, result.getUpdateFrequency());

            em.clear();
            Meter fromDb = meterRepository.findById(result.getId()).get();
            assertEquals("Energy Meter", fromDb.getName());
            assertEquals(company.getId(), fromDb.getCompany().getId());
            assertEquals(asset.getId(), fromDb.getAsset().getId());
            assertEquals(location.getId(), fromDb.getLocation().getId());
            assertEquals(category.getId(), fromDb.getMeterCategory().getId());
            assertTrue(fromDb.getUsers().stream().anyMatch(u -> u.getId().equals(operator.getId())));
        }

        @Test
        void create_exceedsUsageLimit_throwsForbidden() {
            int limit = usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_METERS);
            Asset asset = createAsset("Limit Asset");
            for (int i = 0; i < limit; i++) {
                meterService.create(buildPostDTO("Limit " + i, asset), user);
            }

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.create(buildPostDTO("Over Limit", asset), user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_roleWithoutMeterCreatePermission_throwsForbidden() {
            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.METERS)));
            User other = createCompanyUser("nocreate@test.com", limitedRole);
            Asset asset = createAsset("Blocked Asset");

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.create(buildPostDTO("Blocked", asset), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void createByUser_requiresMeterPlanFeature_throwsForbidden() {
            SubscriptionPlan altPlan = SubscriptionPlan.builder()
                    .name("No Meter Plan")
                    .monthlyCostPerUser(10.0)
                    .yearlyCostPerUser(100.0)
                    .features(new HashSet<>())
                    .build();
            altPlan = subscriptionPlanRepository.save(altPlan);

            Subscription altSub = Subscription.builder()
                    .usersCount(2)
                    .subscriptionPlan(altPlan)
                    .build();
            altSub = subscriptionRepository.save(altSub);

            CompanySettings altSettings = new CompanySettings();
            altSettings = companySettingsRepository.save(altSettings);

            Company altCompany = new Company("AltCompany", 2, altSub);
            altCompany.setCompanySettings(altSettings);
            altCompany = companyRepository.save(altCompany);

            altSettings.setCompany(altCompany);
            companySettingsRepository.save(altSettings);

            Role altRole = Role.builder()
                    .name("Admin 2")
                    .roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.ADMIN)
                    .companySettings(altSettings)
                    .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.METERS)))
                    .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.METERS)))
                    .viewOtherPermissions(new HashSet<>())
                    .editOtherPermissions(new HashSet<>())
                    .deleteOtherPermissions(new HashSet<>())
                    .build();
            altRole = roleRepository.save(altRole);

            User altUser = new User();
            altUser.setFirstName("Alt");
            altUser.setLastName("User");
            altUser.setEmail("alt.user@test.com");
            altUser.setUsername("altuser");
            altUser.setPassword("encoded");
            altUser.setRole(altRole);
            altUser.setCompany(altCompany);
            altUser.setEnabled(true);
            altUser.setSuperAccountRelations(new ArrayList<>());
            altUser.setUserSettings(new UserSettings());
            setCurrentUser(altUser);
            altUser = userRepository.save(altUser);

            Asset altAsset = new Asset();
            altAsset.setName("Alt Asset");
            altAsset.setCompany(altCompany);
            altAsset.setStatus(AssetStatus.OPERATIONAL);
            altAsset.setCreatedBy(altUser.getId());
            altAsset.setFiles(new ArrayList<>());
            altAsset.setCustomers(new ArrayList<>());
            altAsset.setTeams(new ArrayList<>());
            altAsset.setParts(new ArrayList<>());
            altAsset.setCustomFieldValues(new ArrayList<>());
            altAsset = assetRepository.saveAndFlush(altAsset);

            Asset finalAltAsset = altAsset;
            User finalAltUser = altUser;
            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.create(buildPostDTO("No Plan Meter", finalAltAsset), finalAltUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class QueryTests {

        @Test
        void findByCompany_returnsAllForCompany() {
            Asset asset = createAsset("Query Asset");
            meterService.create(buildPostDTO("Query A", asset), user);
            meterService.create(buildPostDTO("Query B", asset), user);

            Collection<Meter> result = meterService.findByCompany(company.getId());

            assertTrue(result.size() >= 2);
            assertTrue(result.stream().allMatch(m -> m.getCompany().getId().equals(company.getId())));
        }

        @Test
        void findByIdAndCompany_scopedToCompany() {
            Meter meter = meterService.create(buildPostDTO("Scoped", createAsset("Scoped Asset")), user);

            Optional<Meter> found = meterService.findByIdAndCompany(meter.getId(), company.getId());
            assertTrue(found.isPresent());

            Optional<Meter> notFound = meterService.findByIdAndCompany(meter.getId(), 99999L);
            assertFalse(notFound.isPresent());
        }

        @Test
        void findByIdsAndCompany_returnsOnlyMatchingCompany() {
            Asset asset = createAsset("Batch Asset");
            Meter m1 = meterService.create(buildPostDTO("Batch 1", asset), user);
            Meter m2 = meterService.create(buildPostDTO("Batch 2", asset), user);

            List<Meter> result = meterService.findByIdsAndCompany(
                    List.of(m1.getId(), m2.getId()), company.getId());

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(m -> m.getCompany().getId().equals(company.getId())));
        }

        @Test
        void findByAsset_returnsMetersForAsset() {
            Asset asset = createAsset("Metered Asset");
            meterService.create(buildPostDTO("M1", asset), user);
            meterService.create(buildPostDTO("M2", asset), user);

            Collection<Meter> result = meterService.findByAsset(asset.getId());

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(m -> m.getAsset().getId().equals(asset.getId())));
        }

        @Test
        void findByAsset_unknownAsset_returnsEmpty() {
            Collection<Meter> result = meterService.findByAsset(99999L);

            assertTrue(result.isEmpty());
        }

        @Test
        void findByCompanyForExport_eagerlyFetchesRelations() {
            Asset asset = createAsset("Export Asset");
            Location location = createLocation("Export Location");
            MeterCategory category = new MeterCategory("Diesel", company.getCompanySettings());
            category = meterCategoryRepository.save(category);

            MeterPostDTO dto = buildPostDTO("Export Meter", asset);
            dto.setLocation(location);
            dto.setMeterCategory(category);
            Meter meter = meterService.create(dto, user);

            em.clear();
            Page<Meter> result = meterService.findByCompanyForExport(company.getId(), PageRequest.of(0, 10));

            assertFalse(result.isEmpty());
            Meter fetched = result.getContent().stream()
                    .filter(m -> m.getId().equals(meter.getId()))
                    .findFirst()
                    .get();
            assertNotNull(fetched.getMeterCategory());
            assertEquals(category.getId(), fetched.getMeterCategory().getId());
            assertEquals(location.getId(), fetched.getLocation().getId());
        }
    }

    @Nested
    class AccessTests {

        @Test
        void getById_ownerCanView() {
            Meter meter = meterService.create(buildPostDTO("Mine", createAsset("Mine Asset")), user);

            Meter result = meterService.getById(meter.getId(), user);

            assertEquals(meter.getId(), result.getId());
        }

        @Test
        void getById_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.getById(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void getById_viewDenied_throwsForbidden() {
            Meter meter = meterService.create(buildPostDTO("Hidden", createAsset("Hidden Asset")), user);

            Role noViewRole = createRole("No Meters", RoleCode.VIEW_ONLY, new HashSet<>());
            User other = createCompanyUser("hidden@test.com", noViewRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.getById(meter.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getById_viewOtherDisabled_throwsForbidden() {
            Meter meter = meterService.create(buildPostDTO("Private", createAsset("Private Asset")), user);

            Role selfOnlyRole = createRole("Self Only", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.METERS)));
            User other = createCompanyUser("selfonly@test.com", selfOnlyRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.getById(meter.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class SearchTests {

        @Test
        void findBySearchCriteria_filtersByName() {
            Asset asset = createAsset("Search Asset");
            meterService.create(buildPostDTO("Compressor Meter", asset), user);
            meterService.create(buildPostDTO("Boiler Meter", asset), user);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("name")
                    .value("Boiler")
                    .operation("cn")
                    .values(new ArrayList<>())
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            Page<Meter> result = meterService.findBySearchCriteria(criteria);

            assertEquals(1, result.getTotalElements());
            assertEquals("Boiler Meter", result.getContent().get(0).getName());
        }

        @Test
        void getSearchCriteria_clientRole_filtersByCompany() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = meterService.getSearchCriteria(user, criteria);

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

            SearchCriteria result = meterService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()) && f.getValue().equals(user.getId()));
            assertTrue(hasCreatedByFilter);
        }

        @Test
        void getSearchCriteria_accessDenied_throwsForbidden() {
            Role noViewRole = createRole("No Meters", RoleCode.VIEW_ONLY, new HashSet<>());
            User noViewUser = createCompanyUser("nosearch@test.com", noViewRole);
            setCurrentUser(noViewUser);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.getSearchCriteria(noViewUser, criteria));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class PatchTests {

        @Test
        void patch_updatesFieldsAndPersists() {
            Meter meter = meterService.create(buildPostDTO("Before Patch", createAsset("Patch Asset")), user);
            User operator = createCompanyUser("patched.op@test.com", adminRole);

            MeterPatchDTO dto = buildPatchDTO("After Patch");
            dto.setLocation(createLocation("Patch Location"));
            dto.setUsers(List.of(operator));

            Meter patched = meterService.patch(meter.getId(), dto, user);

            assertEquals("After Patch", patched.getName());
            assertEquals("L", patched.getUnit());

            em.clear();
            Meter fromDb = meterRepository.findById(meter.getId()).get();
            assertEquals("After Patch", fromDb.getName());
            assertEquals("L", fromDb.getUnit());
            assertEquals(1, fromDb.getUpdateFrequency());
            assertEquals(company.getId(), fromDb.getCompany().getId());
            assertTrue(fromDb.getUsers().stream().anyMatch(u -> u.getId().equals(operator.getId())));
        }

        @Test
        void patch_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.patch(99999L, buildPatchDTO("Ghost"), user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void patch_cannotEdit_throwsForbidden() {
            Meter meter = meterService.create(buildPostDTO("Protected", createAsset("Protected Asset")), user);

            Role limitedRole = createRole("Support", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.METERS)));
            User other = createCompanyUser("limited@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.patch(meter.getId(), buildPatchDTO("Hack"), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteByIdAndUser_removesFromDB() {
            Meter meter = meterService.create(buildPostDTO("Delete Me", createAsset("Delete Asset")), user);
            Long id = meter.getId();

            meterService.deleteByIdAndUser(id, user);

            em.flush();
            em.clear();
            assertFalse(meterRepository.findById(id).isPresent());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.deleteByIdAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_cannotDelete_throwsForbidden() {
            Meter meter = meterService.create(buildPostDTO("Locked", createAsset("Locked Asset")), user);

            Role limitedRole = createRole("No Delete", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.METERS)));
            User other = createCompanyUser("nodelete@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.deleteByIdAndUser(meter.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void delete_removesMeter_keepsAsset() {
            Asset asset = createAsset("Kept Asset");
            Meter meter = meterService.create(buildPostDTO("Temp", asset), user);
            Long assetId = asset.getId();

            meterService.delete(meter.getId());

            em.flush();
            em.clear();
            assertFalse(meterRepository.findById(meter.getId()).isPresent());
            assertTrue(assetRepository.findById(assetId).isPresent());
        }
    }

    @Nested
    class ImportTests {

        @Test
        void importMeter_setsRelations_andPersists() {
            Asset asset = createAsset("Import Machine");
            Location location = createLocation("Import Site");
            User operator = createCompanyUser("import.op@test.com", adminRole);

            MeterImportDTO dto = new MeterImportDTO();
            dto.setName("Imported Meter");
            dto.setUnit("h");
            dto.setUpdateFrequency(1);
            dto.setLocationName("Import Site");
            dto.setAssetName("Import Machine");
            dto.setMeterCategory("Fuel");
            dto.setUsersEmails(List.of("import.op@test.com"));

            Meter meter = new Meter();
            meterService.importMeter(meter, dto, company);

            assertNotNull(meter.getAsset());
            assertEquals(asset.getId(), meter.getAsset().getId());
            assertNotNull(meter.getLocation());
            assertEquals(location.getId(), meter.getLocation().getId());
            assertNotNull(meter.getMeterCategory());
            assertEquals("Fuel", meter.getMeterCategory().getName());
            assertTrue(meter.getUsers().stream().anyMatch(u -> u.getId().equals(operator.getId())));

            meterService.saveAll(List.of(meter));

            em.flush();
            em.clear();
            Meter fromDb = meterRepository.findById(meter.getId()).get();
            assertEquals("Imported Meter", fromDb.getName());
            assertEquals(company.getId(), fromDb.getCompany().getId());
            assertNotNull(fromDb.getMeterCategory());
            assertEquals("Fuel", fromDb.getMeterCategory().getName());
        }
    }
}