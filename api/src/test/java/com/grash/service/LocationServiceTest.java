package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationPostDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.dto.imports.LocationImportDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.LocationMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.LocationRepository;
import com.grash.utils.Consts;
import io.github.bucket4j.Bucket;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.grash.security.ClientIpResolver;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @InjectMocks
    private LocationService locationService;

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private UserService userService;
    @Mock
    private CustomerService customerService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private VendorService vendorService;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TeamService teamService;
    @Mock
    private EntityManager em;
    @Mock
    private CustomSequenceService customSequenceService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private WebhookDispatchService webhookDispatchService;
    @Mock
    private CustomFieldValueService customFieldValueService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private RequestPortalService requestPortalService;
    @Mock
    private ClientIpResolver clientIpResolver;

    private Company company;
    private User user;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;
    private CompanySettings companySettings;
    private GeneralPreferences generalPreferences;

    @BeforeEach
    void setUp() {
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
        Set<PermissionEntity> all = new HashSet<>(Collections.singletonList(PermissionEntity.LOCATIONS));
        return buildRole(RoleType.ROLE_CLIENT, all, all, all, all, all);
    }

    private Role restrictedRole() {
        return buildRole(RoleType.ROLE_CLIENT, new HashSet<>(), new HashSet<>(), new HashSet<>(),
                new HashSet<>(), new HashSet<>());
    }

    private Location buildLocation(Long id) {
        Location location = new Location();
        location.setId(id);
        location.setName("Location" + id);
        location.setAddress("Address " + id);
        location.setCompany(company);
        location.setCreatedBy(user.getId());
        location.setTeams(new ArrayList<>());
        location.setWorkers(new ArrayList<>());
        location.setCustomFieldValues(new ArrayList<>());
        return location;
    }

    private User buildWorker(Long id) {
        User worker = new User();
        worker.setId(id);
        worker.setFirstName("Worker" + id);
        worker.setLastName("Last" + id);
        worker.setEmail("worker" + id + "@test.com");
        worker.setRole(role);
        worker.setCompany(company);
        worker.setEnabled(true);
        worker.setSuperAccountRelations(new ArrayList<>());
        worker.setUserSettings(new UserSettings());
        return worker;
    }

    private LocationPostDTO buildPostDTO() {
        LocationPostDTO dto = new LocationPostDTO();
        dto.setName("New Location");
        dto.setCompany(company);
        return dto;
    }

    private LocationPatchDTO buildPatchDTO() {
        LocationPatchDTO dto = new LocationPatchDTO();
        dto.setName("Patched Location");
        return dto;
    }

    private void stubMessageKeys() {
        lenient().when(messageSource.getMessage(eq("new_assignment"), isNull(), any(Locale.class)))
                .thenReturn("New assignment");
        lenient().when(messageSource.getMessage(eq("notification_location_assigned"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Location assigned");
    }

    private void stubLocationLimit(boolean hasEntitlement, boolean hasMore) {
        lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_LOCATIONS)).thenReturn(hasEntitlement);
        lenient().when(locationRepository.hasMoreThan(eq(company.getId()), anyLong())).thenReturn(hasMore);
    }

    private void stubCreatePipeline(Long sequence) {
        when(customSequenceService.getNextLocationSequence(company)).thenReturn(sequence);
        when(locationRepository.saveAndFlush(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
        when(locationMapper.toShowDto(any(Location.class), any(LocationService.class)))
                .thenReturn(new LocationShowDTO());
    }

    @Nested
    class Create {

        @Test
        void assignsCustomIdAndSaves() {
            Location location = buildLocation(1L);
            when(locationMapper.fromPostDto(any(LocationPostDTO.class))).thenReturn(location);
            stubLocationLimit(false, false);
            stubCreatePipeline(42L);

            Location result = locationService.create(buildPostDTO(), user);

            assertEquals("L000042", result.getCustomId());
            verify(locationRepository).saveAndFlush(any(Location.class));
            verify(em).refresh(any(Location.class));
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.NEW_LOCATION), anyMap(),
                    eq("newLocation"), any(), isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        void withoutCreatePermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.create(buildPostDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(locationRepository, never()).saveAndFlush(any());
        }

        @Test
        void overFreeLimit_throwsForbidden() {
            stubLocationLimit(false, true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.create(buildPostDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withUnlimitedLocationsEntitlement_bypassesLimit() {
            when(locationMapper.fromPostDto(any(LocationPostDTO.class))).thenReturn(buildLocation(1L));
            stubLocationLimit(true, false);
            stubCreatePipeline(2L);

            locationService.create(buildPostDTO(), user);

            verify(locationRepository, never()).hasMoreThan(anyLong(), anyLong());
        }

        @Test
        void withPostDto_mapsAndSetsCustomFields() {
            LocationPostDTO dto = buildPostDTO();
            dto.setCustomFields(Collections.singletonList(new com.grash.dto.cutomField.CustomFieldValuePostDTO()));
            Location mapped = buildLocation(1L);
            when(locationMapper.fromPostDto(any(LocationPostDTO.class))).thenReturn(mapped);
            stubLocationLimit(false, false);
            stubCreatePipeline(10L);

            locationService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.LOCATION), any());
        }

        @Test
        void withPostDto_withoutCustomFields_skipsCustomFieldMapping() {
            LocationPostDTO dto = buildPostDTO();
            Location mapped = buildLocation(1L);
            when(locationMapper.fromPostDto(any(LocationPostDTO.class))).thenReturn(mapped);
            stubLocationLimit(false, false);
            stubCreatePipeline(10L);

            locationService.create(dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }

        @Test
        void withPostDto_customFieldConsumer_setsLocationOnCfValue() {
            LocationPostDTO dto = buildPostDTO();
            dto.setCustomFields(Collections.singletonList(new com.grash.dto.cutomField.CustomFieldValuePostDTO()));
            Location mapped = buildLocation(1L);
            when(locationMapper.fromPostDto(any(LocationPostDTO.class))).thenReturn(mapped);
            stubLocationLimit(false, false);
            stubCreatePipeline(10L);

            doAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Consumer<CustomFieldValue> consumer = inv.getArgument(5);
                CustomFieldValue cfv = new CustomFieldValue();
                consumer.accept(cfv);
                assertSame(mapped, cfv.getLocation());
                return null;
            }).when(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.LOCATION), any());

            locationService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.LOCATION), any());
        }

        @Test
        void notifiesAssignedUsers() {
            LocationPostDTO dto = buildPostDTO();
            Location location = buildLocation(1L);
            location.setWorkers(Collections.singletonList(buildWorker(2L)));
            when(locationMapper.fromPostDto(any(LocationPostDTO.class))).thenReturn(location);
            stubLocationLimit(false, false);
            stubCreatePipeline(7L);
            stubMessageKeys();

            locationService.create(dto, user);

            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
        }
    }

    @Nested
    class Patch {

        @Test
        void patchesExistingLocation() {
            Location saved = buildLocation(1L);
            LocationPatchDTO dto = buildPatchDTO();
            when(locationRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(locationMapper.updateLocation(any(Location.class), any(LocationPatchDTO.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(locationRepository.saveAndFlush(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
            stubMessageKeys();

            Location result = locationService.patch(1L, dto, user);

            assertSame(saved, result);
            verify(em).detach(saved);
            verify(locationRepository).saveAndFlush(saved);
            verify(em).refresh(saved);
        }

        @Test
        void locationNotFound_throwsNotFound() {
            when(locationRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotEdit_throwsForbidden() {
            user.setRole(restrictedRole());
            Location saved = buildLocation(1L);
            saved.setCreatedBy(999L);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void parentLocationSameId_throwsNotAcceptable() {
            Location saved = buildLocation(1L);
            LocationPatchDTO dto = buildPatchDTO();
            Location parent = new Location();
            parent.setId(1L);
            dto.setParentLocation(parent);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.patch(1L, dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void withCustomFields_appliesCustomFields() {
            Location saved = buildLocation(1L);
            LocationPatchDTO dto = buildPatchDTO();
            dto.setCustomFields(Collections.singletonList(new com.grash.dto.cutomField.CustomFieldValuePostDTO()));
            when(locationRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(locationMapper.updateLocation(any(Location.class), any(LocationPatchDTO.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(locationRepository.saveAndFlush(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
            stubMessageKeys();

            locationService.patch(1L, dto, user);

            verify(customFieldValueService).setCustomFields(eq(saved), eq(saved.getCustomFieldValues()),
                    eq(dto.getCustomFields()), eq(company), eq(CustomFieldEntityType.LOCATION), any());
        }

        @Test
        void notifiesNewlyAssignedWorkers() {
            Location saved = buildLocation(1L);
            Location patched = buildLocation(1L);
            patched.setWorkers(Collections.singletonList(buildWorker(2L)));
            LocationPatchDTO dto = buildPatchDTO();
            when(locationRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(locationMapper.updateLocation(any(Location.class), any(LocationPatchDTO.class))).thenReturn(patched);
            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(patched);
            stubMessageKeys();

            locationService.patch(1L, dto, user);

            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsLocation() {
            Location location = buildLocation(1L);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

            Location result = locationService.getById(1L, user);

            assertSame(location, result);
        }

        @Test
        void notFound_throwsNotFound() {
            when(locationRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getById(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void accessDenied_throwsForbidden() {
            user.setRole(restrictedRole());
            Location location = buildLocation(1L);
            location.setCreatedBy(999L);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getById(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class GetChildren {

        @Test
        void withoutViewPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getChildren(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void rootIdForClient_returnsTopLevelLocations() {
            Location location = buildLocation(1L);
            when(locationRepository.findByCompany_Id(1L))
                    .thenReturn(Collections.singletonList(location));

            Collection<Location> result = locationService.getChildren(0L, user);

            assertEquals(Collections.singletonList(location), result);
        }

        @Test
        void locationNotFound_throwsNotFound() {
            when(locationRepository.findById(5L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getChildren(5L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void returnsChildren() {
            Location location = buildLocation(5L);
            Location child = buildLocation(6L);
            when(locationRepository.findById(5L)).thenReturn(Optional.of(location));
            when(locationRepository.findByParentLocation_Id(eq(5L), eq(Pageable.unpaged())))
                    .thenReturn(new PageImpl<>(Collections.singletonList(child)));

            Collection<Location> result = locationService.getChildren(5L, user);

            assertEquals(Collections.singletonList(child), result);
        }
    }

    @Nested
    class GetChildrenPaginated {

        @Test
        void withoutViewPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getChildrenPaginated(1L, Pageable.unpaged(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void rootIdForClient_returnsTopLevelLocations() {
            Location location = buildLocation(1L);
            when(locationRepository.findByCompany_IdAndParentLocationIsNull(1L, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(location)));

            Page<Location> result = locationService.getChildrenPaginated(0L, Pageable.unpaged(), user);

            assertEquals(Collections.singletonList(location), result.getContent());
        }

        @Test
        void locationNotFound_throwsNotFound() {
            when(locationRepository.findById(5L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getChildrenPaginated(5L, Pageable.unpaged(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void returnsChildren() {
            Location location = buildLocation(5L);
            Location child = buildLocation(6L);
            when(locationRepository.findById(5L)).thenReturn(Optional.of(location));
            when(locationRepository.findByParentLocation_Id(eq(5L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(child)));

            Page<Location> result = locationService.getChildrenPaginated(5L, PageRequest.of(0, 10), user);

            assertEquals(Collections.singletonList(child), result.getContent());
        }
    }

    @Nested
    class GetMiniPublic {

        private Bucket bucket;
        private HttpServletRequest request;

        @BeforeEach
        void initBucketAndRequest() {
            bucket = mock(Bucket.class);
            request = mock(HttpServletRequest.class);
            when(clientIpResolver.resolve(request)).thenReturn("1.2.3.4");
            when(rateLimiterService.resolvePublicMiniBucket("1.2.3.4")).thenReturn(bucket);
        }

        @Test
        void rateLimitExceeded_throwsTooManyRequests() {
            when(bucket.tryConsume(1)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getMiniPublic("uuid", request));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
        }

        @Test
        void portalWithLocationField_throwsForbidden() {
            when(bucket.tryConsume(1)).thenReturn(true);
            RequestPortal portal = new RequestPortal();
            portal.setCompany(company);
            RequestPortalField field = new RequestPortalField();
            field.setType(PortalFieldType.LOCATION);
            field.setLocation(buildLocation(1L));
            portal.setFields(Collections.singletonList(field));
            when(requestPortalService.findByUuidByUser("uuid")).thenReturn(Optional.of(portal));

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getMiniPublic("uuid", request));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void returnsCompanyLocations() {
            when(bucket.tryConsume(1)).thenReturn(true);
            RequestPortal portal = new RequestPortal();
            portal.setCompany(company);
            when(requestPortalService.findByUuidByUser("uuid")).thenReturn(Optional.of(portal));
            List<Location> locations = Collections.singletonList(buildLocation(1L));
            when(locationRepository.findByCompany_Id(1L)).thenReturn(locations);

            Collection<Location> result = locationService.getMiniPublic("uuid", request);

            assertSame(locations, result);
        }
    }

    @Nested
    class DeleteByIdAndUser {

        @Test
        void deletesLocation() {
            Location location = buildLocation(1L);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

            locationService.deleteByIdAndUser(1L, user);

            verify(locationRepository).deleteById(1L);
        }

        @Test
        void locationNotFound_throwsNotFound() {
            when(locationRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void forbidden_throwsForbidden() {
            user.setRole(restrictedRole());
            Location location = buildLocation(1L);
            location.setCreatedBy(999L);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(locationRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    class GetSearchCriteria {

        @Test
        void clientWithViewOther_filtersByCompanyOnly() {
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = locationService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(1, result.getFilterFields().size());
            assertEquals("company", result.getFilterFields().get(0).getField());
        }

        @Test
        void clientWithoutViewOther_filtersByCompanyAndCreatedBy() {
            Set<PermissionEntity> locationsOnly = new HashSet<>(
                    Collections.singletonList(PermissionEntity.LOCATIONS));
            Role roleNoViewOther = buildRole(RoleType.ROLE_CLIENT, locationsOnly, new HashSet<>(), locationsOnly,
                    new HashSet<>(), new HashSet<>());
            user.setRole(roleNoViewOther);
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = locationService.getSearchCriteria(user, criteria);

            assertEquals(2, result.getFilterFields().size());
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("company")));
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("createdBy")));
        }

        @Test
        void clientWithoutLocationPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> locationService.getSearchCriteria(user, new SearchCriteria()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void superAdmin_returnsCriteriaUnchanged() {
            user.setRole(buildRole(RoleType.ROLE_SUPER_ADMIN, new HashSet<>(), new HashSet<>(), new HashSet<>(),
                    new HashSet<>(), new HashSet<>()));
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = locationService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(0, result.getFilterFields().size());
        }
    }

    @Nested
    class FindBySearchCriteria {

        @Test
        void delegatesToRepository() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setDirection(Sort.Direction.DESC);
            criteria.setSortField("name");
            when(locationRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(buildLocation(1L))));

            Page<Location> result = locationService.findBySearchCriteria(criteria);

            assertEquals(1, result.getTotalElements());
            verify(locationRepository).findAll(nullable(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    class HasChildren {

        @Test
        void returnsTrueWhenChildrenExist() {
            when(locationRepository.countByParentLocation_Id(1L)).thenReturn(2);

            assertTrue(locationService.hasChildren(1L));
        }

        @Test
        void returnsFalseWhenNoChildren() {
            when(locationRepository.countByParentLocation_Id(1L)).thenReturn(0);

            assertFalse(locationService.hasChildren(1L));
        }
    }

    @Nested
    class CheckUsageBasedLimit {

        @Test
        void underLimit_noException() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_LOCATIONS)).thenReturn(false);
            when(locationRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_LOCATIONS) - 1))))
                    .thenReturn(false);

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(locationService, "checkUsageBasedLimit", company));
        }

        @Test
        void atLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_LOCATIONS)).thenReturn(false);
            when(locationRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_LOCATIONS) - 1))))
                    .thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> ReflectionTestUtils.invokeMethod(locationService, "checkUsageBasedLimit", company));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void withEntitlement_bypassesLimit() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_LOCATIONS)).thenReturn(true);

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(locationService, "checkUsageBasedLimit", company));

            verify(locationRepository, never()).hasMoreThan(anyLong(), anyLong());
        }
    }

    @Nested
    class OrderLocations {

        private LocationImportDTO location(String name, String parentName) {
            return LocationImportDTO.builder().name(name).parentLocationName(parentName).build();
        }

        @Test
        void emptyList_returnsEmpty() {
            assertTrue(LocationService.orderLocations(new ArrayList<>()).isEmpty());
        }

        @Test
        void singleTopLevelLocation_returnsAsIs() {
            LocationImportDTO a = location("A", null);

            List<LocationImportDTO> result = LocationService.orderLocations(Collections.singletonList(a));

            assertEquals(Collections.singletonList(a), result);
        }

        @Test
        void parentListedBeforeChild() {
            LocationImportDTO a = location("A", null);
            LocationImportDTO b = location("B", "A");
            List<LocationImportDTO> input = new ArrayList<>(Arrays.asList(b, a));

            List<LocationImportDTO> result = LocationService.orderLocations(input);

            assertEquals(Arrays.asList(a, b), result);
        }

        @Test
        void nestedHierarchy_ordersRecursively() {
            LocationImportDTO a = location("A", null);
            LocationImportDTO b = location("B", "A");
            LocationImportDTO c = location("C", "B");
            List<LocationImportDTO> input = new ArrayList<>(Arrays.asList(c, a, b));

            List<LocationImportDTO> result = LocationService.orderLocations(input);

            assertEquals(Arrays.asList(a, b, c), result);
        }

        @Test
        void childWithMissingParent_isTreatedAsTopLevel() {
            LocationImportDTO a = location("A", "MissingParent");

            List<LocationImportDTO> result = LocationService.orderLocations(Collections.singletonList(a));

            assertEquals(Collections.singletonList(a), result);
        }

        @Test
        void orderLocationsRecursive_nullLevels_isNoop() throws Exception {
            List<LocationImportDTO> ordered = new ArrayList<>();
            Set<LocationImportDTO> visited = new HashSet<>();

            Method method = LocationService.class.getDeclaredMethod("orderLocationsRecursive",
                    Map.class, List.class, List.class, Set.class);
            method.setAccessible(true);
            method.invoke(null, new HashMap<String, List<LocationImportDTO>>(), null, ordered, visited);

            assertTrue(ordered.isEmpty());
        }
    }
}