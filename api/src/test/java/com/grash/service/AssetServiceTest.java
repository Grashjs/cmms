package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.imports.AssetImportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetMapper;
import com.grash.model.*;
import com.grash.model.enums.AssetStatus;
import com.grash.repository.AssetRepository;
import com.grash.utils.Helper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private LocationService locationService;
    @Mock
    private FileService fileService;
    @Mock
    private AssetCategoryService assetCategoryService;
    @Mock
    private DeprecationService deprecationService;
    @Mock
    private UserService userService;
    @Mock
    private CustomerService customerService;
    @Mock
    private VendorService vendorService;
    @Mock
    private LaborService laborService;
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
    private WorkOrderService workOrderService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private CustomSequenceService customSequenceService;

    private AssetService assetService;

    private Asset asset;
    private Company company;
    private OwnUser user;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        assetRepository = mock(AssetRepository.class);
        locationService = mock(LocationService.class);
        fileService = mock(FileService.class);
        assetCategoryService = mock(AssetCategoryService.class);
        deprecationService = mock(DeprecationService.class);
        userService = mock(UserService.class);
        customerService = mock(CustomerService.class);
        vendorService = mock(VendorService.class);
        laborService = mock(LaborService.class);
        notificationService = mock(NotificationService.class);
        teamService = mock(TeamService.class);
        partService = mock(PartService.class);
        assetMapper = mock(AssetMapper.class);
        em = mock(EntityManager.class);
        assetDowntimeService = mock(AssetDowntimeService.class);
        workOrderService = mock(WorkOrderService.class);
        messageSource = mock(MessageSource.class);
        customSequenceService = mock(CustomSequenceService.class);

        // Manually instantiate AssetService with mocked dependencies
        assetService = new AssetService(
                assetRepository,
                fileService,
                assetCategoryService,
                deprecationService,
                userService,
                customerService,
                vendorService,
                notificationService,
                teamService,
                partService,
                assetMapper,
                em,
                assetDowntimeService,
                messageSource,
                customSequenceService
        );

        // Manually set dependencies that are not injected by constructor
        assetService.setDeps(locationService, laborService, workOrderService);

        company = new Company();
        company.setId(1L);

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);

        asset = new Asset();
        asset.setId(1L);
        asset.setName("Test Asset");
        asset.setCompany(company);
        asset.setParentAsset(null); // Ensure no parent asset by default

        companySettings = new CompanySettings();
        companySettings.setId(1L);
        company.setCompanySettings(companySettings);

        // Assertions to ensure mocks are not null
        assertNotNull(assetService);
        assertNotNull(assetRepository);
        assertNotNull(locationService);
        assertNotNull(fileService);
        assertNotNull(assetCategoryService);
        assertNotNull(deprecationService);
        assertNotNull(userService);
        assertNotNull(customerService);
        assertNotNull(vendorService);
        assertNotNull(laborService);
        assertNotNull(notificationService);
        assertNotNull(teamService);
        assertNotNull(partService);
        assertNotNull(assetMapper);
        assertNotNull(em);
        assertNotNull(assetDowntimeService);
        assertNotNull(workOrderService);
        assertNotNull(messageSource);
        assertNotNull(customSequenceService);
    }

    @Nested
    @DisplayName("Create")
    class Create {
        @Test
        void testCreateAsset() {
            when(customSequenceService.getNextAssetSequence(any(Company.class))).thenReturn(1L);
            when(assetRepository.saveAndFlush(any(Asset.class))).thenReturn(asset);
            Asset createdAsset = assetService.create(asset, user);
            assertNotNull(createdAsset);
            assertEquals("Test Asset", createdAsset.getName());
            verify(assetRepository, times(1)).saveAndFlush(asset);
            verify(em, times(1)).refresh(any(Asset.class));
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {
        @Test
        void testUpdateAsset() {
            AssetPatchDTO patchDTO = new AssetPatchDTO();
            patchDTO.setName("Updated Asset");

            when(assetRepository.existsById(1L)).thenReturn(true);
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(assetMapper.updateAsset(any(Asset.class), any(AssetPatchDTO.class))).thenReturn(asset);
            when(assetRepository.saveAndFlush(any(Asset.class))).thenReturn(asset);

            Asset updatedAsset = assetService.update(1L, patchDTO);

            assertNotNull(updatedAsset);
            verify(em, times(1)).refresh(any(Asset.class));
        }

        @Test
        void testUpdateAssetNotFound() {
            AssetPatchDTO patchDTO = new AssetPatchDTO();
            patchDTO.setName("Updated Asset");

            when(assetRepository.existsById(1L)).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> assetService.update(1L, patchDTO));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {
        @Test
        void testDeleteAsset() {
            assetService.delete(1L);
            verify(assetRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Find")
    class Find {
        @Test
        void testGetAllAssets() {
            assetService.getAll();
            verify(assetRepository, times(1)).findAll();
        }

        @Test
        void testFindAssetById() {
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            Optional<Asset> foundAsset = assetService.findById(1L);
            assertTrue(foundAsset.isPresent());
            assertEquals("Test Asset", foundAsset.get().getName());
            verify(assetRepository, times(1)).findById(1L);
        }

        @Test
        void testFindAssetByIdNotFound() {
            when(assetRepository.findById(1L)).thenReturn(Optional.empty());
            Optional<Asset> foundAsset = assetService.findById(1L);
            assertFalse(foundAsset.isPresent());
            verify(assetRepository, times(1)).findById(1L);
        }

        @Test
        void testFindByCompany() {
            assetService.findByCompany(1L);
            verify(assetRepository, times(1)).findByCompany_Id(1L);
        }

        @Test
        void testFindAssetChildren() {
            assetService.findAssetChildren(1L, null);
            verify(assetRepository, times(1)).findByParentAsset_Id(1L, null);
        }

        @Test
        void testFindByLocation() {
            assetService.findByLocation(1L);
            verify(assetRepository, times(1)).findByLocation_Id(1L);
        }
    }

    @Nested
    @DisplayName("IsAssetInCompany")
    class IsAssetInCompany {
        @Test
        void testIsAssetInCompany() {
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            assertTrue(assetService.isAssetInCompany(asset, 1L, false));
        }

        @Test
        void testIsAssetInCompanyOptional() {
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            assertTrue(assetService.isAssetInCompany(asset, 1L, true));
        }

        @Test
        void testIsAssetInCompanyNotInCompany() {
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            assertFalse(assetService.isAssetInCompany(asset, 2L, false));
        }

        @Test
        void testIsAssetInCompanyOptionalAndNotInCompany() {
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            assertFalse(assetService.isAssetInCompany(asset, 2L, true));
        }

        @Test
        void testIsAssetInCompanyOptionalAndNull() {
            assertTrue(assetService.isAssetInCompany(null, 1L, true));
        }
    }

    @Nested
    @DisplayName("Import")
    class Import {
        @Test
        void testImportAsset() {
            final AssetImportDTO assetImportDTO = new AssetImportDTO();
            assetImportDTO.setParentAssetName("parent");
            assetImportDTO.setCategory("category");
            assetImportDTO.setPrimaryUserEmail("user@email.com");
            assetImportDTO.setAssignedToEmails(Collections.singletonList("user@email.com"));
            assetImportDTO.setTeamsNames(Collections.singletonList("team"));
            assetImportDTO.setCustomersNames(Collections.singletonList("customer"));
            assetImportDTO.setVendorsNames(Collections.singletonList("vendor"));
            assetImportDTO.setPartsNames(Collections.singletonList("part"));
            assetImportDTO.setArchived("true");
            assetImportDTO.setWarrantyExpirationDate(45000.0); // Corrected to Double
            assetImportDTO.setAcquisitionCost(100.0); 

            lenient().when(customSequenceService.getNextAssetSequence(any(Company.class))).thenReturn(1L);
            lenient().when(assetRepository.findByNameIgnoreCaseAndCompany_Id(any(), any())).thenReturn(Collections.emptyList());
            lenient().when(assetRepository.findByBarCodeAndCompany_Id(any(), any())).thenReturn(Optional.empty());
            lenient().when(locationService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Collections.emptyList());
            lenient().when(assetCategoryService.findByNameIgnoreCaseAndCompanySettings(any(), any())).thenReturn(Optional.empty());
            lenient().when(userService.findByEmailAndCompany(any(), any())).thenReturn(Optional.empty());
            lenient().when(teamService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Optional.empty());
            lenient().when(customerService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Optional.empty());
            lenient().when(vendorService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Optional.empty());
            lenient().when(partService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Optional.empty());

            try (MockedStatic<Helper> mockedHelper = Mockito.mockStatic(Helper.class)) {
                mockedHelper.when(() -> Helper.getDateFromExcelDate(anyDouble())).thenReturn(new Date());
                mockedHelper.when(() -> Helper.getBooleanFromString(anyString())).thenReturn(true);
                mockedHelper.when(() -> Helper.getLocale(any(Company.class))).thenReturn(Locale.ENGLISH);
                            assetService.importAsset(asset, assetImportDTO, company);
                        }
                        verify(assetRepository, times(1)).save(any(Asset.class));        }

        @Test
        void testImportAssetLocationNotFound() {
            final AssetImportDTO assetImportDTO = new AssetImportDTO();
            assetImportDTO.setLocationName("NonExistentLocation");

            when(locationService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Collections.emptyList());

            assetService.importAsset(asset, assetImportDTO, company);

            assertNull(asset.getLocation());
            verify(assetRepository, times(1)).save(any(Asset.class));
        }
    }

    @Nested
    @DisplayName("Save")
    class Save {
        @Test
        void testSaveAsset() {
            when(assetRepository.save(any(Asset.class))).thenReturn(asset);
            Asset savedAsset = assetService.save(asset);
            assertNotNull(savedAsset);
            assertEquals("Test Asset", savedAsset.getName());
            verify(assetRepository, times(1)).save(asset);
        }
    }

    @Nested
    @DisplayName("FindByNfcIdAndCompany")
    class FindByNfcIdAndCompany {
        @Test
        void testFindByNfcIdAndCompanyFound() {
            when(assetRepository.findByNfcIdAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.of(asset));
            Optional<Asset> foundAsset = assetService.findByNfcIdAndCompany("nfc123", 1L);
            assertTrue(foundAsset.isPresent());
            assertEquals("Test Asset", foundAsset.get().getName());
            verify(assetRepository, times(1)).findByNfcIdAndCompany_Id("nfc123", 1L);
        }

        @Test
        void testFindByNfcIdAndCompanyNotFound() {
            when(assetRepository.findByNfcIdAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.empty());
            Optional<Asset> foundAsset = assetService.findByNfcIdAndCompany("nfc123", 1L);
            assertFalse(foundAsset.isPresent());
            verify(assetRepository, times(1)).findByNfcIdAndCompany_Id("nfc123", 1L);
        }
    }

    @Nested
    @DisplayName("FindByCompanyWithSort")
    class FindByCompanyWithSort {
        @Test
        void testFindByCompanyWithSort() {
            Sort sort = Sort.by("name");
            when(assetRepository.findByCompany_Id(anyLong(), any(Sort.class))).thenReturn(Collections.singletonList(asset));
            Collection<Asset> assets = assetService.findByCompany(1L, sort);
            assertFalse(assets.isEmpty());
            assertEquals(1, assets.size());
            verify(assetRepository, times(1)).findByCompany_Id(1L, sort);
        }
    }

    @Nested
    @DisplayName("FindByCompanyAndBefore")
    class FindByCompanyAndBefore {
        @Test
        void testFindByCompanyAndBefore() {
            Date date = new Date();
            when(assetRepository.findByCompany_IdAndCreatedAtBefore(anyLong(), any(Date.class))).thenReturn(Collections.singletonList(asset));
            Collection<Asset> assets = assetService.findByCompanyAndBefore(1L, date);
            assertFalse(assets.isEmpty());
            assertEquals(1, assets.size());
            verify(assetRepository, times(1)).findByCompany_IdAndCreatedAtBefore(1L, date);
        }
    }

    @Nested
    @DisplayName("Notify")
    class Notify {
        @Test
        void testNotify() {
            asset.setAssignedTo(Collections.singletonList(user)); // Corrected
            assetService.notify(asset, "Test Title", "Test Message");
            verify(notificationService, times(1)).createMultiple(anyList(), eq(true), eq("Test Title"));
        }
    }

    @Nested
    @DisplayName("PatchNotify")
    class PatchNotify {
        @Test
        void testPatchNotify() {
            Asset oldAsset = mock(Asset.class); // Mock oldAsset
            Asset newAsset = mock(Asset.class); // Mock newAsset
            doReturn(Collections.singletonList(user)).when(newAsset).getUsers();
            when(oldAsset.getNewUsersToNotify(anyList())).thenReturn(Collections.singletonList(user));

            lenient().when(messageSource.getMessage(eq("new_assignment"), eq(null), any(Locale.class))).thenReturn("New Assignment");
            lenient().when(messageSource.getMessage(eq("notification_asset_assigned"), any(Object[].class), any(Locale.class))).thenReturn("Asset Assigned");
            assetService.patchNotify(oldAsset, newAsset, Locale.ENGLISH);
            verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
            // verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
        }
    }

    @Nested
    @DisplayName("StopDownTime")
    class StopDownTime {
        @Test
        void testStopDownTime() {
            when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAsset(anyLong())).thenReturn(Collections.emptyList());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("message");

            assetService.stopDownTime(1L, Locale.ENGLISH);

            verify(assetRepository, times(1)).findById(1L);
            verify(assetDowntimeService, times(1)).findByAsset(1L);
            verify(assetRepository, times(1)).save(asset);
            verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
        }

        @Test
        void testStopDownTimeNotFound() {
            when(assetRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> assetService.stopDownTime(1L, Locale.ENGLISH));

            verify(assetRepository, times(1)).findById(1L);
            verify(assetDowntimeService, never()).findByAsset(anyLong());
        }
    }

    @Nested
    @DisplayName("TriggerDownTime")
    class TriggerDownTime {
        @Test
        void testTriggerDownTime() {
            Asset localAsset = new Asset();
            localAsset.setId(1L);
            localAsset.setName("Local Asset");
            localAsset.setCompany(company);

            Asset parentAsset = new Asset();
            parentAsset.setId(2L);
            parentAsset.setStatus(AssetStatus.OPERATIONAL);
            localAsset.setParentAsset(parentAsset);

            when(assetRepository.findById(localAsset.getId())).thenReturn(Optional.of(localAsset));
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("message");

            assetService.triggerDownTime(localAsset.getId(), Locale.ENGLISH, AssetStatus.DOWN); // Corrected BROKEN to DOWN

            verify(assetRepository, times(1)).findById(anyLong()); // Called for asset and parentAsset
            verify(assetDowntimeService, times(2)).create(any(AssetDowntime.class)); // Called for asset and parentAsset
            verify(assetRepository, times(2)).save(any(Asset.class)); // Called for asset and parentAsset
            verify(notificationService, times(1)).createMultiple(anyList(), anyBoolean(), anyString());
        }
    }

    @Nested
    @DisplayName("FindBySearchCriteria")
    class FindBySearchCriteria {
        @Test
        void testFindBySearchCriteria() {
            SearchCriteria searchCriteria = mock(SearchCriteria.class);
            when(searchCriteria.getPageNum()).thenReturn(0);
            when(searchCriteria.getPageSize()).thenReturn(10);
            when(searchCriteria.getSortField()).thenReturn("name");
            when(searchCriteria.getDirection()).thenReturn(Sort.Direction.ASC);
            when(searchCriteria.getFilterFields()).thenReturn(Collections.singletonList(FilterField.builder().field("name").operation("eq").value("Test").build()));

            Page<Asset> assetPage = new PageImpl<>(Collections.singletonList(asset));
            lenient().when(assetRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(assetPage);
            when(assetMapper.toShowDto(any(Asset.class), eq(assetService))).thenReturn(new AssetShowDTO());

            Page<AssetShowDTO> result = assetService.findBySearchCriteria(searchCriteria);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.getTotalElements());
            verify(assetRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(assetMapper, times(1)).toShowDto(any(Asset.class), any(AssetService.class));
        }
    }

    @Nested
    @DisplayName("FindByNameIgnoreCaseAndCompany")
    class FindByNameIgnoreCaseAndCompany {
        @Test
        void testFindByNameIgnoreCaseAndCompany() {
            when(assetRepository.findByNameIgnoreCaseAndCompany_Id(anyString(), anyLong())).thenReturn(Collections.singletonList(asset));
            List<Asset> assets = assetService.findByNameIgnoreCaseAndCompany("Test Asset", 1L);
            assertFalse(assets.isEmpty());
            assertEquals(1, assets.size());
            verify(assetRepository, times(1)).findByNameIgnoreCaseAndCompany_Id("Test Asset", 1L);
        }
    }

    @Nested
    @DisplayName("FindByIdAndCompany")
    class FindByIdAndCompany {
        @Test
        void testFindByIdAndCompanyFound() {
            when(assetRepository.findByIdAndCompany_Id(anyLong(), anyLong())).thenReturn(Optional.of(asset));
            Optional<Asset> foundAsset = assetService.findByIdAndCompany(1L, 1L);
            assertTrue(foundAsset.isPresent());
            assertEquals("Test Asset", foundAsset.get().getName());
            verify(assetRepository, times(1)).findByIdAndCompany_Id(1L, 1L);
        }

        @Test
        void testFindByIdAndCompanyNotFound() {
            when(assetRepository.findByIdAndCompany_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
            Optional<Asset> foundAsset = assetService.findByIdAndCompany(1L, 1L);
            assertFalse(foundAsset.isPresent());
            verify(assetRepository, times(1)).findByIdAndCompany_Id(1L, 1L);
        }
    }

    @Nested
    @DisplayName("FindByBarcodeAndCompany")
    class FindByBarcodeAndCompany {
        @Test
        void testFindByBarcodeAndCompanyFound() {
            when(assetRepository.findByBarCodeAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.of(asset));
            Optional<Asset> foundAsset = assetService.findByBarcodeAndCompany("barcode123", 1L);
            assertTrue(foundAsset.isPresent());
            assertEquals("Test Asset", foundAsset.get().getName());
            verify(assetRepository, times(1)).findByBarCodeAndCompany_Id("barcode123", 1L);
        }

        @Test
        void testFindByBarcodeAndCompanyNotFound() {
            when(assetRepository.findByBarCodeAndCompany_Id(anyString(), anyLong())).thenReturn(Optional.empty());
            Optional<Asset> foundAsset = assetService.findByBarcodeAndCompany("barcode123", 1L);
            assertFalse(foundAsset.isPresent());
            verify(assetRepository, times(1)).findByBarCodeAndCompany_Id("barcode123", 1L);
        }
    }

    @Nested
    @DisplayName("OrderAssets")
    class OrderAssets {
        @Test
        void testOrderAssetsWithNoParents() {
            AssetImportDTO asset1 = new AssetImportDTO();
            asset1.setName("Asset1");
            AssetImportDTO asset2 = new AssetImportDTO();
            asset2.setName("Asset2");
            List<AssetImportDTO> assets = Arrays.asList(asset1, asset2);

            List<AssetImportDTO> orderedAssets = AssetService.orderAssets(assets);

            assertEquals(2, orderedAssets.size());
            assertTrue(orderedAssets.contains(asset1));
            assertTrue(orderedAssets.contains(asset2));
        }

        @Test
        void testOrderAssetsWithParents() {
            AssetImportDTO parentAsset = new AssetImportDTO();
            parentAsset.setName("ParentAsset");
            AssetImportDTO childAsset = new AssetImportDTO();
            childAsset.setName("ChildAsset");
            childAsset.setParentAssetName("ParentAsset");
            List<AssetImportDTO> assets = Arrays.asList(childAsset, parentAsset);

            List<AssetImportDTO> orderedAssets = AssetService.orderAssets(assets);

            assertEquals(2, orderedAssets.size());
            assertEquals(parentAsset, orderedAssets.get(0));
            assertEquals(childAsset, orderedAssets.get(1));
        }

        @Test
        void testOrderAssetsWithMultipleLevels() {
            AssetImportDTO grandParent = new AssetImportDTO();
            grandParent.setName("GrandParent");
            AssetImportDTO parent = new AssetImportDTO();
            parent.setName("Parent");
            parent.setParentAssetName("GrandParent");
            AssetImportDTO child = new AssetImportDTO();
            child.setName("Child");
            child.setParentAssetName("Parent");
            List<AssetImportDTO> assets = Arrays.asList(child, grandParent, parent);

            List<AssetImportDTO> orderedAssets = AssetService.orderAssets(assets);

            assertEquals(3, orderedAssets.size());
            assertEquals(grandParent, orderedAssets.get(0));
            assertEquals(parent, orderedAssets.get(1));
            assertEquals(child, orderedAssets.get(2));
        }

        @Test
        void testOrderAssetsWithMissingParent() {
            AssetImportDTO childAsset = new AssetImportDTO();
            childAsset.setName("ChildAsset");
            childAsset.setParentAssetName("NonExistentParent");
            List<AssetImportDTO> assets = Collections.singletonList(childAsset);

            List<AssetImportDTO> orderedAssets = AssetService.orderAssets(assets);

            assertEquals(1, orderedAssets.size());
            assertEquals(childAsset, orderedAssets.get(0));
        }
    }

    @Nested
    @DisplayName("HasChildren")
    class HasChildren {
        @Test
        void testHasChildrenTrue() {
            when(assetRepository.countByParentAsset_Id(anyLong())).thenReturn(1); // Corrected
            assertTrue(assetService.hasChildren(1L));
            verify(assetRepository, times(1)).countByParentAsset_Id(1L);
        }

        @Test
        void testHasChildrenFalse() {
            when(assetRepository.countByParentAsset_Id(anyLong())).thenReturn(0); // Corrected
            assertFalse(assetService.hasChildren(1L));
            verify(assetRepository, times(1)).countByParentAsset_Id(1L);
        }
    }

    @Nested
    @DisplayName("GetMTBFLF")
    class GetMTBFLF {
        @Test
        void testGetMTBFLF() {
            Date start = new Date();
            Date end = new Date();
            asset.setCreatedAt(new Date(System.currentTimeMillis() - 3600 * 1000 * 24 * 30L)); // 30 days ago

            AssetDowntime downtime = new AssetDowntime();
            downtime.setDuration(3600L); // 1 hour
            List<AssetDowntime> downtimes = Collections.singletonList(downtime); // Corrected to List

            when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(downtimes);

            long mtbflf = assetService.getMTBFLF(1L, start, end);

            assertTrue(mtbflf > 0);
            verify(assetRepository, times(1)).findById(1L);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }

        @Test
        void testGetMTBFLFNoDowntimes() {
            Date start = new Date();
            Date end = new Date();
            asset.setCreatedAt(new Date(System.currentTimeMillis() - 3600 * 1000 * 24 * 30L)); // 30 days ago

            when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

            long mtbflf = assetService.getMTBFLF(1L, start, end);

            assertEquals(0, mtbflf);
            verify(assetRepository, times(1)).findById(1L);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }
    }

    @Nested
    @DisplayName("GetMTBF")
    class GetMTBF {
        @Test
        void testGetMTBF() {
            Date start = new Date();
            Date end = new Date();

            AssetDowntime downtime1 = new AssetDowntime();
            downtime1.setStartsOn(new Date(System.currentTimeMillis() - 100000));
            downtime1.setDuration(10000L); // Set duration instead of endsOn
            AssetDowntime downtime2 = new AssetDowntime();
            downtime2.setStartsOn(new Date(System.currentTimeMillis() - 50000));
            downtime2.setDuration(10000L); // Set duration instead of endsOn
            List<AssetDowntime> downtimes = Arrays.asList(downtime1, downtime2);

            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(downtimes);

            long mtbf = assetService.getMTBF(1L, start, end);

            assertTrue(mtbf >= 0);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }

        @Test
        void testGetMTBFNotEnoughDowntimes() {
            Date start = new Date();
            Date end = new Date();

            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(Collections.singletonList(new AssetDowntime()));

            long mtbf = assetService.getMTBF(1L, start, end);

            assertEquals(0, mtbf);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }
    }

    @Nested
    @DisplayName("GetMTTR")
    class GetMTTR {
        @Test
        void testGetMTTR() {
            Date start = new Date();
            Date end = new Date();

            WorkOrder workOrder = new WorkOrder();
            workOrder.setId(1L);
            List<WorkOrder> workOrders = Collections.singletonList(workOrder); // Corrected to List

            Labor labor = new Labor();
            labor.setId(1L);
            labor.setDuration(60L); // 1 minute
            List<Labor> labors = Collections.singletonList(labor);

            when(workOrderService.findByAssetAndCreatedAtBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(workOrders);
            when(laborService.findByWorkOrder(anyLong())).thenReturn(labors);

            try (MockedStatic<Labor> mockedLabor = Mockito.mockStatic(Labor.class)) {
                mockedLabor.when(() -> Labor.getTotalWorkDuration(anyList())).thenReturn(60L);
                long mttr = assetService.getMTTR(1L, start, end);
                assertTrue(mttr > 0);
            }

            verify(workOrderService, times(1)).findByAssetAndCreatedAtBetween(1L, start, end);
            verify(laborService, times(1)).findByWorkOrder(1L);
        }

        @Test
        void testGetMTTRNoWorkOrders() {
            Date start = new Date();
            Date end = new Date();

            when(workOrderService.findByAssetAndCreatedAtBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

            long mttr = assetService.getMTTR(1L, start, end);

            assertEquals(0, mttr);
            verify(workOrderService, times(1)).findByAssetAndCreatedAtBetween(1L, start, end);
            verify(laborService, never()).findByWorkOrder(anyLong());
        }
    }

    @Nested
    @DisplayName("GetDowntime")
    class GetDowntime {
        @Test
        void testGetDowntime() {
            Date start = new Date();
            Date end = new Date();

            AssetDowntime downtime = new AssetDowntime();
            downtime.setDuration(3600L); // 1 hour
            List<AssetDowntime> downtimes = Collections.singletonList(downtime); // Corrected to List

            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(downtimes);

            long totalDowntime = assetService.getDowntime(1L, start, end);

            assertEquals(3600L, totalDowntime);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }

        @Test
        void testGetDowntimeNoDowntimes() {
            Date start = new Date();
            Date end = new Date();

            // Removed unnecessary stubbing for assetRepository.findById
            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

            long totalDowntime = assetService.getDowntime(1L, start, end);

            assertEquals(0L, totalDowntime);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }
    }

    @Nested
    @DisplayName("GetUptime")
    class GetUptime {
        @Test
        void testGetUptime() {
            Date start = new Date();
            Date end = new Date();
            asset.setCreatedAt(new Date(System.currentTimeMillis() - 3600 * 1000 * 24 * 30L)); // 30 days ago

            when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());

            long uptime = assetService.getUptime(1L, start, end);

            assertTrue(uptime > 0);
            verify(assetRepository, times(1)).findById(1L);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }

        @Test
        void testGetUptimeWithDowntime() {
            Date start = new Date();
            Date end = new Date();
            asset.setCreatedAt(new Date(System.currentTimeMillis() - 3600 * 1000 * 24 * 30L)); // 30 days ago

            AssetDowntime downtime = new AssetDowntime();
            downtime.setDuration(3600L); // 1 hour
            List<AssetDowntime> downtimes = Collections.singletonList(downtime); // Corrected to List

            when(assetRepository.findById(anyLong())).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAssetAndStartsOnBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(downtimes);

            long uptime = assetService.getUptime(1L, start, end);

            assertTrue(uptime > 0);
            verify(assetRepository, times(1)).findById(1L);
            verify(assetDowntimeService, times(1)).findByAssetAndStartsOnBetween(1L, start, end);
        }
    }

    @Nested
    @DisplayName("GetTotalCost")
    class GetTotalCost {
        @Test
        void testGetTotalCost() {
            Date start = new Date();
            Date end = new Date();
            WorkOrder workOrder = new WorkOrder();
            workOrder.setId(1L);
            List<WorkOrder> workOrders = Collections.singletonList(workOrder); // Corrected to List

            when(workOrderService.findByAssetAndCreatedAtBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(workOrders);
            when(workOrderService.getAllCost(anyCollection(), anyBoolean())).thenReturn(100.0);

            double totalCost = assetService.getTotalCost(1L, start, end, true);

            assertEquals(100.0, totalCost);
            verify(workOrderService, times(1)).findByAssetAndCreatedAtBetween(1L, start, end);
            verify(workOrderService, times(1)).getAllCost(anyCollection(), eq(true));
        }

        @Test
        void testGetTotalCostNoWorkOrders() {
            Date start = new Date();
            Date end = new Date();
            List<WorkOrder> workOrders = Collections.emptyList(); // Corrected to List

            when(workOrderService.findByAssetAndCreatedAtBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(workOrders);
            when(workOrderService.getAllCost(anyCollection(), anyBoolean())).thenReturn(0.0);

            double totalCost = assetService.getTotalCost(1L, start, end, true);

            assertEquals(0.0, totalCost);
            verify(workOrderService, times(1)).findByAssetAndCreatedAtBetween(1L, start, end);
            verify(workOrderService, times(1)).getAllCost(anyCollection(), eq(true));
        }
    }
}
