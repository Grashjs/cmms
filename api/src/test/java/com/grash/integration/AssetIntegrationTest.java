package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetPostDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.*;
import com.grash.service.AssetDowntimeService;
import com.grash.service.AssetService;
import com.grash.service.WebhookDispatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.grash.utils.Consts.usageBasedFreeLimits;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@Transactional
class AssetIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private AssetDowntimeRepository assetDowntimeRepository;
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
    private LocationRepository locationRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private EntityManager em;
    @Autowired
    private AssetService assetService;
    @Autowired
    private AssetDowntimeService assetDowntimeService;

    @MockBean
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

        Set<PermissionEntity> viewPermissions = new HashSet<>(Arrays.asList(
                PermissionEntity.ASSETS, PermissionEntity.LOCATIONS, PermissionEntity.PARTS_AND_MULTIPARTS));

        adminRole = Role.builder()
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .companySettings(settings)
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
                .viewPermissions(new HashSet<>(viewPermissions))
                .viewOtherPermissions(new HashSet<>(viewPermissions))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)))
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

    private AssetPostDTO buildPostDTO(String name) {
        AssetPostDTO dto = new AssetPostDTO();
        dto.setName(name);
        return dto;
    }

    private AssetPatchDTO buildPatchDTO(String name) {
        AssetPatchDTO dto = new AssetPatchDTO();
        dto.setStatus(AssetStatus.OPERATIONAL);
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

    private Location createLocation(String name) {
        Location location = new Location();
        location.setName(name);
        location.setCompany(company);
        return locationRepository.saveAndFlush(location);
    }

    private AssetDowntime createDowntime(Asset asset, Date startsOn, long duration) {
        AssetDowntime downtime = new AssetDowntime();
        downtime.setAsset(asset);
        downtime.setCompany(company);
        downtime.setStartsOn(startsOn);
        downtime.setDuration(duration);
        return assetDowntimeRepository.saveAndFlush(downtime);
    }

    @Nested
    class CreateTests {

        @Test
        void createByUser_persistsAssetWithCustomId() {
            Asset result = assetService.createByUser(buildPostDTO("Pump"), user);

            assertNotNull(result.getId());
            assertNotNull(result.getCustomId());
            assertTrue(result.getCustomId().startsWith("A"));

            em.clear();
            Asset fromDb = assetRepository.findById(result.getId()).get();
            assertEquals("Pump", fromDb.getName());
            assertEquals(company.getId(), fromDb.getCompany().getId());
        }

        @Test
        void createByUser_sequentialCustomIds() {
            Asset first = assetService.createByUser(buildPostDTO("First"), user);
            Asset second = assetService.createByUser(buildPostDTO("Second"), user);

            assertNotNull(first.getCustomId());
            assertNotNull(second.getCustomId());
            assertTrue(first.getCustomId().startsWith("A"));
            assertTrue(second.getCustomId().startsWith("A"));
            assertNotEquals(first.getCustomId(), second.getCustomId());
        }

        @Test
        void createByUser_autoGeneratesBarcodeWhenConfigured() {
            GeneralPreferences gp = company.getCompanySettings().getGeneralPreferences();
            gp.setAutoGenerateAssetBarcode(true);
            generalPreferencesRepository.save(gp);

            Asset result = assetService.createByUser(buildPostDTO("Auto Barcode"), user);

            assertNotNull(result.getBarCode());
            assertFalse(result.getBarCode().isBlank());
        }

        @Test
        void createByUser_duplicateBarcode_throwsNotAcceptable() {
            AssetPostDTO dto1 = buildPostDTO("Barcode 1");
            dto1.setBarCode("BAR-123");
            assetService.createByUser(dto1, user);

            AssetPostDTO dto2 = buildPostDTO("Barcode 2");
            dto2.setBarCode("BAR-123");

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.createByUser(dto2, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void createByUser_duplicateNfcId_throwsNotAcceptable() {
            AssetPostDTO dto1 = buildPostDTO("NFC 1");
            dto1.setNfcId("NFC-001");
            assetService.createByUser(dto1, user);

            AssetPostDTO dto2 = buildPostDTO("NFC 2");
            dto2.setNfcId("NFC-001");

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.createByUser(dto2, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void createByUser_parentAssetWithoutLicense_throwsForbidden() {
            Asset parent = createAsset("Parent");

            AssetPostDTO dto = buildPostDTO("Child");
            dto.setParentAsset(parent);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.createByUser(dto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void createByUser_exceedsUsageLimit_throwsForbidden() {
            int limit = usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_ASSETS);
            for (int i = 0; i < limit; i++) {
                assetService.createByUser(buildPostDTO("Limit " + i), user);
            }

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.createByUser(buildPostDTO("Over Limit"), user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_dispatchesNewAssetWebhook() {
            assetService.createByUser(buildPostDTO("Webhook"), user);

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company),
                    eq(WebhookEvent.NEW_ASSET),
                    anyMap(),
                    eq("newAsset"),
                    any(),
                    isNull(), isNull(), isNull(), isNull(), isNull());
        }
    }

    @Nested
    class QueryTests {

        @Test
        void findByCompany_returnsAllForCompany() {
            createAsset("Query A");
            createAsset("Query B");

            Collection<Asset> result = assetService.findByCompany(company.getId());

            assertTrue(result.size() >= 2);
            assertTrue(result.stream().allMatch(a -> a.getCompany().getId().equals(company.getId())));
        }

        @Test
        void findByIdAndCompany_scopedToCompany() {
            Asset asset = createAsset("Scoped");

            Optional<Asset> found = assetService.findByIdAndCompany(asset.getId(), company.getId());
            assertTrue(found.isPresent());

            Optional<Asset> notFound = assetService.findByIdAndCompany(asset.getId(), 99999L);
            assertFalse(notFound.isPresent());
        }

        @Test
        void findByIdsAndCompany_multipleIds() {
            Asset a1 = createAsset("Batch 1");
            Asset a2 = createAsset("Batch 2");

            Collection<Asset> result = assetService.findByIdsAndCompany(
                    List.of(a1.getId(), a2.getId()), company.getId());

            assertEquals(2, result.size());
        }

        @Test
        void findByBarcodeAndCompany_returnsMatch() {
            Asset asset = createAsset("Barcode Query");
            asset.setBarCode("BC-777");
            assetRepository.saveAndFlush(asset);

            Optional<Asset> found = assetService.findByBarcodeAndCompany("BC-777", company.getId());
            assertTrue(found.isPresent());
            assertEquals(asset.getId(), found.get().getId());

            assertFalse(assetService.findByBarcodeAndCompany("MISSING", company.getId()).isPresent());
        }

        @Test
        void findByNfcIdAndCompany_returnsMatch() {
            Asset asset = createAsset("NFC Query");
            asset.setNfcId("NFC-777");
            assetRepository.saveAndFlush(asset);

            Optional<Asset> found = assetService.findByNfcIdAndCompany("NFC-777", company.getId());
            assertTrue(found.isPresent());
            assertEquals(asset.getId(), found.get().getId());

            assertFalse(assetService.findByNfcIdAndCompany("MISSING", company.getId()).isPresent());
        }

        @Test
        void findByCompanyForExport_eagerlyFetchesRelations() {
            Location location = createLocation("Export Building");

            Asset asset = createAsset("Export Asset");
            asset.setLocation(location);
            assetRepository.saveAndFlush(asset);

            em.clear();
            Page<Asset> result = assetService.findByCompanyForExport(
                    company.getId(), PageRequest.of(0, 10));

            assertFalse(result.isEmpty());
            Asset fetched = result.getContent().get(0);
            assertNotNull(fetched.getLocation());
            assertEquals(location.getId(), fetched.getLocation().getId());
        }

        @Test
        void getTotalAcquisitionCost_sumsCostsBeforeDate() {
            Asset costed1 = createAsset("Cost 1");
            costed1.setAcquisitionCost(500.0);
            assetRepository.saveAndFlush(costed1);

            Asset costed2 = createAsset("Cost 2");
            costed2.setAcquisitionCost(300.0);
            assetRepository.saveAndFlush(costed2);

            createAsset("No Cost");

            double total = assetService.getTotalAcquisitionCost(
                    company.getId(), new Date(System.currentTimeMillis() + 86400000));

            assertEquals(800.0, total, 0.001);
        }

        @Test
        void findByCompanyAndParentAssetNull_onlyTopLevelAssets() {
            Asset parent = createAsset("Parent");
            Asset child = createAsset("Child");
            child.setParentAsset(parent);
            assetRepository.saveAndFlush(child);

            List<Asset> result = assetService.findByCompanyAndParentAssetNull(
                    company.getId(), Pageable.unpaged());

            assertTrue(result.stream().anyMatch(a -> a.getId().equals(parent.getId())));
            assertFalse(result.stream().anyMatch(a -> a.getId().equals(child.getId())));
        }

        @Test
        void findAssetChildren_paginated() {
            Asset parent = createAsset("Parent");
            Asset child = createAsset("Child");
            child.setParentAsset(parent);
            assetRepository.saveAndFlush(child);

            Page<Asset> result = assetService.findAssetChildren(parent.getId(), PageRequest.of(0, 10));

            assertEquals(1, result.getTotalElements());
            assertEquals(child.getId(), result.getContent().get(0).getId());
        }

        @Test
        void hasChildren_returnsTrueForParentWithChildren() {
            Asset parent = createAsset("Parent");
            assertFalse(assetService.hasChildren(parent.getId()));

            Asset child = createAsset("Child");
            child.setParentAsset(parent);
            assetRepository.saveAndFlush(child);

            assertTrue(assetService.hasChildren(parent.getId()));
        }

        @Test
        void getParentIdsWithChildren_identifiesParentsOnly() {
            Asset parent = createAsset("Parent");
            Asset child = createAsset("Child");
            child.setParentAsset(parent);
            assetRepository.saveAndFlush(child);

            Set<Long> result = assetService.getParentIdsWithChildren(List.of(parent.getId(), child.getId()));

            assertTrue(result.contains(parent.getId()));
            assertFalse(result.contains(child.getId()));
        }

        @Test
        void findByNameIgnoreCaseAndCompany_caseInsensitive() {
            createAsset("Compressor");

            List<Asset> result = assetService.findByNameIgnoreCaseAndCompany("compressor", company.getId());

            assertEquals(1, result.size());
            assertEquals("Compressor", result.get(0).getName());
        }
    }

    @Nested
    class AccessTests {

        @Test
        void checkAccessToAssetId_ownerCanView() {
            Asset asset = createAsset("Viewable");

            Asset result = assetService.checkAccessToAssetId(asset.getId(), user);

            assertEquals(asset.getId(), result.getId());
        }

        @Test
        void checkAccessToAssetId_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.checkAccessToAssetId(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void checkAccessToAssetId_viewDenied_throwsForbidden() {
            Asset asset = createAsset("Hidden");

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)));
            User other = createCompanyUser("other@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.checkAccessToAssetId(asset.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void findByLocationAndUser_returnsAssetsForLocation() {
            Location location = createLocation("Building A");

            Asset asset = createAsset("Located");
            asset.setLocation(location);
            assetRepository.saveAndFlush(asset);

            Collection<Asset> result = assetService.findByLocationAndUser(location.getId(), user);

            assertTrue(result.stream().anyMatch(a -> a.getId().equals(asset.getId())));
        }

        @Test
        void findByLocationAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findByLocationAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void findByPartAndUser_returnsAssetsWithPart() {
            Part part = new Part();
            part.setName("Filter");
            part.setCompany(company);
            part.setQuantity(10);
            part.setMinQuantity(1);
            part.setFiles(new ArrayList<>());
            part.setAssignedTo(new ArrayList<>());
            part.setCustomers(new ArrayList<>());
            part.setVendors(new ArrayList<>());
            part.setTeams(new ArrayList<>());
            part.setAssets(new ArrayList<>());
            part.setCustomFieldValues(new ArrayList<>());
            part = partRepository.saveAndFlush(part);

            Asset asset = createAsset("Part Asset");
            asset.getParts().add(part);
            part.getAssets().add(asset);
            assetRepository.saveAndFlush(asset);
            partRepository.saveAndFlush(part);

            em.clear();
            Collection<Asset> result = assetService.findByPartAndUser(part.getId(), user);

            assertTrue(result.stream().anyMatch(a -> a.getId().equals(asset.getId())));
        }

        @Test
        void findByPartAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findByPartAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void findChildren_root_returnsTopLevelAssets() {
            Asset parent = createAsset("Root");
            Asset child = createAsset("Child");
            child.setParentAsset(parent);
            assetRepository.saveAndFlush(child);

            List<Asset> result = assetService.findChildren(0L, user, Pageable.unpaged());

            assertTrue(result.stream().anyMatch(a -> a.getId().equals(parent.getId())));
            assertFalse(result.stream().anyMatch(a -> a.getId().equals(child.getId())));
        }

        @Test
        void findChildren_returnsChildAssets() {
            Asset parent = createAsset("Parent");
            Asset child = createAsset("Child");
            child.setParentAsset(parent);
            assetRepository.saveAndFlush(child);

            List<Asset> result = assetService.findChildren(parent.getId(), user, Pageable.unpaged());

            assertEquals(1, result.size());
            assertEquals(child.getId(), result.get(0).getId());
        }

        @Test
        void findChildren_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findChildren(99999L, user, Pageable.unpaged()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void findChildren_noViewPermission_throwsForbidden() {
            Role noViewRole = createRole("No Assets", RoleCode.VIEW_ONLY, new HashSet<>());
            User noViewUser = createCompanyUser("noview@test.com", noViewRole);
            setCurrentUser(noViewUser);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findChildren(1L, noViewUser, Pageable.unpaged()));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void findChildrenPaginated_paginatesChildren() {
            Asset parent = createAsset("Parent");
            Asset c1 = createAsset("Child 1");
            c1.setParentAsset(parent);
            assetRepository.saveAndFlush(c1);
            Asset c2 = createAsset("Child 2");
            c2.setParentAsset(parent);
            assetRepository.saveAndFlush(c2);

            Page<Asset> result = assetService.findChildrenPaginated(parent.getId(), user, PageRequest.of(0, 1));

            assertEquals(2, result.getTotalElements());
            assertEquals(1, result.getContent().size());
        }

        @Test
        void getByNfcIdAndCompany_withoutLicense_throwsForbidden() {
            Asset asset = createAsset("NFC");
            asset.setNfcId("NFC-SCAN");
            assetRepository.saveAndFlush(asset);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.getByNfcIdAndCompany("NFC-SCAN", user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class PatchTests {

        @Test
        void patch_updatesAndPersists() {
            Asset asset = createAsset("Original");

            AssetPatchDTO dto = buildPatchDTO("Updated");
            dto.setDescription("New description");

            Asset result = assetService.patch(asset.getId(), dto, user);

            assertEquals("Updated", result.getName());
            assertEquals("New description", result.getDescription());

            em.clear();
            Asset fromDb = assetRepository.findById(asset.getId()).get();
            assertEquals("Updated", fromDb.getName());
        }

        @Test
        void patch_notFound_throwsNotFound() {
            AssetPatchDTO dto = buildPatchDTO("Ghost");

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.patch(99999L, dto, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void patch_duplicateBarcode_throwsNotAcceptable() {
            Asset existing = createAsset("Existing");
            existing.setBarCode("DUP");
            assetRepository.saveAndFlush(existing);

            Asset target = createAsset("Target");

            AssetPatchDTO dto = buildPatchDTO("Target");
            dto.setBarCode("DUP");

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.patch(target.getId(), dto, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void patch_parentAssetCannotBeSelf_throwsNotAcceptable() {
            Asset asset = createAsset("Self");

            AssetPatchDTO dto = buildPatchDTO("Self");
            dto.setParentAsset(asset);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.patch(asset.getId(), dto, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void patch_statusToDown_createsDowntime() {
            Asset asset = createAsset("Go Down");

            AssetPatchDTO dto = buildPatchDTO("Go Down");
            dto.setStatus(AssetStatus.DOWN);

            assetService.patch(asset.getId(), dto, user);

            em.clear();
            Asset fromDb = assetRepository.findById(asset.getId()).get();
            assertEquals(AssetStatus.DOWN, fromDb.getStatus());

            Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId());
            assertEquals(1, downtimes.size());
            assertEquals(0, downtimes.iterator().next().getDuration());
        }

        @Test
        void patch_statusBackToOperational_stopsDowntime() throws Exception {
            Asset asset = createAsset("Recover");

            AssetPatchDTO downDto = buildPatchDTO("Recover");
            downDto.setStatus(AssetStatus.DOWN);
            assetService.patch(asset.getId(), downDto, user);

            Thread.sleep(1100);

            AssetPatchDTO upDto = buildPatchDTO("Recover");
            upDto.setStatus(AssetStatus.OPERATIONAL);
            assetService.patch(asset.getId(), upDto, user);

            em.clear();
            Asset fromDb = assetRepository.findById(asset.getId()).get();
            assertEquals(AssetStatus.OPERATIONAL, fromDb.getStatus());

            Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId());
            assertEquals(1, downtimes.size());
            assertTrue(downtimes.iterator().next().getDuration() > 0);
        }
    }

    @Nested
    class DowntimeTests {

        @Test
        void triggerDownTime_setsStatusAndCreatesDowntime() {
            Asset asset = createAsset("Down");

            assetService.triggerDownTime(asset.getId(), Locale.US, AssetStatus.DOWN);

            em.clear();
            Asset fromDb = assetRepository.findById(asset.getId()).get();
            assertEquals(AssetStatus.DOWN, fromDb.getStatus());

            Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId());
            assertEquals(1, downtimes.size());
            assertEquals(0, downtimes.iterator().next().getDuration());
        }

        @Test
        void stopDownTime_marksOperationalAndSetsDuration() {
            Asset asset = createAsset("Stop");

            AssetDowntime running = new AssetDowntime();
            running.setAsset(asset);
            running.setCompany(company);
            running.setStartsOn(new Date(System.currentTimeMillis() - 5000));
            running.setDuration(0);
            assetDowntimeRepository.saveAndFlush(running);
            em.clear();

            assetService.stopDownTime(asset.getId(), Locale.US);

            em.flush();
            em.clear();
            Asset fromDb = assetRepository.findById(asset.getId()).get();
            assertEquals(AssetStatus.OPERATIONAL, fromDb.getStatus());

            Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId());
            assertEquals(1, downtimes.size());
            assertTrue(downtimes.iterator().next().getDuration() >= 5);
        }

        @Test
        void getDowntime_returnsSumOfDurations() {
            Asset asset = createAsset("Sum Down");
            long now = System.currentTimeMillis();
            createDowntime(asset, new Date(now - 200000), 1000);
            createDowntime(asset, new Date(now - 100000), 2000);

            long total = assetService.getDowntime(asset.getId(),
                    new Date(0), new Date(now + 86400000));

            assertEquals(3000, total);
        }

        @Test
        void getMTBF_withMultipleDowntimes() {
            Asset asset = createAsset("MTBF");
            long day = 86400000L;
            Date base = new Date();
            createDowntime(asset, base, 86400);
            createDowntime(asset, new Date(base.getTime() + 3 * day), 86400);
            createDowntime(asset, new Date(base.getTime() + 6 * day), 86400);

            long mtbf = assetService.getMTBF(asset.getId(),
                    new Date(base.getTime() - day), new Date(base.getTime() + 10 * day));

            assertEquals(2, mtbf);
        }

        @Test
        void getUptime_returnsDurationMinusDowntime() {
            Asset asset = createAsset("Uptime");
            asset.setInServiceDate(new Date(System.currentTimeMillis() - 200000));
            assetRepository.saveAndFlush(asset);

            createDowntime(asset, new Date(System.currentTimeMillis() - 100000), 30);

            long uptime = assetService.getUptime(asset.getId(),
                    new Date(0), new Date(System.currentTimeMillis() + 86400000));

            assertEquals(170, uptime);
        }
    }

    @Nested
    class SearchTests {

        @Test
        void findBySearchCriteria_filtersByName() {
            createAsset("Pump Alpha");
            createAsset("HVAC Beta");

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

            Page<AssetShowDTO> result = assetService.findBySearchCriteria(criteria);

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

            SearchCriteria result = assetService.getSearchCriteria(user, criteria);

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

            SearchCriteria result = assetService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()) && f.getValue().equals(user.getId()));
            assertTrue(hasCreatedByFilter);
        }

        @Test
        void getSearchCriteria_accessDenied_throwsForbidden() {
            Role noViewRole = createRole("No Assets", RoleCode.VIEW_ONLY, new HashSet<>());
            User noViewUser = createCompanyUser("nosearch@test.com", noViewRole);
            setCurrentUser(noViewUser);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.getSearchCriteria(noViewUser, criteria));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteByIdAndUser_removesFromDB() {
            Asset asset = createAsset("Delete Me");
            Long id = asset.getId();

            assetService.deleteByIdAndUser(id, user);

            em.flush();
            em.clear();
            assertFalse(assetRepository.findById(id).isPresent());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.deleteByIdAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_notCreator_throwsForbidden() {
            Asset asset = createAsset("Protected");

            Role limitedRole = createRole("Limited", RoleCode.VIEW_ONLY,
                    new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)));
            User other = createCompanyUser("nodelete@test.com", limitedRole);
            setCurrentUser(other);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.deleteByIdAndUser(asset.getId(), other));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }
}
