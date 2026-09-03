package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenancePostDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.PreventiveMaintenanceRepository;
import com.grash.utils.Consts;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreventiveMaintenanceServiceTest {

    @InjectMocks
    private PreventiveMaintenanceService preventiveMaintenanceService;

    @Mock
    private PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    @Mock
    private EntityManager em;
    @Mock
    private CustomSequenceService customSequenceService;
    @Mock
    private PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    @Mock
    private LocationService locationService;
    @Mock
    private TeamService teamService;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private WorkOrderCategoryService workOrderCategoryService;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private CustomFieldValueService customFieldValueService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private TaskService taskService;

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
                .features(new HashSet<>(Collections.singletonList(PlanFeatures.PREVENTIVE_MAINTENANCE)))
                .build();
        subscription = Subscription.builder()
                .id(1L)
                .subscriptionPlan(subscriptionPlan)
                .build();
        companySettings = new CompanySettings();
        companySettings.setId(1L);
        generalPreferences = new GeneralPreferences(companySettings);
        com.grash.model.Currency currency = new com.grash.model.Currency();
        currency.setId(1L);
        currency.setCode("$");
        currency.setName("USD");
        generalPreferences.setCurrency(currency);
        companySettings.setGeneralPreferences(generalPreferences);
        company = new Company("TestCo", 10, subscription);
        company.setId(1L);
        company.setCompanySettings(companySettings);

        role = Role.builder()
                .id(1L)
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                .build();

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

    private PreventiveMaintenance buildPM(Long id) {
        PreventiveMaintenance pm = new PreventiveMaintenance();
        pm.setId(id);
        pm.setName("Test PM");
        pm.setTitle("Test PM Title");
        pm.setCompany(company);
        pm.setCreatedBy(user.getId());
        pm.setPriority(Priority.NONE);
        pm.setEstimatedDuration(1.0);
        pm.setCustomFieldValues(new ArrayList<>());
        pm.setAssignedTo(new ArrayList<>());
        pm.setCustomers(new ArrayList<>());
        pm.setFiles(new ArrayList<>());
        Schedule schedule = new Schedule(pm);
        schedule.setId(100L);
        schedule.setStartsOn(new Date());
        schedule.setFrequency(1);
        schedule.setRecurrenceType(RecurrenceType.DAILY);
        schedule.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
        schedule.setDaysOfWeek(new ArrayList<>());
        schedule.setDisabled(false);
        pm.setSchedule(schedule);
        return pm;
    }

    @Nested
    class CreateTests {

        private PreventiveMaintenancePostDTO buildPostDTO() {
            PreventiveMaintenancePostDTO dto = new PreventiveMaintenancePostDTO();
            dto.setName("New PM");
            dto.setTitle("New PM Title");
            dto.setFrequency(1);
            dto.setRecurrenceType(RecurrenceType.DAILY);
            dto.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
            dto.setCustomFields(new ArrayList<>());
            dto.setDaysOfWeek(new ArrayList<>());
            dto.setAssignedTo(new ArrayList<>());
            dto.setCustomers(new ArrayList<>());
            dto.setFiles(new ArrayList<>());
            return dto;
        }

        @Test
        void create_success() {
            PreventiveMaintenancePostDTO dto = buildPostDTO();
            PreventiveMaintenance mappedPM = buildPM(null);
            when(preventiveMaintenanceMapper.toModel(dto)).thenReturn(mappedPM);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PM_SCHEDULES)).thenReturn(true);
            when(customSequenceService.getNextPreventiveMaintenanceSequence(company)).thenReturn(1L);
            when(preventiveMaintenanceRepository.saveAndFlush(any())).thenAnswer(inv -> {
                PreventiveMaintenance saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(scheduleService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PreventiveMaintenance result = preventiveMaintenanceService.create(dto, user);

            assertNotNull(result);
            assertEquals("PM000001", result.getCustomId());
            verify(preventiveMaintenanceRepository).saveAndFlush(any());
            verify(scheduleService).scheduleWorkOrder(any());
        }

        @Test
        void create_noCreatePermission_throwsForbidden() {
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .createPermissions(new HashSet<>())
                    .viewPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            PreventiveMaintenancePostDTO dto = buildPostDTO();
            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.create(dto, restrictedUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_planLacksFeature_throwsForbidden() {
            SubscriptionPlan basicPlan = SubscriptionPlan.builder()
                    .id(2L)
                    .name("Basic")
                    .features(new HashSet<>())
                    .build();
            Subscription basicSub = Subscription.builder()
                    .id(2L)
                    .subscriptionPlan(basicPlan)
                    .build();
            company.setSubscription(basicSub);

            PreventiveMaintenancePostDTO dto = buildPostDTO();
            PreventiveMaintenance mappedPM = buildPM(null);
            when(preventiveMaintenanceMapper.toModel(dto)).thenReturn(mappedPM);

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.create(dto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_usageLimitReached_throwsForbidden() {
            PreventiveMaintenancePostDTO dto = buildPostDTO();
            PreventiveMaintenance mappedPM = buildPM(null);
            when(preventiveMaintenanceMapper.toModel(dto)).thenReturn(mappedPM);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PM_SCHEDULES)).thenReturn(false);
            when(preventiveMaintenanceRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_PM_SCHEDULES) - 1))))
                    .thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.create(dto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void create_usageLimitWithEntitlement_noException() {
            PreventiveMaintenancePostDTO dto = buildPostDTO();
            PreventiveMaintenance mappedPM = buildPM(null);
            when(preventiveMaintenanceMapper.toModel(dto)).thenReturn(mappedPM);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PM_SCHEDULES)).thenReturn(true);
            when(customSequenceService.getNextPreventiveMaintenanceSequence(company)).thenReturn(1L);
            when(preventiveMaintenanceRepository.saveAndFlush(any())).thenAnswer(inv -> {
                PreventiveMaintenance saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(scheduleService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> preventiveMaintenanceService.create(dto, user));
            verify(preventiveMaintenanceRepository, never()).hasMoreThan(anyLong(), anyLong());
        }

        @Test
        void create_setsCustomId() {
            PreventiveMaintenancePostDTO dto = buildPostDTO();
            PreventiveMaintenance mappedPM = buildPM(null);
            when(preventiveMaintenanceMapper.toModel(dto)).thenReturn(mappedPM);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PM_SCHEDULES)).thenReturn(true);
            when(customSequenceService.getNextPreventiveMaintenanceSequence(company)).thenReturn(42L);
            when(preventiveMaintenanceRepository.saveAndFlush(any())).thenAnswer(inv -> {
                PreventiveMaintenance saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(scheduleService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PreventiveMaintenance result = preventiveMaintenanceService.create(dto, user);

            assertEquals("PM000042", result.getCustomId());
        }

        @Test
        void create_savesScheduleWithPostDTOFields() {
            PreventiveMaintenancePostDTO dto = buildPostDTO();
            dto.setStartsOn(new Date(1000));
            dto.setFrequency(7);
            dto.setDueDateDelay(3);
            dto.setRecurrenceType(RecurrenceType.WEEKLY);
            dto.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            dto.setDaysOfWeek(List.of(0, 1, 2));

            PreventiveMaintenance mappedPM = buildPM(null);
            when(preventiveMaintenanceMapper.toModel(dto)).thenReturn(mappedPM);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PM_SCHEDULES)).thenReturn(true);
            when(customSequenceService.getNextPreventiveMaintenanceSequence(company)).thenReturn(1L);
            when(preventiveMaintenanceRepository.saveAndFlush(any())).thenAnswer(inv -> {
                PreventiveMaintenance saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(scheduleService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            preventiveMaintenanceService.create(dto, user);

            verify(scheduleService).save(argThat(schedule ->
                    schedule.getFrequency() == 7
                            && schedule.getDueDateDelay() == 3
                            && schedule.getRecurrenceType() == RecurrenceType.WEEKLY
                            && schedule.getRecurrenceBasedOn() == RecurrenceBasedOn.COMPLETED_DATE
                            && schedule.getDaysOfWeek().equals(List.of(0, 1, 2))
            ));
        }
    }

    @Nested
    class PatchTests {

        @Test
        void patch_success() {
            PreventiveMaintenance pm = buildPM(1L);
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();
            patchDTO.setName("Updated PM");
            patchDTO.setTitle("Updated Title");
            patchDTO.setCustomFields(new ArrayList<>());
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));
            when(preventiveMaintenanceMapper.updatePreventiveMaintenance(eq(pm), eq(patchDTO))).thenReturn(pm);
            when(preventiveMaintenanceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            PreventiveMaintenance result = preventiveMaintenanceService.patch(1L, patchDTO, user);

            assertNotNull(result);
            assertFalse(result.getSchedule().isDisabled());
            verify(preventiveMaintenanceRepository).saveAndFlush(pm);
        }

        @Test
        void patch_notFound_throwsNotFound() {
            when(preventiveMaintenanceRepository.findById(99L)).thenReturn(Optional.empty());
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.patch(99L, patchDTO, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void patch_noEditPermission_throwsForbidden() {
            PreventiveMaintenance pm = buildPM(1L);
            pm.setCreatedBy(99L);
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .editOtherPermissions(new HashSet<>())
                    .createPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.patch(1L, patchDTO, restrictedUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void patch_planLacksFeature_throwsForbidden() {
            PreventiveMaintenance pm = buildPM(1L);
            SubscriptionPlan basicPlan = SubscriptionPlan.builder()
                    .id(2L)
                    .name("Basic")
                    .features(new HashSet<>())
                    .build();
            Subscription basicSub = Subscription.builder()
                    .id(2L)
                    .subscriptionPlan(basicPlan)
                    .build();
            company.setSubscription(basicSub);

            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.patch(1L, patchDTO, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void patch_enablesScheduleAfterUpdate() {
            PreventiveMaintenance pm = buildPM(1L);
            pm.getSchedule().setDisabled(true);
            PreventiveMaintenancePatchDTO patchDTO = new PreventiveMaintenancePatchDTO();
            patchDTO.setName("Updated");
            patchDTO.setCustomFields(new ArrayList<>());
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));
            when(preventiveMaintenanceMapper.updatePreventiveMaintenance(eq(pm), eq(patchDTO))).thenReturn(pm);
            when(preventiveMaintenanceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            preventiveMaintenanceService.patch(1L, patchDTO, user);

            assertFalse(pm.getSchedule().isDisabled());
        }
    }

    @Nested
    class GetByIdTests {

        @Test
        void getById_found() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));

            PreventiveMaintenance result = preventiveMaintenanceService.getById(1L, user);

            assertEquals(1L, result.getId());
        }

        @Test
        void getById_notFound_throwsNotFound() {
            when(preventiveMaintenanceRepository.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.getById(99L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void getById_accessDenied_throwsForbidden() {
            PreventiveMaintenance pm = buildPM(1L);
            pm.setCreatedBy(99L);
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .viewPermissions(new HashSet<>())
                    .viewOtherPermissions(new HashSet<>())
                    .createPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.getById(1L, restrictedUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteById_success() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));

            preventiveMaintenanceService.deleteByIdAndUser(1L, user);

            verify(scheduleService).stopScheduleJobs(100L);
            verify(preventiveMaintenanceRepository).deleteById(1L);
        }

        @Test
        void deleteById_notFound_throwsNotFound() {
            when(preventiveMaintenanceRepository.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.deleteByIdAndUser(99L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteById_noDeletePermission_throwsForbidden() {
            PreventiveMaintenance pm = buildPM(1L);
            pm.setCreatedBy(99L);
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .deleteOtherPermissions(new HashSet<>())
                    .createPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.deleteByIdAndUser(1L, restrictedUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(scheduleService, never()).stopScheduleJobs(anyLong());
        }
    }

    @Nested
    class TriggerWorkOrderTests {

        @Test
        void triggerWorkOrder_success() {
            PreventiveMaintenance pm = buildPM(1L);
            WorkOrder wo = new WorkOrder();
            wo.setId(10L);
            Role triggerRole = Role.builder()
                    .id(1L)
                    .name("Admin")
                    .roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.ADMIN)
                    .createPermissions(new HashSet<>(Arrays.asList(
                            PermissionEntity.WORK_ORDERS,
                            PermissionEntity.PREVENTIVE_MAINTENANCES)))
                    .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                    .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PREVENTIVE_MAINTENANCES)))
                    .build();
            pm.setCreatedBy(1L);
            user.setRole(triggerRole);
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));
            when(workOrderService.getWorkOrderFromWorkOrderBase(pm)).thenReturn(new WorkOrderPostDTO());
            when(workOrderService.create(any(), eq(company))).thenReturn(wo);
            when(taskService.findByPreventiveMaintenance(1L)).thenReturn(Collections.emptyList());

            WorkOrder result = preventiveMaintenanceService.triggerWorkOrder(1L, user);

            assertNotNull(result);
            verify(workOrderService).create(any(), eq(company));
        }

        @Test
        void triggerWorkOrder_noCreatePermission_throwsForbidden() {
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .createPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.triggerWorkOrder(1L, restrictedUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void triggerWorkOrder_notFound_throwsNotFound() {
            role.setCreatePermissions(new HashSet<>(Arrays.asList(
                    PermissionEntity.WORK_ORDERS,
                    PermissionEntity.PREVENTIVE_MAINTENANCES)));
            when(preventiveMaintenanceRepository.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.triggerWorkOrder(99L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void triggerWorkOrder_accessDenied_throwsForbidden() {
            PreventiveMaintenance pm = buildPM(1L);
            pm.setCreatedBy(99L);
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                    .viewPermissions(new HashSet<>())
                    .viewOtherPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.triggerWorkOrder(1L, restrictedUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class GetRecentWorkOrdersTests {

        @Test
        void getRecentWorkOrders_success() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(pm));
            WorkOrder wo1 = new WorkOrder();
            wo1.setId(10L);
            WorkOrder wo2 = new WorkOrder();
            wo2.setId(11L);
            Page<WorkOrder> page = new PageImpl<>(List.of(wo1, wo2));
            when(workOrderService.findLastByPM(1L, 10)).thenReturn(page);

            List<WorkOrder> result = preventiveMaintenanceService.getRecentWorkOrders(1L, user);

            assertEquals(2, result.size());
        }

        @Test
        void getRecentWorkOrders_notFound_throwsNotFound() {
            when(preventiveMaintenanceRepository.findByIdAndCompany_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> preventiveMaintenanceService.getRecentWorkOrders(99L, user));
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_found() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findById(1L)).thenReturn(Optional.of(pm));

            Optional<PreventiveMaintenance> result = preventiveMaintenanceService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
        }

        @Test
        void findById_notFound() {
            when(preventiveMaintenanceRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<PreventiveMaintenance> result = preventiveMaintenanceService.findById(99L);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    class FindByCompanyTests {

        @Test
        void findByCompany_delegatesToRepository() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(pm));

            Collection<PreventiveMaintenance> result = preventiveMaintenanceService.findByCompany(1L);

            assertEquals(1, result.size());
            verify(preventiveMaintenanceRepository).findByCompany_Id(1L);
        }

        @Test
        void findByCompanyForExport_delegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<PreventiveMaintenance> page = new PageImpl<>(Collections.emptyList());
            when(preventiveMaintenanceRepository.findByCompanyForExport(1L, pageable)).thenReturn(page);

            Page<PreventiveMaintenance> result = preventiveMaintenanceService.findByCompanyForExport(1L, pageable);

            assertNotNull(result);
            verify(preventiveMaintenanceRepository).findByCompanyForExport(1L, pageable);
        }
    }

    @Nested
    class GetAllTests {

        @Test
        void getAll_delegatesToRepository() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findAll()).thenReturn(Collections.singletonList(pm));

            Collection<PreventiveMaintenance> result = preventiveMaintenanceService.getAll();

            assertEquals(1, result.size());
            verify(preventiveMaintenanceRepository).findAll();
        }
    }

    @Nested
    class FindByIdsAndCompanyTests {

        @Test
        void findByIdsAndCompany_delegatesToRepository() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.findByIdInAndCompany_Id(List.of(1L, 2L), 1L))
                    .thenReturn(Collections.singletonList(pm));

            List<PreventiveMaintenance> result = preventiveMaintenanceService.findByIdsAndCompany(List.of(1L, 2L), 1L);

            assertEquals(1, result.size());
        }
    }

    @Nested
    class SaveAllTests {

        @Test
        void saveAll_delegatesToRepository() {
            PreventiveMaintenance pm = buildPM(1L);
            when(preventiveMaintenanceRepository.saveAll(anyList())).thenReturn(Collections.singletonList(pm));

            List<PreventiveMaintenance> result = preventiveMaintenanceService.saveAll(Collections.singletonList(pm));

            assertEquals(1, result.size());
            verify(preventiveMaintenanceRepository).saveAll(anyList());
        }
    }

    @Nested
    class SearchCriteriaTests {

        @Test
        void getSearchCriteria_roleClient_filtersByCompany() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());

            SearchCriteria result = preventiveMaintenanceService.getSearchCriteria(user, criteria);

            assertFalse(result.getFilterFields().isEmpty());
        }

        @Test
        void getSearchCriteria_roleClient_noViewPermission_throwsForbidden() {
            Role restrictedRole = Role.builder()
                    .id(2L)
                    .name("Restricted")
                    .roleType(RoleType.ROLE_CLIENT)
                    .viewPermissions(new HashSet<>())
                    .build();
            User restrictedUser = new User();
            restrictedUser.setId(2L);
            restrictedUser.setRole(restrictedRole);
            restrictedUser.setCompany(company);
            restrictedUser.setEnabled(true);
            restrictedUser.setSuperAccountRelations(new ArrayList<>());
            restrictedUser.setUserSettings(new UserSettings());

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());

            CustomException ex = assertThrows(CustomException.class,
                    () -> preventiveMaintenanceService.getSearchCriteria(restrictedUser, criteria));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getSearchCriteria_roleClient_withSuperAccountRelations_usesChildCompanies() {
            User childUser = new User();
            childUser.setId(3L);
            Company childCompany = new Company("ChildCo", 5, subscription);
            childCompany.setId(2L);
            childUser.setCompany(childCompany);

            SuperAccountRelation relation = new SuperAccountRelation();
            relation.setChildUser(childUser);
            user.setSuperAccountRelations(new ArrayList<>(Collections.singletonList(relation)));

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());

            SearchCriteria result = preventiveMaintenanceService.getSearchCriteria(user, criteria);

            assertFalse(result.getFilterFields().isEmpty());
        }
    }

    @Nested
    class GetSearchCriteriaEntityGraphTests {

        @Test
        void findBySearchCriteriaWithEntityGraph_delegatesToRepository() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(org.springframework.data.domain.Sort.Direction.ASC);
            when(preventiveMaintenanceRepository.<PreventiveMaintenance>findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<PreventiveMaintenance> result =
                    preventiveMaintenanceService.findBySearchCriteriaWithEntityGraph(criteria);

            assertNotNull(result);
            verify(preventiveMaintenanceRepository).<PreventiveMaintenance>findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    class CreateWorkOrderFromPreventiveMaintenanceTests {

        @Test
        void createWorkOrderFromPM_setsParentAndCopiesTasks() {
            PreventiveMaintenance pm = buildPM(1L);
            WorkOrderPostDTO woPostDTO = new WorkOrderPostDTO();
            WorkOrder savedWO = new WorkOrder();
            savedWO.setId(20L);
            when(workOrderService.getWorkOrderFromWorkOrderBase(pm)).thenReturn(woPostDTO);
            when(workOrderService.create(any(), eq(company))).thenReturn(savedWO);

            Task task = new Task();
            task.setId(1L);
            when(taskService.findByPreventiveMaintenance(1L)).thenReturn(Collections.singletonList(task));

            WorkOrder result = preventiveMaintenanceService.createWorkOrderFromPreventiveMaintenance(pm);

            assertEquals(20L, result.getId());
            verify(taskService).create(any(Task.class));
        }

        @Test
        void createWorkOrderFromPM_withDueDateDelay_setsDueDate() {
            PreventiveMaintenance pm = buildPM(1L);
            pm.getSchedule().setDueDateDelay(5);
            WorkOrderPostDTO woPostDTO = new WorkOrderPostDTO();
            WorkOrder savedWO = new WorkOrder();
            savedWO.setId(20L);
            when(workOrderService.getWorkOrderFromWorkOrderBase(pm)).thenReturn(woPostDTO);
            when(workOrderService.create(any(), eq(company))).thenReturn(savedWO);
            when(taskService.findByPreventiveMaintenance(1L)).thenReturn(Collections.emptyList());

            WorkOrder result = preventiveMaintenanceService.createWorkOrderFromPreventiveMaintenance(pm);

            assertNotNull(result);
            verify(workOrderService).create(any(), eq(company));
        }
    }
}
