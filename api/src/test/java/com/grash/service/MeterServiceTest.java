package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.MeterPatchDTO;
import com.grash.dto.MeterPostDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.imports.MeterImportDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterMapper;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.MeterRepository;
import com.grash.utils.Consts;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterServiceTest {

    @InjectMocks
    private MeterService meterService;

    @Mock
    private MeterRepository meterRepository;
    @Mock
    private MeterCategoryService meterCategoryService;
    @Mock
    private AssetService assetService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private LocationService locationService;
    @Mock
    private UserService userService;
    @Mock
    private EntityManager em;
    @Mock
    private MeterMapper meterMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private CustomFieldValueService customFieldValueService;

    private Company company;
    private CompanySettings companySettings;
    private User user;
    private Role adminRole;
    private Role restrictedRole;

    @BeforeEach
    void setUp() {
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.builder()
                .id(1L)
                .name("Pro")
                .features(new HashSet<>(Collections.singletonList(PlanFeatures.METER)))
                .build();
        Subscription subscription = Subscription.builder()
                .id(1L)
                .subscriptionPlan(subscriptionPlan)
                .build();
        companySettings = new CompanySettings();
        companySettings.setId(1L);
        company = new Company("TestCo", 10, subscription);
        company.setId(1L);
        company.setCompanySettings(companySettings);

        adminRole = adminRole();
        restrictedRole = restrictedRole();

        user = buildUser(1L);
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private Role buildRole(Set<PermissionEntity> view, Set<PermissionEntity> viewOther,
                           Set<PermissionEntity> create, Set<PermissionEntity> editOther,
                           Set<PermissionEntity> deleteOther) {
        return Role.builder()
                .id(1L)
                .name("Role")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .viewPermissions(view)
                .viewOtherPermissions(viewOther)
                .createPermissions(create)
                .editOtherPermissions(editOther)
                .deleteOtherPermissions(deleteOther)
                .build();
    }

    private Role adminRole() {
        Set<PermissionEntity> meters = new HashSet<>(Collections.singletonList(PermissionEntity.METERS));
        return buildRole(meters, meters, meters, meters, meters);
    }

    private Role restrictedRole() {
        return buildRole(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("U" + id);
        u.setLastName("L" + id);
        u.setEmail("u" + id + "@test.com");
        u.setRole(adminRole);
        u.setCompany(company);
        u.setEnabled(true);
        u.setSuperAccountRelations(new ArrayList<>());
        u.setUserSettings(new UserSettings());
        return u;
    }

    private Meter buildMeter(Long id) {
        Meter m = new Meter();
        m.setId(id);
        m.setName("Meter" + id);
        m.setUpdateFrequency(1);
        m.setCompany(company);
        m.setCreatedBy(1L);
        m.setUsers(new ArrayList<>());
        m.setCustomFieldValues(new ArrayList<>());
        Asset asset = new Asset();
        asset.setId(1L);
        m.setAsset(asset);
        return m;
    }

    private MeterPatchDTO buildPatchDTO() {
        return new MeterPatchDTO();
    }

    private void stubPartLimit(boolean hasEntitlement, boolean hasMore) {
        lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_METERS))
                .thenReturn(hasEntitlement);
        lenient().when(meterRepository.hasMoreThan(eq(company.getId()), anyLong())).thenReturn(hasMore);
    }

    private void stubMessageKeys() {
        lenient().when(messageSource.getMessage(eq("new_assignment"), isNull(), any(Locale.class)))
                .thenReturn("New assignment");
        lenient().when(messageSource.getMessage(eq("notification_meter_assigned"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Meter assigned");
    }

    @Nested
    class Create {

        @Test
        void savesMeter() {
            MeterPostDTO dto = new MeterPostDTO();
            dto.setName("New Meter");
            Meter mapped = buildMeter(1L);
            when(meterMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(meterRepository.saveAndFlush(mapped)).thenReturn(mapped);

            Meter result = meterService.create(dto, user);

            assertSame(mapped, result);
            verify(em).refresh(mapped);
        }

        @Test
        void overFreeLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_METERS)).thenReturn(false);
            when(meterRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_METERS) - 1))))
                    .thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.create(new MeterPostDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(meterRepository, never()).saveAndFlush(any());
        }

        @Test
        void withEntitlement_bypassesLimit() {
            MeterPostDTO dto = new MeterPostDTO();
            Meter mapped = buildMeter(1L);
            when(meterMapper.fromPostDto(dto)).thenReturn(mapped);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_METERS)).thenReturn(true);
            when(meterRepository.saveAndFlush(mapped)).thenReturn(mapped);

            meterService.create(dto, user);

            verify(meterRepository, never()).hasMoreThan(anyLong(), anyLong());
        }

        @Test
        void withoutCreatePermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.create(new MeterPostDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void planWithoutMeterFeature_throwsForbidden() {
            SubscriptionPlan basicPlan = SubscriptionPlan.builder()
                    .id(2L)
                    .name("Basic")
                    .features(new HashSet<>())
                    .build();
            Subscription basicSubscription = Subscription.builder()
                    .id(2L)
                    .subscriptionPlan(basicPlan)
                    .build();
            Company basicCompany = new Company("BasicCo", 5, basicSubscription);
            basicCompany.setId(2L);
            User basicUser = buildUser(2L);
            basicUser.setCompany(basicCompany);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.create(new MeterPostDTO(), basicUser));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withCustomFields_setsCustomFields() {
            MeterPostDTO dto = new MeterPostDTO();
            dto.setCustomFields(new ArrayList<>(Collections.singletonList(new CustomFieldValuePostDTO())));
            Meter mapped = buildMeter(1L);
            when(meterMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(meterRepository.saveAndFlush(mapped)).thenReturn(mapped);

            meterService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), eq(dto.getCustomFields()),
                    eq(company), eq(CustomFieldEntityType.METER), any());
        }

        @Test
        void withCustomFieldsEmpty_skipsCustomFields() {
            MeterPostDTO dto = new MeterPostDTO();
            Meter mapped = buildMeter(1L);
            when(meterMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(meterRepository.saveAndFlush(mapped)).thenReturn(mapped);

            meterService.create(dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }

        @Test
        void withNullCustomFields_skipsCustomFields() {
            MeterPostDTO dto = new MeterPostDTO();
            dto.setCustomFields(null);
            Meter mapped = buildMeter(1L);
            when(meterMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(meterRepository.saveAndFlush(mapped)).thenReturn(mapped);

            meterService.create(dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }

        @Test
        void customFieldConsumer_setsMeterOnCustomFieldValue() {
            MeterPostDTO dto = new MeterPostDTO();
            dto.setCustomFields(new ArrayList<>(Collections.singletonList(new CustomFieldValuePostDTO())));
            Meter mapped = buildMeter(1L);
            when(meterMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(meterRepository.saveAndFlush(mapped)).thenReturn(mapped);

            doAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Consumer<CustomFieldValue> consumer = inv.getArgument(5);
                CustomFieldValue cfv = new CustomFieldValue();
                consumer.accept(cfv);
                assertSame(mapped, cfv.getMeter());
                return null;
            }).when(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.METER), any());

            meterService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.METER), any());
        }
    }

    @Nested
    class Patch {

        @Test
        void patchesExistingMeter() {
            Meter saved = buildMeter(1L);
            MeterPatchDTO dto = buildPatchDTO();
            when(meterRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(meterMapper.updateMeter(saved, dto)).thenReturn(saved);
            when(meterRepository.saveAndFlush(saved)).thenReturn(saved);

            Meter result = meterService.patch(1L, dto, user);

            assertSame(saved, result);
            verify(em).detach(saved);
            verify(em).refresh(saved);
        }

        @Test
        void notFound_throwsNotFound() {
            when(meterRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotEdit_throwsForbidden() {
            user.setRole(restrictedRole());
            Meter saved = buildMeter(1L);
            saved.setCreatedBy(999L);
            when(meterRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withCustomFields_appliesCustomFields() {
            Meter saved = buildMeter(1L);
            MeterPatchDTO dto = buildPatchDTO();
            dto.setCustomFields(new ArrayList<>(Collections.singletonList(new CustomFieldValuePostDTO())));
            when(meterRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(meterMapper.updateMeter(saved, dto)).thenReturn(saved);
            when(meterRepository.saveAndFlush(saved)).thenReturn(saved);

            meterService.patch(1L, dto, user);

            verify(customFieldValueService).setCustomFields(eq(saved), eq(saved.getCustomFieldValues()),
                    eq(dto.getCustomFields()), eq(company), eq(CustomFieldEntityType.METER), any());
        }

        @Test
        void withCustomFieldsEmpty_skipsCustomFields() {
            Meter saved = buildMeter(1L);
            MeterPatchDTO dto = buildPatchDTO();
            when(meterRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(meterMapper.updateMeter(saved, dto)).thenReturn(saved);
            when(meterRepository.saveAndFlush(saved)).thenReturn(saved);

            meterService.patch(1L, dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }

        @Test
        void withNullCustomFields_skipsCustomFields() {
            Meter saved = buildMeter(1L);
            MeterPatchDTO dto = buildPatchDTO();
            dto.setCustomFields(null);
            when(meterRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(meterMapper.updateMeter(saved, dto)).thenReturn(saved);
            when(meterRepository.saveAndFlush(saved)).thenReturn(saved);

            meterService.patch(1L, dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsMeter() {
            Meter meter = buildMeter(1L);
            when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));

            assertSame(meter, meterService.getById(1L, user));
        }

        @Test
        void notFound_throwsNotFound() {
            when(meterRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> meterService.getById(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void accessDenied_throwsForbidden() {
            user.setRole(restrictedRole());
            when(meterRepository.findById(1L)).thenReturn(Optional.of(buildMeter(1L)));

            CustomException ex = assertThrows(CustomException.class, () -> meterService.getById(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteAndAccess {

        @Test
        void delete_deletesById() {
            meterService.delete(1L);

            verify(meterRepository).deleteById(1L);
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            when(meterRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_forbidden_throwsForbidden() {
            user.setRole(restrictedRole());
            Meter meter = buildMeter(1L);
            meter.setCreatedBy(999L);
            when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_deletes() {
            Meter meter = buildMeter(1L);
            when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));

            meterService.deleteByIdAndUser(1L, user);

            verify(meterRepository).deleteById(1L);
        }

        @Test
        void findById_returnsOptional() {
            Meter meter = buildMeter(1L);
            when(meterRepository.findById(1L)).thenReturn(Optional.of(meter));

            assertEquals(Optional.of(meter), meterService.findById(1L));
        }

        @Test
        void findByIdAndCompany_returnsOptional() {
            Meter meter = buildMeter(1L);
            when(meterRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(meter));

            assertEquals(Optional.of(meter), meterService.findByIdAndCompany(1L, 1L));
        }

        @Test
        void getAll_returnsAll() {
            List<Meter> meters = Collections.singletonList(buildMeter(1L));
            when(meterRepository.findAll()).thenReturn(meters);

            assertSame(meters, meterService.getAll());
        }

        @Test
        void findByCompany_returnsCompanyMeters() {
            List<Meter> meters = Collections.singletonList(buildMeter(1L));
            when(meterRepository.findByCompany_Id(1L)).thenReturn(meters);

            assertSame(meters, meterService.findByCompany(1L));
        }

        @Test
        void findByAsset_returnsAssetMeters() {
            List<Meter> meters = Collections.singletonList(buildMeter(1L));
            when(meterRepository.findByAsset_Id(1L)).thenReturn(meters);

            assertSame(meters, meterService.findByAsset(1L));
        }

        @Test
        void findByCompanyForExport_returnsPage() {
            Page<Meter> page = new PageImpl<>(Collections.singletonList(buildMeter(1L)));
            when(meterRepository.findByCompanyForExport(eq(1L), any(Pageable.class))).thenReturn(page);

            assertSame(page, meterService.findByCompanyForExport(1L, Pageable.unpaged()));
        }

        @Test
        void saveAll_savesAll() {
            List<Meter> meters = Collections.singletonList(buildMeter(1L));
            when(meterRepository.saveAll(meters)).thenReturn(meters);

            assertSame(meters, meterService.saveAll(meters));
        }

        @Test
        void findByIdsAndCompany_returnsMeters() {
            List<Meter> meters = Collections.singletonList(buildMeter(1L));
            when(meterRepository.findByIdInAndCompany_Id(Collections.singletonList(1L), 1L)).thenReturn(meters);

            assertSame(meters, meterService.findByIdsAndCompany(Collections.singletonList(1L), 1L));
        }
    }

    @Nested
    class GetSearchCriteria {

        @Test
        void clientWithViewOther_filtersByCompanyOnly() {
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = meterService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(1, result.getFilterFields().size());
            assertEquals("company", result.getFilterFields().get(0).getField());
        }

        @Test
        void clientWithoutViewOther_filtersByCompanyAndCreatedBy() {
            Set<PermissionEntity> metersOnly =
                    new HashSet<>(Collections.singletonList(PermissionEntity.METERS));
            user.setRole(buildRole(metersOnly, new HashSet<>(), metersOnly, new HashSet<>(), new HashSet<>()));
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = meterService.getSearchCriteria(user, criteria);

            assertEquals(2, result.getFilterFields().size());
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("company")));
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("createdBy")));
        }

        @Test
        void clientWithoutPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.getSearchCriteria(user, new SearchCriteria()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void superAdmin_returnsCriteriaUnchanged() {
            Role superAdmin = buildRole(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(),
                    new HashSet<>());
            superAdmin.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            user.setRole(superAdmin);
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = meterService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(0, result.getFilterFields().size());
        }
    }

    @Nested
    class FindBySearchCriteria {

        @Test
        void withoutFilters_buildsNullSpecification() {
            SearchCriteria criteria = new SearchCriteria();
            Page<Meter> page = new PageImpl<>(Collections.singletonList(buildMeter(1L)));
            when(meterRepository.findAll((Specification<Meter>) isNull(), any(Pageable.class))).thenReturn(page);

            Page<Meter> result = meterService.findBySearchCriteria(criteria);

            assertSame(page, result);
        }

        @Test
        void withFilters_buildsSpecification() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.getFilterFields().add(FilterField.builder()
                    .field("name")
                    .value("Meter")
                    .operation("eq")
                    .build());
            Page<Meter> page = new PageImpl<>(Collections.singletonList(buildMeter(1L)));
            when(meterRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<Meter> result = meterService.findBySearchCriteria(criteria);

            assertSame(page, result);
        }
    }

    @Nested
    class ImportMeter {

        @Test
        void overFreeLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_METERS)).thenReturn(false);
            when(meterRepository.hasMoreThan(eq(1L), anyLong())).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> meterService.importMeter(new Meter(), new MeterImportDTO(), company));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void setsFieldsAndAssociations() {
            stubPartLimit(false, false);
            Meter meter = new Meter();
            MeterImportDTO dto = MeterImportDTO.builder()
                    .name("Imported")
                    .unit("kWh")
                    .updateFrequency(7)
                    .locationName("Main Plant")
                    .assetName("Compressor")
                    .meterCategory("Electric")
                    .usersEmails(Arrays.asList("a@test.com", "b@test.com"))
                    .build();
            Location location = new Location();
            when(locationService.findByNameIgnoreCaseAndCompany("Main Plant", 1L))
                    .thenReturn(Collections.singletonList(location));
            Asset asset = new Asset();
            when(assetService.findByNameIgnoreCaseAndCompany("Compressor", 1L))
                    .thenReturn(Collections.singletonList(asset));
            MeterCategory category = new MeterCategory();
            when(meterCategoryService.getOrCreate("Electric", companySettings)).thenReturn(category);
            User assigned = buildUser(7L);
            when(userService.findByEmailAndCompany("a@test.com", 1L)).thenReturn(Optional.of(assigned));
            when(userService.findByEmailAndCompany("b@test.com", 1L)).thenReturn(Optional.empty());

            meterService.importMeter(meter, dto, company);

            assertEquals("Imported", meter.getName());
            assertEquals("kWh", meter.getUnit());
            assertEquals(7, meter.getUpdateFrequency());
            assertSame(location, meter.getLocation());
            assertSame(asset, meter.getAsset());
            assertSame(category, meter.getMeterCategory());
            assertEquals(Collections.singletonList(assigned), meter.getUsers());
        }

        @Test
        void withoutData_keepsAssociationsNull() {
            stubPartLimit(false, false);
            MeterImportDTO dto = MeterImportDTO.builder().name("Imported").updateFrequency(1).build();
            Meter meter = new Meter();

            meterService.importMeter(meter, dto, company);

            assertNull(meter.getLocation());
            assertNull(meter.getAsset());
            assertNull(meter.getMeterCategory());
            assertTrue(meter.getUsers().isEmpty());
            verify(meterCategoryService, never()).getOrCreate(anyString(), any());
        }

        @Test
        void blankMeterCategory_skipsCategoryLookup() {
            stubPartLimit(false, false);
            MeterImportDTO dto = MeterImportDTO.builder()
                    .name("Imported")
                    .updateFrequency(1)
                    .meterCategory("   ")
                    .build();
            Meter meter = new Meter();

            meterService.importMeter(meter, dto, company);

            assertNull(meter.getMeterCategory());
            verify(meterCategoryService, never()).getOrCreate(anyString(), any());
        }
    }

    @Nested
    class Notify {

        @Test
        void notifiesAssignedUsers() {
            Meter meter = buildMeter(1L);
            User assigned = buildUser(5L);
            meter.setUsers(Collections.singletonList(assigned));
            stubMessageKeys();

            meterService.notify(meter, Locale.ENGLISH);

            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
        }

        @Test
        void nullUsers_doesNotNotify() {
            Meter meter = buildMeter(1L);
            meter.setUsers(null);

            meterService.notify(meter, Locale.ENGLISH);

            verify(notificationService, never()).createMultiple(anyList(), eq(true), anyString());
        }
    }

    @Nested
    class PatchNotify {

        @Test
        void notifiesOnlyNewUsers() {
            User u1 = buildUser(1L);
            User u2 = buildUser(2L);
            Meter oldMeter = buildMeter(1L);
            oldMeter.setUsers(new ArrayList<>(Collections.singletonList(u1)));
            Meter newMeter = buildMeter(2L);
            newMeter.setUsers(new ArrayList<>(Arrays.asList(u1, u2)));
            stubMessageKeys();

            meterService.patchNotify(oldMeter, newMeter, Locale.ENGLISH);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationService).createMultiple(captor.capture(), eq(true), anyString());
            List<Notification> created = captor.getValue();
            assertEquals(1, created.size());
            assertEquals(u2.getId(), created.get(0).getUser().getId());
        }

        @Test
        void nullUsers_doesNotNotify() {
            Meter oldMeter = buildMeter(1L);
            Meter newMeter = buildMeter(2L);
            newMeter.setUsers(null);

            meterService.patchNotify(oldMeter, newMeter, Locale.ENGLISH);

            verify(notificationService, never()).createMultiple(anyList(), eq(true), anyString());
        }
    }
}