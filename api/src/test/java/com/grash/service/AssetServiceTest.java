package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetPostDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.imports.AssetImportDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.AssetRepository;
import com.grash.utils.Consts;
import io.github.bucket4j.Bucket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @InjectMocks
    private AssetService assetService;

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetCategoryService assetCategoryService;
    @Mock
    private UserService userService;
    @Mock
    private CustomerService customerService;
    @Mock
    private VendorService vendorService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TeamService teamService;
    @Mock
    private PartService partService;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private EntityManager em;
    @Mock
    private AssetDowntimeService assetDowntimeService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private CustomSequenceService customSequenceService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private RequestPortalService requestPortalService;
    @Mock
    private CustomFieldValueService customFieldValueService;
    @Mock
    private LocationService locationService;
    @Mock
    private LaborService laborService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private WebhookDispatchService webhookDispatchService;

    private Company company;
    private User user;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;
    private CompanySettings companySettings;
    private GeneralPreferences generalPreferences;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(assetService, "locationService", locationService);
        ReflectionTestUtils.setField(assetService, "laborService", laborService);
        ReflectionTestUtils.setField(assetService, "workOrderService", workOrderService);
        ReflectionTestUtils.setField(assetService, "webhookDispatchService", webhookDispatchService);

        subscriptionPlan = SubscriptionPlan.builder()
                .id(1L)
                .name("Pro")
                .features(new HashSet<>())
                .build();
        subscription = Subscription.builder()
                .id(1L)
                .subscriptionPlan(subscriptionPlan)
                .build();
        companySettings = new CompanySettings();
        companySettings.setId(1L);
        generalPreferences = new GeneralPreferences(companySettings);
        companySettings.setGeneralPreferences(generalPreferences);
        company = new Company("TestCo", 10, subscription);
        company.setId(1L);
        company.setCompanySettings(companySettings);

        role = adminRole();

        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");
        user.setRole(role);
        user.setCompany(company);
        user.setEnabled(true);
        user.setSuperAccountRelations(new ArrayList<>());
        user.setUserSettings(new UserSettings());
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private Role buildRole(RoleType roleType, Set<PermissionEntity> view, Set<PermissionEntity> viewOther,
                           Set<PermissionEntity> create, Set<PermissionEntity> editOther,
                           Set<PermissionEntity> deleteOther) {
        return Role.builder()
                .id(1L)
                .name("Role")
                .roleType(roleType)
                .code(RoleCode.ADMIN)
                .viewPermissions(view)
                .viewOtherPermissions(viewOther)
                .createPermissions(create)
                .editOtherPermissions(editOther)
                .deleteOtherPermissions(deleteOther)
                .build();
    }

    private Role adminRole() {
        Set<PermissionEntity> all = new HashSet<>(Arrays.asList(
                PermissionEntity.ASSETS, PermissionEntity.LOCATIONS, PermissionEntity.PARTS_AND_MULTIPARTS));
        return buildRole(RoleType.ROLE_CLIENT, all, all, all, all, all);
    }

    private Role restrictedRole() {
        return buildRole(RoleType.ROLE_CLIENT, new HashSet<>(), new HashSet<>(), new HashSet<>(),
                new HashSet<>(), new HashSet<>());
    }

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("U" + id);
        u.setLastName("L" + id);
        u.setEmail("u" + id + "@test.com");
        u.setRole(role);
        u.setCompany(company);
        u.setEnabled(true);
        u.setSuperAccountRelations(new ArrayList<>());
        u.setUserSettings(new UserSettings());
        return u;
    }

    private Asset buildAsset(Long id) {
        Asset a = new Asset();
        a.setId(id);
        a.setName("Asset" + id);
        a.setCompany(company);
        a.setCreatedBy(user.getId());
        a.setTeams(new ArrayList<>());
        a.setAssignedTo(new ArrayList<>());
        a.setStatus(AssetStatus.OPERATIONAL);
        return a;
    }

    private AssetPatchDTO buildPatchDTO() {
        AssetPatchDTO dto = new AssetPatchDTO();
        dto.setStatus(AssetStatus.OPERATIONAL);
        dto.setArchived(false);
        return dto;
    }

    private AssetPostDTO buildPostDTO() {
        AssetPostDTO dto = new AssetPostDTO();
        dto.setName("New Asset");
        dto.setCompany(company);
        dto.setStatus(AssetStatus.OPERATIONAL);
        dto.setTeams(new ArrayList<>());
        dto.setAssignedTo(new ArrayList<>());
        return dto;
    }

    private void stubMessageKeys() {
        lenient().when(messageSource.getMessage(eq("new_assignment"), isNull(), any(Locale.class)))
                .thenReturn("New assignment");
        lenient().when(messageSource.getMessage(eq("notification_asset_assigned"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Asset assigned");
        lenient().when(messageSource.getMessage(eq("notification_asset_operational"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Asset operational");
        lenient().when(messageSource.getMessage(eq("asset_status_change"), isNull(), any(Locale.class)))
                .thenReturn("Status change");
        lenient().when(messageSource.getMessage(eq("notification_asset_down"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Asset down");
    }

    private void stubAssetLimit(boolean hasEntitlement, boolean hasMore) {
        lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ASSETS)).thenReturn(hasEntitlement);
        lenient().when(assetRepository.hasMoreThan(eq(company.getId()), anyLong())).thenReturn(hasMore);
    }

    private void stubCreatePipeline(Long sequence) {
        when(customSequenceService.getNextAssetSequence(company)).thenReturn(sequence);
        when(assetRepository.saveAndFlush(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetMapper.toShowDto(any(Asset.class), any(AssetService.class))).thenReturn(new AssetShowDTO());
    }

    private void stubUpdatePipeline() {
        when(assetRepository.existsById(1L)).thenReturn(true);
        when(assetRepository.saveAndFlush(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubWebhookShowDto() {
        when(assetMapper.toShowDto(any(Asset.class), any(AssetService.class))).thenReturn(new AssetShowDTO());
    }

    @Nested
    class Create {

        @Test
        void assignsCustomIdAndSaves() {
            Asset asset = buildAsset(1L);
            stubAssetLimit(false, false);
            stubCreatePipeline(42L);

            Asset result = assetService.create(asset, user);

            assertEquals("A000042", result.getCustomId());
            verify(assetRepository).saveAndFlush(asset);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.NEW_ASSET), anyMap(),
                    eq("newAsset"), any(), isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        void withParentWithoutHierarchyEntitlement_throwsForbidden() {
            Asset asset = buildAsset(1L);
            asset.setParentAsset(buildAsset(2L));
            stubAssetLimit(false, false);
            when(licenseService.hasEntitlement(LicenseEntitlement.ASSET_HIERARCHY)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class, () -> assetService.create(asset, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(assetRepository, never()).saveAndFlush(any());
        }

        @Test
        void withParentAndHierarchyEntitlement_savesChild() {
            Asset asset = buildAsset(1L);
            asset.setParentAsset(buildAsset(2L));
            stubAssetLimit(false, false);
            when(licenseService.hasEntitlement(LicenseEntitlement.ASSET_HIERARCHY)).thenReturn(true);
            stubCreatePipeline(7L);

            Asset result = assetService.create(asset, user);

            assertEquals("A000007", result.getCustomId());
            verify(assetRepository).saveAndFlush(asset);
        }

        @Test
        void overFreeLimit_throwsForbidden() {
            Asset asset = buildAsset(1L);
            stubAssetLimit(false, true);

            CustomException ex = assertThrows(CustomException.class, () -> assetService.create(asset, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withUnlimitedAssetsEntitlement_bypassesLimit() {
            Asset asset = buildAsset(1L);
            stubAssetLimit(true, false);
            stubCreatePipeline(2L);

            assetService.create(asset, user);

            verify(assetRepository, never()).hasMoreThan(anyLong(), anyLong());
        }

        @Test
        void withAutoGenerateBarcodeEnabled_assignsBarcode() {
            generalPreferences.setAutoGenerateAssetBarcode(true);
            Asset asset = buildAsset(1L);
            stubAssetLimit(false, false);
            stubCreatePipeline(1L);

            Asset result = assetService.create(asset, user);

            assertNotNull(result.getBarCode());
        }

        @Test
        void withPostDto_mapsAndSetsCustomFields() {
            AssetPostDTO dto = buildPostDTO();
            dto.setCustomFields(Collections.singletonList(new com.grash.dto.cutomField.CustomFieldValuePostDTO()));
            Asset mapped = buildAsset(1L);
            when(assetMapper.fromPostDto(any(AssetPostDTO.class))).thenReturn(mapped);
            stubAssetLimit(false, false);
            stubCreatePipeline(10L);

            assetService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.ASSET), any());
        }

        @Test
        void withPostDto_withoutCustomFields_skipsCustomFieldMapping() {
            AssetPostDTO dto = buildPostDTO();
            Asset mapped = buildAsset(1L);
            when(assetMapper.fromPostDto(any(AssetPostDTO.class))).thenReturn(mapped);
            stubAssetLimit(false, false);
            stubCreatePipeline(10L);

            assetService.create(dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }

        @Test
        void withPostDto_customFieldConsumer_setsAssetOnCfValue() {
            AssetPostDTO dto = buildPostDTO();
            dto.setCustomFields(Collections.singletonList(new com.grash.dto.cutomField.CustomFieldValuePostDTO()));
            Asset mapped = buildAsset(1L);
            when(assetMapper.fromPostDto(any(AssetPostDTO.class))).thenReturn(mapped);
            stubAssetLimit(false, false);
            stubCreatePipeline(10L);

            doAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Consumer<CustomFieldValue> consumer = inv.getArgument(5);
                CustomFieldValue cfv = new CustomFieldValue();
                consumer.accept(cfv);
                assertSame(mapped, cfv.getAsset());
                return null;
            }).when(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.ASSET), any());

            assetService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.ASSET), any());
        }
    }

    @Nested
    class CreateByUser {

        @Test
        void createsAssetAndNotifies() {
            AssetPostDTO dto = buildPostDTO();
            Asset created = buildAsset(1L);
            when(assetMapper.fromPostDto(any(AssetPostDTO.class))).thenReturn(created);
            stubAssetLimit(false, false);
            stubCreatePipeline(42L);
            stubMessageKeys();

            Asset result = assetService.createByUser(dto, user);

            assertSame(created, result);
            assertEquals("A000042", created.getCustomId());
            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
        }

        @Test
        void withoutCreatePermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.createByUser(buildPostDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void duplicateBarCode_throwsNotAcceptable() {
            AssetPostDTO dto = buildPostDTO();
            dto.setBarCode("BAR123");
            when(assetRepository.findByBarCodeAndCompany_Id("BAR123", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.createByUser(dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void duplicateNfcId_throwsNotAcceptable() {
            AssetPostDTO dto = buildPostDTO();
            dto.setNfcId("NFC1");
            when(assetRepository.findByNfcIdAndCompany_Id("NFC1", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.createByUser(dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class CheckUsageBasedLimit {

        @Test
        void underLimit_noException() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ASSETS)).thenReturn(false);
            when(assetRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_ASSETS) - 1))))
                    .thenReturn(false);

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(assetService, "checkUsageBasedLimit", company));
        }

        @Test
        void atLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ASSETS)).thenReturn(false);
            when(assetRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_ASSETS) - 1))))
                    .thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> ReflectionTestUtils.invokeMethod(assetService, "checkUsageBasedLimit", company));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withEntitlement_bypassesLimit() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ASSETS)).thenReturn(true);

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(assetService, "checkUsageBasedLimit", company));

            verify(assetRepository, never()).hasMoreThan(anyLong(), anyLong());
        }
    }

    @Nested
    class Update {

        @Test
        void updatesExistingAsset() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            stubUpdatePipeline();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset result = assetService.update(1L, dto, company);

            assertSame(saved, result);
            verify(em).refresh(saved);
            verify(webhookDispatchService, never()).dispatchWebhook(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any());
        }

        @Test
        void assetNotFound_throwsNotFound() {
            when(assetRepository.existsById(1L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.update(1L, buildPatchDTO(), company));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void parentWithoutHierarchyEntitlement_throwsForbidden() {
            AssetPatchDTO dto = buildPatchDTO();
            dto.setParentAsset(buildAsset(2L));
            when(licenseService.hasEntitlement(LicenseEntitlement.ASSET_HIERARCHY)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class, () -> assetService.update(1L, dto, company));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(assetRepository, never()).existsById(anyLong());
        }

        @Test
        void statusChange_dispatchesStatusChangeWebhook() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            Asset patched = buildAsset(1L);
            patched.setStatus(AssetStatus.DOWN);
            stubUpdatePipeline();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenReturn(patched);
            stubWebhookShowDto();

            Asset result = assetService.update(1L, dto, company);

            assertEquals(AssetStatus.DOWN, result.getStatus());
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE), anyMap(),
                    eq("changedAsset"), any(), isNull(), eq(AssetStatus.DOWN), isNull(), isNull(), isNull());
        }

        @Test
        void withCustomFields_appliesCustomFields() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            dto.setCustomFields(Collections.singletonList(new com.grash.dto.cutomField.CustomFieldValuePostDTO()));
            stubUpdatePipeline();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenAnswer(inv -> inv.getArgument(0));

            assetService.update(1L, dto, company);

            verify(customFieldValueService).setCustomFields(eq(saved), eq(saved.getCustomFieldValues()),
                    eq(dto.getCustomFields()), eq(company), eq(CustomFieldEntityType.ASSET), any());
        }
    }

    @Nested
    class Patch {

        @Test
        void patchesExistingAsset() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            stubUpdatePipeline();
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenAnswer(inv -> inv.getArgument(0));
            stubMessageKeys();

            Asset result = assetService.patch(1L, dto, user);

            assertSame(saved, result);
            verify(em).detach(saved);
            verify(assetRepository).saveAndFlush(saved);
        }

        @Test
        void assetNotFound_throwsNotFound() {
            when(assetRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotEdit_throwsForbidden() {
            user.setRole(restrictedRole());
            Asset saved = buildAsset(1L);
            saved.setCreatedBy(999L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void duplicateBarCodeOnAnotherAsset_throwsNotAcceptable() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            dto.setBarCode("BAR");
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetRepository.findByBarCodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.patch(1L, dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void parentAssetSameId_throwsNotAcceptable() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            Asset parent = new Asset();
            parent.setId(1L);
            dto.setParentAsset(parent);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.patch(1L, dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void duplicateNfcIdOnAnotherAsset_throwsNotAcceptable() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            dto.setNfcId("NFC1");
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetRepository.findByNfcIdAndCompany_Id("NFC1", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.patch(1L, dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void transitionToDown_triggersDowntime() {
            Asset saved = buildAsset(1L);
            AssetPatchDTO dto = buildPatchDTO();
            dto.setStatus(AssetStatus.DOWN);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            stubUpdatePipeline();
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenAnswer(inv -> inv.getArgument(0));
            stubWebhookShowDto();
            stubMessageKeys();

            Asset result = assetService.patch(1L, dto, user);

            assertEquals(AssetStatus.DOWN, result.getStatus());
            verify(assetDowntimeService).create(any(AssetDowntime.class), eq(false));
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE), anyMap(),
                    eq("changedAsset"), any(), isNull(), eq(AssetStatus.DOWN), isNull(), isNull(), isNull());
        }

        @Test
        void transitionFromDown_stopsDowntime() {
            Asset saved = buildAsset(1L);
            saved.setStatus(AssetStatus.DOWN);
            AssetPatchDTO dto = buildPatchDTO();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(assetDowntimeService.findByAsset(1L)).thenReturn(Collections.emptyList());
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assetRepository.findByParentAsset_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            stubUpdatePipeline();
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenAnswer(inv -> inv.getArgument(0));
            stubWebhookShowDto();
            stubMessageKeys();

            Asset result = assetService.patch(1L, dto, user);

            assertEquals(AssetStatus.OPERATIONAL, result.getStatus());
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE), anyMap(),
                    eq("changedAsset"), any(), isNull(), eq(AssetStatus.OPERATIONAL), isNull(), isNull(), isNull());
        }
    }

    @Nested
    class Scanning {

        @Test
        void getByNfcIdAndCompany_withoutEntitlement_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.NFC_BARCODE)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.getByNfcIdAndCompany("NFC1", user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getByNfcIdAndCompany_returnsAsset() {
            Asset asset = buildAsset(1L);
            when(licenseService.hasEntitlement(LicenseEntitlement.NFC_BARCODE)).thenReturn(true);
            when(assetRepository.findByNfcIdAndCompany_Id("NFC1", 1L)).thenReturn(Optional.of(asset));

            Asset result = assetService.getByNfcIdAndCompany("NFC1", user);

            assertSame(asset, result);
        }

        @Test
        void getByBarcodeAndCompany_withoutEntitlement_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.NFC_BARCODE)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.getByBarcodeAndCompany("BAR", user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getByBarcodeAndCompany_returnsAsset() {
            Asset asset = buildAsset(1L);
            when(licenseService.hasEntitlement(LicenseEntitlement.NFC_BARCODE)).thenReturn(true);
            when(assetRepository.findByBarCodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(asset));

            Asset result = assetService.getByBarcodeAndCompany("BAR", user);

            assertSame(asset, result);
        }
    }

    @Nested
    class AccessControl {

        @Test
        void checkAccessToAssetId_notFound_throwsNotFound() {
            when(assetRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.checkAccessToAssetId(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void checkAccessToAssetId_forbidden_throwsForbidden() {
            user.setRole(restrictedRole());
            when(assetRepository.findById(1L)).thenReturn(Optional.of(buildAsset(1L)));

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.checkAccessToAssetId(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void checkAccessToAssetId_returnsAsset() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

            Asset result = assetService.checkAccessToAssetId(1L, user);

            assertSame(asset, result);
        }

        @Test
        void findByLocationAndUser_locationNotFound_throwsNotFound() {
            when(locationService.findById(5L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findByLocationAndUser(5L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void findByLocationAndUser_forbidden_throwsForbidden() {
            user.setRole(restrictedRole());
            Location location = new Location();
            location.setId(5L);
            when(locationService.findById(5L)).thenReturn(Optional.of(location));

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findByLocationAndUser(5L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void findByLocationAndUser_returnsAssets() {
            Location location = new Location();
            location.setId(5L);
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(locationService.findById(5L)).thenReturn(Optional.of(location));
            when(assetRepository.findByLocation_Id(5L)).thenReturn(assets);

            Collection<Asset> result = assetService.findByLocationAndUser(5L, user);

            assertSame(assets, result);
        }

        @Test
        void findByPartAndUser_partNotFound_throwsNotFound() {
            when(partService.findById(5L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> assetService.findByPartAndUser(5L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void findByPartAndUser_forbidden_throwsForbidden() {
            user.setRole(restrictedRole());
            Part part = new Part();
            part.setId(5L);
            when(partService.findById(5L)).thenReturn(Optional.of(part));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.findByPartAndUser(5L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void findByPartAndUser_returnsAssets() {
            Part part = new Part();
            part.setId(5L);
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            part.setAssets(assets);
            when(partService.findById(5L)).thenReturn(Optional.of(part));

            Collection<Asset> result = assetService.findByPartAndUser(5L, user);

            assertSame(assets, result);
        }
    }

    @Nested
    class FindChildren {

        @Test
        void withoutViewPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findChildren(0L, user, Pageable.unpaged()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void rootIdForClient_returnsTopLevelAssets() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByCompany_IdAndParentAssetIsNull(1L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));

            List<Asset> result = assetService.findChildren(0L, user, Pageable.unpaged());

            assertEquals(Collections.singletonList(asset), result);
        }

        @Test
        void assetNotFound_throwsNotFound() {
            when(assetRepository.findById(5L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findChildren(5L, user, Pageable.unpaged()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void returnsChildren() {
            Asset asset = buildAsset(5L);
            Asset child = buildAsset(6L);
            when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
            when(assetRepository.findByParentAsset_Id(5L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(child)));

            List<Asset> result = assetService.findChildren(5L, user, Pageable.unpaged());

            assertEquals(Collections.singletonList(child), result);
        }
    }

    @Nested
    class FindMiniPublic {

        private Bucket bucket;

        @BeforeEach
        void initBucket() {
            bucket = mock(Bucket.class);
            when(rateLimiterService.resolvePublicMiniBucket("1.2.3.4")).thenReturn(bucket);
        }

        @Test
        void rateLimitExceeded_throwsTooManyRequests() {
            when(bucket.tryConsume(1)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findMiniPublic("uuid", null, "1.2.3.4"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
        }

        @Test
        void portalWithAssetField_throwsForbidden() {
            when(bucket.tryConsume(1)).thenReturn(true);
            RequestPortal portal = new RequestPortal();
            portal.setCompany(company);
            RequestPortalField field = new RequestPortalField();
            field.setType(PortalFieldType.ASSET);
            field.setAsset(buildAsset(1L));
            portal.setFields(Collections.singletonList(field));
            when(requestPortalService.findByUuidByUser("uuid")).thenReturn(Optional.of(portal));

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findMiniPublic("uuid", null, "1.2.3.4"));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withoutLocation_returnsCompanyAssets() {
            when(bucket.tryConsume(1)).thenReturn(true);
            RequestPortal portal = new RequestPortal();
            portal.setCompany(company);
            when(requestPortalService.findByUuidByUser("uuid")).thenReturn(Optional.of(portal));
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByCompany_Id(1L)).thenReturn(assets);

            List<Asset> result = assetService.findMiniPublic("uuid", null, "1.2.3.4");

            assertSame(assets, result);
        }

        @Test
        void withLocation_returnsLocationAssets() {
            when(bucket.tryConsume(1)).thenReturn(true);
            RequestPortal portal = new RequestPortal();
            portal.setCompany(company);
            when(requestPortalService.findByUuidByUser("uuid")).thenReturn(Optional.of(portal));
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByLocation_IdAndCompany_Id(5L, 1L)).thenReturn(assets);

            List<Asset> result = assetService.findMiniPublic("uuid", 5L, "1.2.3.4");

            assertSame(assets, result);
        }
    }

    @Nested
    class GetSearchCriteria {

        @Test
        void clientWithViewOther_filtersByCompanyOnly() {
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = assetService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(1, result.getFilterFields().size());
            assertEquals("company", result.getFilterFields().get(0).getField());
        }

        @Test
        void clientWithoutViewOther_filtersByCompanyAndCreatedBy() {
            Set<PermissionEntity> assetsOnly = new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS));
            Role roleNoViewOther = buildRole(RoleType.ROLE_CLIENT, assetsOnly, new HashSet<>(), assetsOnly,
                    new HashSet<>(), new HashSet<>());
            user.setRole(roleNoViewOther);
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = assetService.getSearchCriteria(user, criteria);

            assertEquals(2, result.getFilterFields().size());
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("company")));
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("createdBy")));
        }

        @Test
        void clientWithoutAssetPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.getSearchCriteria(user, new SearchCriteria()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void superAdmin_returnsCriteriaUnchanged() {
            user.setRole(buildRole(RoleType.ROLE_SUPER_ADMIN, new HashSet<>(), new HashSet<>(), new HashSet<>(),
                    new HashSet<>(), new HashSet<>()));
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = assetService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(0, result.getFilterFields().size());
        }
    }

    @Nested
    class OrderAssets {

        private AssetImportDTO asset(String name, String parentName) {
            return AssetImportDTO.builder().name(name).parentAssetName(parentName).build();
        }

        @Test
        void emptyList_returnsEmpty() {
            assertTrue(AssetService.orderAssets(new ArrayList<>()).isEmpty());
        }

        @Test
        void singleTopLevelAsset_returnsAsIs() {
            AssetImportDTO a = asset("A", null);

            List<AssetImportDTO> result = AssetService.orderAssets(Collections.singletonList(a));

            assertEquals(Collections.singletonList(a), result);
        }

        @Test
        void parentListedBeforeChild() {
            AssetImportDTO a = asset("A", null);
            AssetImportDTO b = asset("B", "A");
            List<AssetImportDTO> input = new ArrayList<>(Arrays.asList(b, a));

            List<AssetImportDTO> result = AssetService.orderAssets(input);

            assertEquals(Arrays.asList(a, b), result);
        }

        @Test
        void nestedHierarchy_ordersRecursively() {
            AssetImportDTO a = asset("A", null);
            AssetImportDTO b = asset("B", "A");
            AssetImportDTO c = asset("C", "B");
            List<AssetImportDTO> input = new ArrayList<>(Arrays.asList(c, a, b));

            List<AssetImportDTO> result = AssetService.orderAssets(input);

            assertEquals(Arrays.asList(a, b, c), result);
        }

        @Test
        void childWithMissingParent_isTreatedAsTopLevel() {
            AssetImportDTO a = asset("A", "MissingParent");

            List<AssetImportDTO> result = AssetService.orderAssets(Collections.singletonList(a));

            assertEquals(Collections.singletonList(a), result);
        }

        @Test
        void orderAssetsRecursive_nullLevels_isNoop() throws Exception {
            List<AssetImportDTO> ordered = new ArrayList<>();
            Set<AssetImportDTO> visited = new HashSet<>();

            Method method = AssetService.class.getDeclaredMethod("orderAssetsRecursive",
                    Map.class, List.class, List.class, Set.class);
            method.setAccessible(true);
            method.invoke(null, new HashMap<String, List<AssetImportDTO>>(), null, ordered, visited);

            assertTrue(ordered.isEmpty());
        }
    }

    @Nested
    class Metrics {

        private final Date start = new Date(0L);
        private final Date end = new Date(172800000L);

        @Test
        void getMTBF_withLessThanTwoDowntimes_returnsZero() {
            AssetDowntime dt = AssetDowntime.builder().startsOn(new Date(0L)).duration(0).build();
            when(assetDowntimeService.findByAssetAndStartsOnBetween(1L, start, end))
                    .thenReturn(Collections.singletonList(dt));

            assertEquals(0L, assetService.getMTBF(1L, start, end));
        }

        @Test
        void getMTBF_averagesIntervals() {
            AssetDowntime dt1 = AssetDowntime.builder().startsOn(new Date(0L)).duration(0).build();
            AssetDowntime dt2 = AssetDowntime.builder().startsOn(end).duration(0).build();
            when(assetDowntimeService.findByAssetAndStartsOnBetween(1L, start, end))
                    .thenReturn(new ArrayList<>(Arrays.asList(dt1, dt2)));

            assertEquals(2L, assetService.getMTBF(1L, start, end));
        }

        @Test
        void getMTTR_withoutWorkOrders_returnsZero() {
            when(workOrderService.findByAssetAndCreatedAtBetween(1L, start, end)).thenReturn(Collections.emptyList());

            assertEquals(0L, assetService.getMTTR(1L, start, end));
            verify(laborService, never()).findByWorkOrder(anyLong());
        }

        @Test
        void getMTTR_averagesLaborDuration() {
            WorkOrder wo = new WorkOrder();
            wo.setId(10L);
            Labor labor = new Labor();
            labor.setStartedAt(new Date(0L));
            labor.setDuration(3600);
            when(workOrderService.findByAssetAndCreatedAtBetween(1L, start, end))
                    .thenReturn(Collections.singletonList(wo));
            when(laborService.findByWorkOrder(10L)).thenReturn(Collections.singletonList(labor));

            assertEquals(60L, assetService.getMTTR(1L, start, end));
        }

        @Test
        void getDowntime_sumsDurations() {
            AssetDowntime dt1 = AssetDowntime.builder().startsOn(new Date(0L)).duration(60).build();
            AssetDowntime dt2 = AssetDowntime.builder().startsOn(new Date(1000L)).duration(120).build();
            when(assetDowntimeService.findByAssetAndStartsOnBetween(1L, start, end))
                    .thenReturn(new ArrayList<>(Arrays.asList(dt1, dt2)));

            assertEquals(180L, assetService.getDowntime(1L, start, end));
        }

        @Test
        void getUptime_subtractsDowntimeFromEffectiveDuration() {
            Asset asset = buildAsset(1L);
            asset.setCreatedAt(new Date(0L));
            asset.setUpdatedAt(new Date(0L));
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAssetAndStartsOnBetween(any(Long.class), any(Date.class), any(Date.class)))
                    .thenReturn(Collections.emptyList());

            long result = assetService.getUptime(1L, start, end);

            assertEquals(TimeUnit.SECONDS.convert(172800000L, TimeUnit.MILLISECONDS), result);
        }

        @Test
        void getUptime_inServiceDateInFuture_returnsZero() {
            Asset asset = buildAsset(1L);
            asset.setInServiceDate(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));

            assertEquals(0L, assetService.getUptime(1L, start, end));
        }

        @Test
        void getTotalCost_delegatesToWorkOrderService() {
            WorkOrder wo = new WorkOrder();
            wo.setId(10L);
            when(workOrderService.findByAssetAndCreatedAtBetween(1L, start, end))
                    .thenReturn(Collections.singletonList(wo));
            when(workOrderService.getAllCost(anyCollection(), eq(true))).thenReturn(150.5);

            assertEquals(150.5, assetService.getTotalCost(1L, start, end, true));
        }
    }

    @Nested
    class StopDownTime {

        @Test
        void assetNotFound_throwsEntityNotFound() {
            when(assetRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> assetService.stopDownTime(1L, Locale.ENGLISH));
        }

        @Test
        void stopsRunningDowntimeAndSetsOperational() {
            Asset asset = buildAsset(1L);
            asset.setStatus(AssetStatus.DOWN);
            AssetDowntime running = AssetDowntime.builder().startsOn(new Date(0L)).duration(0).build();
            running.setId(1L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAsset(1L)).thenReturn(Collections.singletonList(running));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assetRepository.findByParentAsset_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            stubWebhookShowDto();
            stubMessageKeys();

            assetService.stopDownTime(1L, Locale.ENGLISH);

            assertEquals(AssetStatus.OPERATIONAL, asset.getStatus());
            assertTrue(running.getDuration() > 0);
            verify(assetDowntimeService).save(running);
            verify(assetRepository).save(asset);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE), anyMap(),
                    eq("changedAsset"), any(), isNull(), eq(AssetStatus.OPERATIONAL), isNull(), isNull(), isNull());
        }

        @Test
        void withoutRunningDowntime_onlyChangesStatus() {
            Asset asset = buildAsset(1L);
            asset.setStatus(AssetStatus.DOWN);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAsset(1L)).thenReturn(Collections.emptyList());
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assetRepository.findByParentAsset_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            stubWebhookShowDto();
            stubMessageKeys();

            assetService.stopDownTime(1L, Locale.ENGLISH);

            assertEquals(AssetStatus.OPERATIONAL, asset.getStatus());
            verify(assetDowntimeService, never()).save(any(AssetDowntime.class));
        }

        @Test
        void assetAlreadyOperational_noStatusChangeWebhook() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAsset(1L)).thenReturn(Collections.emptyList());
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assetRepository.findByParentAsset_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            stubMessageKeys();

            assetService.stopDownTime(1L, Locale.ENGLISH);

            assertEquals(AssetStatus.OPERATIONAL, asset.getStatus());
            verify(webhookDispatchService, never()).dispatchWebhook(any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any());
        }

        @Test
        void recursivelyStopsChildrenDowntime() {
            Asset parent = buildAsset(1L);
            Asset child = buildAsset(2L);
            child.setStatus(AssetStatus.DOWN);
            AssetDowntime running = AssetDowntime.builder().startsOn(new Date(0L)).duration(0).build();
            running.setId(1L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(assetDowntimeService.findByAsset(1L)).thenReturn(Collections.emptyList());
            when(assetRepository.findByParentAsset_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(child)));
            when(assetRepository.findByParentAsset_Id(eq(2L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(assetDowntimeService.findByAsset(2L)).thenReturn(Collections.singletonList(running));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            stubWebhookShowDto();
            stubMessageKeys();

            assetService.stopDownTime(1L, Locale.ENGLISH);

            assertEquals(AssetStatus.OPERATIONAL, child.getStatus());
            assertTrue(running.getDuration() > 0);
            verify(assetDowntimeService).save(running);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE),
                    anyMap(), eq("changedAsset"), any(), isNull(), eq(AssetStatus.OPERATIONAL), isNull(), isNull(),
                    isNull());
        }
    }

    @Nested
    class TriggerDownTime {

        @Test
        void triggersDownTimeAndDispatchesWebhook() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            stubWebhookShowDto();
            stubMessageKeys();

            assetService.triggerDownTime(1L, Locale.ENGLISH, AssetStatus.DOWN);

            assertEquals(AssetStatus.DOWN, asset.getStatus());
            verify(assetDowntimeService).create(any(AssetDowntime.class), eq(false));
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE), anyMap(),
                    eq("changedAsset"), any(), isNull(), eq(AssetStatus.DOWN), isNull(), isNull(), isNull());
        }

        @Test
        void propagatesDownTimeToParents() {
            Asset asset = buildAsset(1L);
            Asset parent = buildAsset(2L);
            asset.setParentAsset(parent);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            stubWebhookShowDto();
            stubMessageKeys();

            assetService.triggerDownTime(1L, Locale.ENGLISH, AssetStatus.DOWN);

            assertEquals(AssetStatus.DOWN, parent.getStatus());
            assertEquals(AssetStatus.DOWN, asset.getStatus());
            verify(assetDowntimeService, times(2)).create(any(AssetDowntime.class), eq(false));
            verify(webhookDispatchService, times(2)).dispatchWebhook(eq(company),
                    eq(WebhookEvent.ASSET_STATUS_CHANGE), anyMap(), eq("changedAsset"), any(), isNull(),
                    eq(AssetStatus.DOWN), isNull(), isNull(), isNull());
        }

        @Test
        void parentAlreadyDown_keepsParentDown() {
            Asset asset = buildAsset(1L);
            Asset parent = buildAsset(2L);
            parent.setStatus(AssetStatus.DOWN);
            asset.setParentAsset(parent);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
            stubWebhookShowDto();
            stubMessageKeys();

            assetService.triggerDownTime(1L, Locale.ENGLISH, AssetStatus.DOWN);

            assertEquals(AssetStatus.DOWN, parent.getStatus());
            assertEquals(AssetStatus.DOWN, asset.getStatus());
            verify(assetRepository, never()).save(parent);
            verify(assetDowntimeService, times(2)).create(any(AssetDowntime.class), eq(false));
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.ASSET_STATUS_CHANGE),
                    anyMap(), eq("changedAsset"), any(), isNull(), eq(AssetStatus.DOWN), isNull(), isNull(), isNull());
        }
    }

    @Nested
    class DeleteByIdAndUser {

        @Test
        void assetNotFound_throwsNotFound() {
            when(assetRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> assetService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotDelete_throwsForbidden() {
            user.setRole(restrictedRole());
            Asset saved = buildAsset(1L);
            saved.setCreatedBy(999L);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class, () -> assetService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(assetRepository, never()).deleteById(anyLong());
        }

        @Test
        void deletesAsset() {
            when(assetRepository.findById(1L)).thenReturn(Optional.of(buildAsset(1L)));

            assetService.deleteByIdAndUser(1L, user);

            verify(assetRepository).deleteById(1L);
        }
    }

    @Nested
    class Notify {

        @Test
        void notify_sendsNotificationToAllUsers() {
            Asset asset = buildAsset(1L);
            User u1 = buildUser(1L);
            User u2 = buildUser(2L);
            asset.setAssignedTo(new ArrayList<>(Arrays.asList(u1, u2)));

            assetService.notify(asset, "Title", "Message");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationService).createMultiple(captor.capture(), eq(true), eq("Title"));
            List<Notification> notifications = captor.getValue();
            assertEquals(2, notifications.size());
            assertTrue(notifications.stream().allMatch(n -> n.getNotificationType() == NotificationType.ASSET));
            assertTrue(notifications.stream().anyMatch(n -> n.getUser().getId().equals(1L)));
            assertTrue(notifications.stream().anyMatch(n -> n.getUser().getId().equals(2L)));
        }

        @Test
        void patchNotify_onlyNotifiesNewUsers() {
            Asset oldAsset = buildAsset(1L);
            Asset newAsset = buildAsset(1L);
            User u1 = buildUser(1L);
            User u2 = buildUser(2L);
            oldAsset.setAssignedTo(new ArrayList<>(Collections.singletonList(u1)));
            newAsset.setAssignedTo(new ArrayList<>(Arrays.asList(u1, u2)));
            stubMessageKeys();

            assetService.patchNotify(oldAsset, newAsset, Locale.ENGLISH);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationService).createMultiple(captor.capture(), eq(true), anyString());
            List<Notification> notifications = captor.getValue();
            assertEquals(1, notifications.size());
            assertEquals(2L, notifications.get(0).getUser().getId());
        }
    }

    @Nested
    class Delegation {

        @Test
        void getAll() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findAll()).thenReturn(assets);

            assertSame(assets, assetService.getAll());
        }

        @Test
        void findByCompanyWithSort() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            Sort sort = Sort.by("name");
            when(assetRepository.findByCompany_Id(1L, sort)).thenReturn(assets);

            assertSame(assets, assetService.findByCompany(1L, sort));
        }

        @Test
        void findByCompany() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByCompany_Id(1L)).thenReturn(assets);

            assertSame(assets, assetService.findByCompany(1L));
        }

        @Test
        void findByLocation() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByLocation_Id(5L)).thenReturn(assets);

            assertSame(assets, assetService.findByLocation(5L));
        }

        @Test
        void findByIdAndCompany() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(asset));

            assertSame(asset, assetService.findByIdAndCompany(1L, 1L).orElseThrow());
        }

        @Test
        void findByBarcodeAndCompany() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByBarCodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(asset));

            assertSame(asset, assetService.findByBarcodeAndCompany("BAR", 1L).orElseThrow());
        }

        @Test
        void findByNfcIdAndCompany() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByNfcIdAndCompany_Id("NFC", 1L)).thenReturn(Optional.of(asset));

            assertSame(asset, assetService.findByNfcIdAndCompany("NFC", 1L).orElseThrow());
        }

        @Test
        void findByNameIgnoreCaseAndCompany() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByNameIgnoreCaseAndCompany_Id("Pump", 1L)).thenReturn(assets);

            assertSame(assets, assetService.findByNameIgnoreCaseAndCompany("Pump", 1L));
        }

        @Test
        void findByCompanyAndParentAssetNull() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByCompany_IdAndParentAssetIsNull(1L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));

            List<Asset> result = assetService.findByCompanyAndParentAssetNull(1L, Pageable.unpaged());

            assertEquals(Collections.singletonList(asset), result);
        }

        @Test
        void findByCompanyAndBefore() {
            Date date = new Date(1000L);
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByCompany_IdAndCreatedAtBefore(1L, date)).thenReturn(assets);

            assertSame(assets, assetService.findByCompanyAndBefore(1L, date));
        }

        @Test
        void getTotalAcquisitionCost() {
            Date date = new Date(1000L);
            when(assetRepository.getTotalAcquisitionCost(1L, date)).thenReturn(1234.5);

            assertEquals(1234.5, assetService.getTotalAcquisitionCost(1L, date));
        }

        @Test
        void findAssetChildren() {
            Asset child = buildAsset(2L);
            when(assetRepository.findByParentAsset_Id(1L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(child)));

            Page<Asset> result = assetService.findAssetChildren(1L, Pageable.unpaged());

            assertEquals(Collections.singletonList(child), result.getContent());
        }

        @Test
        void findByIdsAndCompany() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByIdInAndCompany_Id(Collections.singletonList(1L), 1L)).thenReturn(assets);

            assertSame(assets, assetService.findByIdsAndCompany(Collections.singletonList(1L), 1L));
        }

        @Test
        void findByCompanyForExport() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByCompanyForExport(1L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));

            Page<Asset> result = assetService.findByCompanyForExport(1L, Pageable.unpaged());

            assertEquals(Collections.singletonList(asset), result.getContent());
        }

        @Test
        void save() {
            Asset asset = buildAsset(1L);
            when(assetRepository.save(asset)).thenReturn(asset);

            assertSame(asset, assetService.save(asset));
        }

        @Test
        void saveAll() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.saveAll(assets)).thenReturn(assets);

            assertSame(assets, assetService.saveAll(assets));
        }

        @Test
        void findMini_withoutLocation_returnsCompanyAssets() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByCompany_Id(1L)).thenReturn(assets);

            assertSame(assets, assetService.findMini(null, user));
        }

        @Test
        void findMini_withLocation_returnsLocationAssets() {
            List<Asset> assets = Collections.singletonList(buildAsset(1L));
            when(assetRepository.findByLocation_Id(5L)).thenReturn(assets);

            assertSame(assets, assetService.findMini(5L, user));
        }

        @Test
        void delete() {
            assetService.delete(1L);

            verify(assetRepository).deleteById(1L);
        }
    }

    @Nested
    class HasChildren {

        @Test
        void returnsTrueWhenChildrenExist() {
            when(assetRepository.countByParentAsset_Id(1L)).thenReturn(3);

            assertTrue(assetService.hasChildren(1L));
        }

        @Test
        void returnsFalseWhenNoChildren() {
            when(assetRepository.countByParentAsset_Id(1L)).thenReturn(0);

            assertFalse(assetService.hasChildren(1L));
        }

        @Test
        void getParentIdsWithChildren_returnsDistinctParents() {
            when(assetRepository.findParentIdsWithChildren(List.of(1L, 2L))).thenReturn(List.of(1L));

            Set<Long> result = assetService.getParentIdsWithChildren(List.of(1L, 2L));

            assertEquals(Set.of(1L), result);
        }

        @Test
        void getParentIdsWithChildren_emptyInput_returnsEmpty() {
            assertTrue(assetService.getParentIdsWithChildren(Collections.emptyList()).isEmpty());
        }
    }

    @Nested
    class FindBySearchCriteria {

        @Test
        void returnsMappedPage() {
            SearchCriteria criteria = new SearchCriteria();
            Asset asset = buildAsset(1L);
            when(assetRepository.findAll((Specification<Asset>) isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));
            when(assetRepository.findParentIdsWithChildren(any())).thenReturn(Collections.emptyList());
            when(assetMapper.toShowDto(any(Asset.class), anySet())).thenReturn(new AssetShowDTO());

            Page<AssetShowDTO> result = assetService.findBySearchCriteria(criteria);

            assertEquals(1, result.getContent().size());
            verify(assetMapper).toShowDto(eq(asset), anySet());
        }
    }

    @Nested
    class SetDeps {

        @Test
        void injectsLazyDependencies() {
            assetService.setDeps(locationService, laborService, workOrderService, webhookDispatchService);

            assertSame(locationService, ReflectionTestUtils.getField(assetService, "locationService"));
            assertSame(laborService, ReflectionTestUtils.getField(assetService, "laborService"));
            assertSame(workOrderService, ReflectionTestUtils.getField(assetService, "workOrderService"));
            assertSame(webhookDispatchService, ReflectionTestUtils.getField(assetService, "webhookDispatchService"));
        }
    }

    @Nested
    class FindChildrenPaginated {

        @Test
        void withoutViewPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findChildrenPaginated(0L, user, Pageable.unpaged()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void rootIdForClient_returnsTopLevelAssets() {
            Asset asset = buildAsset(1L);
            when(assetRepository.findByCompany_IdAndParentAssetIsNull(1L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(asset)));

            Page<Asset> result = assetService.findChildrenPaginated(0L, user, Pageable.unpaged());

            assertEquals(Collections.singletonList(asset), result.getContent());
        }

        @Test
        void assetNotFound_throwsNotFound() {
            when(assetRepository.findById(5L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.findChildrenPaginated(5L, user, Pageable.unpaged()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void returnsChildren() {
            Asset asset = buildAsset(5L);
            Asset child = buildAsset(6L);
            when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
            when(assetRepository.findByParentAsset_Id(5L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(child)));

            Page<Asset> result = assetService.findChildrenPaginated(5L, user, Pageable.unpaged());

            assertEquals(Collections.singletonList(child), result.getContent());
        }
    }

    @Nested
    class SetAssetFieldsFromImportDto {

        private AssetImportDTO buildImportDTO() {
            return AssetImportDTO.builder()
                    .name("Pump")
                    .description("Main pump")
                    .area("Zone 1")
                    .archived("true")
                    .barCode("BAR-123")
                    .model("M1")
                    .manufacturer("ACME")
                    .power("5kW")
                    .serialNumber("SN-1")
                    .additionalInfos("extra info")
                    .warrantyExpirationDate(45000.0)
                    .acquisitionCost(2500.0)
                    .status("DOWN")
                    .locationName("Building A")
                    .parentAssetName("Parent Asset")
                    .category("Electrical")
                    .primaryUserEmail("tech@test.com")
                    .assignedToEmails(Collections.singletonList("worker@test.com"))
                    .teamsNames(Collections.singletonList("Night"))
                    .customersNames(Collections.singletonList("ACME"))
                    .vendorsNames(Collections.singletonList("VendorCo"))
                    .partsNames(Collections.singletonList("Bearing"))
                    .build();
        }

        @Test
        void overFreeLimit_throwsForbidden() {
            stubAssetLimit(false, true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.setAssetFieldsFromImportDto(buildAsset(1L), new AssetImportDTO(), company,
                            null));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void parentWithoutHierarchyEntitlement_throwsForbidden() {
            stubAssetLimit(false, false);
            AssetImportDTO dto = new AssetImportDTO();
            dto.setParentAssetName("Parent");

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.setAssetFieldsFromImportDto(buildAsset(1L), dto, company, null));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void duplicateBarcodeOnCreation_throwsNotAcceptable() {
            stubAssetLimit(false, false);
            AssetImportDTO dto = new AssetImportDTO();
            dto.setBarCode("BAR");
            when(assetRepository.findByBarCodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.setAssetFieldsFromImportDto(buildAsset(1L), dto, company, null));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void duplicateBarcodeSameAsset_allowed() {
            stubAssetLimit(false, false);
            when(customSequenceService.getNextAssetSequence(company)).thenReturn(5L);
            AssetImportDTO dto = new AssetImportDTO();
            dto.setId(2L);
            dto.setBarCode("BAR");
            when(assetRepository.findByBarCodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            Asset asset = buildAsset(1L);
            assertDoesNotThrow(() -> assetService.setAssetFieldsFromImportDto(asset, dto, company, null));

            assertEquals("BAR", asset.getBarCode());
            assertEquals("A000005", asset.getCustomId());
        }

        @Test
        void duplicateBarcodeOnUpdateAnotherAsset_throwsNotAcceptable() {
            stubAssetLimit(false, false);
            AssetImportDTO dto = new AssetImportDTO();
            dto.setId(1L);
            dto.setBarCode("BAR");
            when(assetRepository.findByBarCodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildAsset(2L)));

            CustomException ex = assertThrows(CustomException.class,
                    () -> assetService.setAssetFieldsFromImportDto(buildAsset(1L), dto, company, null));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void parentAssetFallsBackToDatabaseWhenNotInBatch() {
            stubAssetLimit(false, false);
            when(licenseService.hasEntitlement(LicenseEntitlement.ASSET_HIERARCHY)).thenReturn(true);
            AssetImportDTO dto = new AssetImportDTO();
            dto.setName("Pump");
            dto.setParentAssetName("Parent Asset");
            Asset parent = buildAsset(2L);
            when(assetRepository.findByNameIgnoreCaseAndCompany_Id("Parent Asset", 1L))
                    .thenReturn(Collections.singletonList(parent));

            Asset asset = buildAsset(1L);
            assetService.setAssetFieldsFromImportDto(asset, dto, company, new HashMap<>());

            assertSame(parent, asset.getParentAsset());
        }

        @Test
        void mapsAllImportFields() {
            stubAssetLimit(false, false);
            when(licenseService.hasEntitlement(LicenseEntitlement.ASSET_HIERARCHY)).thenReturn(true);
            when(customSequenceService.getNextAssetSequence(company)).thenReturn(10L);
            lenient().when(messageSource.getMessage(anyString(), isNull(), any(Locale.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Location location = new Location();
            location.setId(5L);
            when(locationService.findByNameIgnoreCaseAndCompany("Building A", 1L))
                    .thenReturn(Collections.singletonList(location));

            AssetCategory category = new AssetCategory();
            category.setId(7L);
            when(assetCategoryService.getOrCreate("Electrical", company.getCompanySettings())).thenReturn(category);

            User primary = buildUser(2L);
            when(userService.findByEmailAndCompany("tech@test.com", 1L)).thenReturn(Optional.of(primary));

            User assigned = buildUser(3L);
            when(userService.findByEmailAndCompany("worker@test.com", 1L)).thenReturn(Optional.of(assigned));

            Team team = new Team();
            team.setId(4L);
            when(teamService.findByNameIgnoreCaseAndCompany("Night", 1L)).thenReturn(Optional.of(team));

            Customer customer = new Customer();
            customer.setId(8L);
            when(customerService.findByNameIgnoreCaseAndCompany("ACME", 1L)).thenReturn(Optional.of(customer));

            Vendor vendor = new Vendor();
            vendor.setId(9L);
            when(vendorService.findByNameIgnoreCaseAndCompany("VendorCo", 1L)).thenReturn(Optional.of(vendor));

            Part part = new Part();
            part.setId(10L);
            when(partService.findByNameIgnoreCaseAndCompany("Bearing", 1L)).thenReturn(Optional.of(part));

            Asset parent = buildAsset(2L);
            Map<String, Asset> assetsByName = new HashMap<>();
            assetsByName.put("Parent Asset", parent);

            AssetImportDTO dto = buildImportDTO();
            Asset asset = buildAsset(1L);

            assetService.setAssetFieldsFromImportDto(asset, dto, company, assetsByName);

            assertEquals("Pump", asset.getName());
            assertEquals("Main pump", asset.getDescription());
            assertEquals("Zone 1", asset.getArea());
            assertEquals("BAR-123", asset.getBarCode());
            assertEquals("M1", asset.getModel());
            assertEquals("ACME", asset.getManufacturer());
            assertEquals("5kW", asset.getPower());
            assertEquals("SN-1", asset.getSerialNumber());
            assertEquals("extra info", asset.getAdditionalInfos());
            assertEquals(2500.0, asset.getAcquisitionCost());
            assertTrue(asset.isArchived());
            assertEquals("A000010", asset.getCustomId());
            assertSame(location, asset.getLocation());
            assertSame(parent, asset.getParentAsset());
            assertSame(category, asset.getCategory());
            assertSame(primary, asset.getPrimaryUser());
            assertEquals(Collections.singletonList(assigned), asset.getAssignedTo());
            assertEquals(Collections.singletonList(team), asset.getTeams());
            assertEquals(Collections.singletonList(customer), asset.getCustomers());
            assertEquals(Collections.singletonList(vendor), asset.getVendors());
            assertEquals(Collections.singletonList(part), asset.getParts());
            assertEquals(AssetStatus.DOWN, asset.getStatus());
            assertNotNull(asset.getWarrantyExpirationDate());
        }
    }
}
