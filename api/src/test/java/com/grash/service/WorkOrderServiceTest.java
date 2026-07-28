package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.CalendarEvent;
import com.grash.dto.DateRange;
import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.dto.WorkOrderBaseMiniDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPatchDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.imports.WorkOrderImportDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.WorkOrderRepository;
import com.grash.utils.Consts;
import com.grash.utils.Utils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.grash.dto.ReportConfig;
import com.grash.dto.workOrder.WorkOrderSendReportDTO;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @InjectMocks
    private WorkOrderService workOrderService;

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private AssetService assetService;
    @Mock
    private LaborService laborService;
    @Mock
    private AdditionalCostService additionalCostService;
    @Mock
    private PartQuantityService partQuantityService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private EntityManager em;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private MailService mailService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private CustomSequenceService customSequenceService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private UserService userService;
    @Mock
    private CompanyService companyService;
    @Mock
    private WebhookDispatchService webhookDispatchService;
    @Mock
    private CustomFieldValueService customFieldValueService;
    @Mock
    private ReviewEligibilityService reviewEligibilityService;
    @Mock
    private IntercomService intercomService;
    @Mock
    private TaskService taskService;
    @Mock
    private RelationService relationService;
    @Mock
    private CommentService commentService;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private PreventiveMaintenanceService preventiveMaintenanceService;
    @Mock
    private PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    @Mock
    private CustomerService customerService;
    @Mock
    private LocationService locationService;
    @Mock
    private WorkOrderCategoryService workOrderCategoryService;

    private Company company;
    private User user;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;
    private CompanySettings companySettings;
    private GeneralPreferences generalPreferences;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workOrderService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(workOrderService, "laborService", laborService);
        ReflectionTestUtils.setField(workOrderService, "webhookDispatchService", webhookDispatchService);
        ReflectionTestUtils.setField(workOrderService, "workflowService", workflowService);
        ReflectionTestUtils.setField(workOrderService, "additionalCostService", additionalCostService);
        ReflectionTestUtils.setField(workOrderService, "partQuantityService", partQuantityService);
        ReflectionTestUtils.setField(workOrderService, "taskService", taskService);
        ReflectionTestUtils.setField(workOrderService, "relationService", relationService);
        ReflectionTestUtils.setField(workOrderService, "commentService", commentService);
        ReflectionTestUtils.setField(workOrderService, "scheduleService", scheduleService);
        ReflectionTestUtils.setField(workOrderService, "preventiveMaintenanceService", preventiveMaintenanceService);
        ReflectionTestUtils.setField(workOrderService, "preventiveMaintenanceMapper", preventiveMaintenanceMapper);
        ReflectionTestUtils.setField(workOrderService, "customerService", customerService);
        ReflectionTestUtils.setField(workOrderService, "locationService", locationService);
        ReflectionTestUtils.setField(workOrderService, "workOrderCategoryService", workOrderCategoryService);

        subscriptionPlan = SubscriptionPlan.builder()
                .id(1L)
                .name("Pro")
                .features(new HashSet<>(Arrays.asList(PlanFeatures.SIGNATURE, PlanFeatures.WEBHOOK)))
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

        role = Role.builder()
                .id(1L)
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
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

    // ─── Helper methods ───────────────────────────────────────────────

    private WorkOrder buildWorkOrder(Long id) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setTitle("Test WO");
        wo.setDescription("desc");
        wo.setPriority(Priority.NONE);
        wo.setEstimatedDuration(1.0);
        wo.setStatus(Status.OPEN);
        wo.setCompany(company);
        wo.setCreatedBy(user.getId());
        wo.setAssignedTo(new ArrayList<>());
        wo.setCustomers(new ArrayList<>());
        wo.setFiles(new ArrayList<>());
        return wo;
    }

    private Asset buildAsset(Long id) {
        Asset a = new Asset();
        a.setId(id);
        a.setName("Asset");
        a.setCompany(company);
        return a;
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
        u.setUserSettings(new UserSettings());
        return u;
    }

    private WorkOrderCategory buildCategory(Long id, String name) {
        WorkOrderCategory cat = new WorkOrderCategory();
        cat.setId(id);
        cat.setName(name);
        return cat;
    }

    private Location buildLocation(Long id) {
        Location loc = new Location();
        loc.setId(id);
        loc.setName("Loc");
        loc.setCompany(company);
        return loc;
    }

    private Team buildTeam(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Team");
        team.setCompany(company);
        team.setUsers(new ArrayList<>());
        return team;
    }

    private Customer buildCustomer(Long id) {
        Customer c = new Customer();
        c.setId(id);
        c.setName("Cust" + id);
        c.setCompany(company);
        c.setEmail("cust" + id + "@test.com");
        return c;
    }

    private Collection<WOField> detectChangedFields(WorkOrder original, WorkOrder updated) {
        return workOrderService.detectChangedFieldsFromEntity(original, updated);
    }

    private Collection<WOField> detectPatchDTO(WorkOrder original, WorkOrderPatchDTO patch) {
        return workOrderService.detectPatchDTOChangedFields(original, patch);
    }

    // ═══════════════════════════════════════════════════════════════════
    // detectPatchDTOChangedFields
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class DetectPatchDTOChangedFields {

        private WorkOrder original;

        @BeforeEach
        void initOriginal() {
            original = buildWorkOrder(1L);
            original.setAsset(buildAsset(10L));
            original.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            original.setCategory(buildCategory(30L, "Cat"));
            original.setDescription("original desc");
            original.setDueDate(new Date(1000));
            original.setEstimatedDuration(2.0);
            original.setLocation(buildLocation(40L));
            original.setPriority(Priority.LOW);
            original.setTitle("original title");
            original.setTeam(buildTeam(50L));
            original.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
        }

        @Test
        void assetChanged() {
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setAsset(buildAsset(99L));
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            patch.setCategory(buildCategory(30L, "Cat"));
            patch.setDescription("original desc");
            patch.setDueDate(new Date(1000));
            patch.setEstimatedDuration(2.0);
            patch.setLocation(buildLocation(40L));
            patch.setPriority(Priority.LOW);
            patch.setTitle("original title");
            patch.setTeam(buildTeam(50L));
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSET));
        }

        @Test
        void assetUnchanged() {
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setAsset(buildAsset(10L));
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            patch.setCategory(buildCategory(30L, "Cat"));
            patch.setDescription("original desc");
            patch.setDueDate(new Date(1000));
            patch.setEstimatedDuration(2.0);
            patch.setLocation(buildLocation(40L));
            patch.setPriority(Priority.LOW);
            patch.setTitle("original title");
            patch.setTeam(buildTeam(50L));
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSET));
        }

        @Test
        void assetNullOnPatch() {
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setAsset(null);
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            patch.setCategory(buildCategory(30L, "Cat"));
            patch.setDescription("original desc");
            patch.setDueDate(new Date(1000));
            patch.setEstimatedDuration(2.0);
            patch.setLocation(buildLocation(40L));
            patch.setPriority(Priority.LOW);
            patch.setTitle("original title");
            patch.setTeam(buildTeam(50L));
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSET));
        }

        @Test
        void assetNullOnBothSides() {
            original.setAsset(null);
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setAsset(null);
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            patch.setCategory(buildCategory(30L, "Cat"));
            patch.setDescription("original desc");
            patch.setDueDate(new Date(1000));
            patch.setEstimatedDuration(2.0);
            patch.setLocation(buildLocation(40L));
            patch.setPriority(Priority.LOW);
            patch.setTitle("original title");
            patch.setTeam(buildTeam(50L));
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSET));
        }

        @Test
        void assigneesChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L), buildUser(21L))));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesNullOnPatchNullOnOriginal() {
            original.setAssignedTo(null);
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(null);
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesNullOnOneSide() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(null);
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void categoryChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCategory(buildCategory(99L, "New"));
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.CATEGORY));
        }

        @Test
        void categoryUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCategory(buildCategory(30L, "Cat"));
            assertFalse(detectPatchDTO(original, patch).contains(WOField.CATEGORY));
        }

        @Test
        void categoryNullOnOneSide() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCategory(null);
            assertTrue(detectPatchDTO(original, patch).contains(WOField.CATEGORY));
        }

        @Test
        void descriptionChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDescription("new desc");
            assertTrue(detectPatchDTO(original, patch).contains(WOField.DESCRIPTION));
        }

        @Test
        void descriptionUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDescription("original desc");
            assertFalse(detectPatchDTO(original, patch).contains(WOField.DESCRIPTION));
        }

        @Test
        void descriptionNullOnOneSide() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDescription(null);
            assertTrue(detectPatchDTO(original, patch).contains(WOField.DESCRIPTION));
        }

        @Test
        void dueDateChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDueDate(new Date(2000));
            assertTrue(detectPatchDTO(original, patch).contains(WOField.DUE_DATE));
        }

        @Test
        void dueDateUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDueDate(new Date(1000));
            assertFalse(detectPatchDTO(original, patch).contains(WOField.DUE_DATE));
        }

        @Test
        void estimatedDurationChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setEstimatedDuration(5.0);
            assertTrue(detectPatchDTO(original, patch).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void estimatedDurationUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setEstimatedDuration(2.0);
            assertFalse(detectPatchDTO(original, patch).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void locationChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setLocation(buildLocation(99L));
            assertTrue(detectPatchDTO(original, patch).contains(WOField.LOCATION));
        }

        @Test
        void locationUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setLocation(buildLocation(40L));
            assertFalse(detectPatchDTO(original, patch).contains(WOField.LOCATION));
        }

        @Test
        void locationNullOnBothSides() {
            original.setLocation(null);
            WorkOrderPatchDTO patch = basePatch();
            patch.setLocation(null);
            assertFalse(detectPatchDTO(original, patch).contains(WOField.LOCATION));
        }

        @Test
        void priorityChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setPriority(Priority.HIGH);
            assertTrue(detectPatchDTO(original, patch).contains(WOField.PRIORITY));
        }

        @Test
        void priorityUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setPriority(Priority.LOW);
            assertFalse(detectPatchDTO(original, patch).contains(WOField.PRIORITY));
        }

        @Test
        void titleChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTitle("new title");
            assertTrue(detectPatchDTO(original, patch).contains(WOField.TITLE));
        }

        @Test
        void titleUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTitle("original title");
            assertFalse(detectPatchDTO(original, patch).contains(WOField.TITLE));
        }

        @Test
        void teamChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTeam(buildTeam(99L));
            assertTrue(detectPatchDTO(original, patch).contains(WOField.TEAM));
        }

        @Test
        void teamUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTeam(buildTeam(50L));
            assertFalse(detectPatchDTO(original, patch).contains(WOField.TEAM));
        }

        @Test
        void teamNullOnBothSides() {
            original.setTeam(null);
            WorkOrderPatchDTO patch = basePatch();
            patch.setTeam(null);
            assertFalse(detectPatchDTO(original, patch).contains(WOField.TEAM));
        }

        @Test
        void customersChanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L), buildCustomer(61L))));
            assertTrue(detectPatchDTO(original, patch).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersUnchanged() {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            assertFalse(detectPatchDTO(original, patch).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersDifferentOrderSameElements() {
            Customer c1 = buildCustomer(60L);
            Customer c2 = buildCustomer(61L);
            original.setCustomers(new ArrayList<>(List.of(c1, c2)));
            WorkOrderPatchDTO patch = basePatch();
            patch.setCustomers(new ArrayList<>(List.of(c2, c1)));
            assertFalse(detectPatchDTO(original, patch).contains(WOField.CUSTOMERS));
        }

        @Test
        void multipleFieldsChanged() {
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setTitle("new title");
            patch.setDescription("new desc");
            patch.setPriority(Priority.HIGH);
            patch.setAsset(null);
            patch.setCategory(null);
            patch.setDueDate(null);
            patch.setEstimatedDuration(0);
            patch.setLocation(null);
            patch.setTeam(null);
            patch.setAssignedTo(new ArrayList<>());
            patch.setCustomers(new ArrayList<>());
            Collection<WOField> result = detectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.TITLE));
            assertTrue(result.contains(WOField.DESCRIPTION));
            assertTrue(result.contains(WOField.PRIORITY));
            assertTrue(result.contains(WOField.ASSET));
            assertTrue(result.contains(WOField.CATEGORY));
            assertTrue(result.contains(WOField.DUE_DATE));
            assertTrue(result.contains(WOField.LOCATION));
            assertTrue(result.contains(WOField.TEAM));
            assertTrue(result.contains(WOField.CUSTOMERS));
        }

        private WorkOrderPatchDTO basePatch() {
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setAsset(buildAsset(10L));
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            patch.setCategory(buildCategory(30L, "Cat"));
            patch.setDescription("original desc");
            patch.setDueDate(new Date(1000));
            patch.setEstimatedDuration(2.0);
            patch.setLocation(buildLocation(40L));
            patch.setPriority(Priority.LOW);
            patch.setTitle("original title");
            patch.setTeam(buildTeam(50L));
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            return patch;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // detectChangedFieldsFromEntity
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class DetectChangedFieldsFromEntity {

        @Test
        void assetChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(buildAsset(10L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(buildAsset(99L));
            assertTrue(detectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assetUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(buildAsset(10L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(buildAsset(10L));
            assertFalse(detectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assetNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(buildAsset(10L));
            assertTrue(detectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assetNullOnBothSides() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(null);
            assertFalse(detectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assigneesChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAssignedTo(new ArrayList<>(List.of(buildUser(20L), buildUser(21L))));
            assertTrue(detectChangedFields(original, updated).contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            assertFalse(detectChangedFields(original, updated).contains(WOField.ASSIGNEES));
        }

        @Test
        void categoryChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCategory(buildCategory(30L, "A"));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCategory(buildCategory(31L, "B"));
            assertTrue(detectChangedFields(original, updated).contains(WOField.CATEGORY));
        }

        @Test
        void categoryNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCategory(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCategory(buildCategory(30L, "A"));
            assertTrue(detectChangedFields(original, updated).contains(WOField.CATEGORY));
        }

        @Test
        void descriptionChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setDescription("old");
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDescription("new");
            assertTrue(detectChangedFields(original, updated).contains(WOField.DESCRIPTION));
        }

        @Test
        void dueDateChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setDueDate(new Date(1000));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDueDate(new Date(2000));
            assertTrue(detectChangedFields(original, updated).contains(WOField.DUE_DATE));
        }

        @Test
        void estimatedDurationChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setEstimatedDuration(1.0);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setEstimatedDuration(2.0);
            assertTrue(detectChangedFields(original, updated).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void locationChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setLocation(buildLocation(40L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setLocation(buildLocation(41L));
            assertTrue(detectChangedFields(original, updated).contains(WOField.LOCATION));
        }

        @Test
        void priorityChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setPriority(Priority.LOW);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setPriority(Priority.HIGH);
            assertTrue(detectChangedFields(original, updated).contains(WOField.PRIORITY));
        }

        @Test
        void titleChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTitle("old");
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTitle("new");
            assertTrue(detectChangedFields(original, updated).contains(WOField.TITLE));
        }

        @Test
        void teamChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTeam(buildTeam(50L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTeam(buildTeam(51L));
            assertTrue(detectChangedFields(original, updated).contains(WOField.TEAM));
        }

        @Test
        void statusChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setStatus(Status.OPEN);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setStatus(Status.COMPLETE);
            assertTrue(detectChangedFields(original, updated).contains(WOField.STATUS));
        }

        @Test
        void statusUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setStatus(Status.OPEN);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setStatus(Status.OPEN);
            assertFalse(detectChangedFields(original, updated).contains(WOField.STATUS));
        }

        @Test
        void customersChanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(new ArrayList<>(List.of(buildCustomer(61L))));
            assertTrue(detectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(new ArrayList<>());
            assertTrue(detectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }

        @Test
        void categoryUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCategory(buildCategory(30L, "Cat"));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCategory(buildCategory(30L, "Cat"));
            assertFalse(detectChangedFields(original, updated).contains(WOField.CATEGORY));
        }

        @Test
        void categoryNullOnBothSides() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCategory(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCategory(null);
            assertFalse(detectChangedFields(original, updated).contains(WOField.CATEGORY));
        }

        @Test
        void descriptionUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setDescription("same");
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDescription("same");
            assertFalse(detectChangedFields(original, updated).contains(WOField.DESCRIPTION));
        }

        @Test
        void descriptionNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setDescription(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDescription("new");
            assertTrue(detectChangedFields(original, updated).contains(WOField.DESCRIPTION));
        }

        @Test
        void dueDateUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setDueDate(new Date(1000));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDueDate(new Date(1000));
            assertFalse(detectChangedFields(original, updated).contains(WOField.DUE_DATE));
        }

        @Test
        void dueDateNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setDueDate(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDueDate(new Date(1000));
            assertTrue(detectChangedFields(original, updated).contains(WOField.DUE_DATE));
        }

        @Test
        void estimatedDurationUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setEstimatedDuration(2.0);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setEstimatedDuration(2.0);
            assertFalse(detectChangedFields(original, updated).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void estimatedDurationNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setEstimatedDuration(0);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setEstimatedDuration(2.0);
            assertTrue(detectChangedFields(original, updated).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void locationUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setLocation(buildLocation(40L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setLocation(buildLocation(40L));
            assertFalse(detectChangedFields(original, updated).contains(WOField.LOCATION));
        }

        @Test
        void locationNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setLocation(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setLocation(buildLocation(40L));
            assertTrue(detectChangedFields(original, updated).contains(WOField.LOCATION));
        }

        @Test
        void locationNullOnBothSides() {
            WorkOrder original = buildWorkOrder(1L);
            original.setLocation(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setLocation(null);
            assertFalse(detectChangedFields(original, updated).contains(WOField.LOCATION));
        }

        @Test
        void priorityUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setPriority(Priority.LOW);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setPriority(Priority.LOW);
            assertFalse(detectChangedFields(original, updated).contains(WOField.PRIORITY));
        }

        @Test
        void titleUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTitle("same");
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTitle("same");
            assertFalse(detectChangedFields(original, updated).contains(WOField.TITLE));
        }

        @Test
        void titleNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTitle(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTitle("new");
            assertTrue(detectChangedFields(original, updated).contains(WOField.TITLE));
        }

        @Test
        void teamUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTeam(buildTeam(50L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTeam(buildTeam(50L));
            assertFalse(detectChangedFields(original, updated).contains(WOField.TEAM));
        }

        @Test
        void teamNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTeam(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTeam(buildTeam(50L));
            assertTrue(detectChangedFields(original, updated).contains(WOField.TEAM));
        }

        @Test
        void teamNullOnBothSides() {
            WorkOrder original = buildWorkOrder(1L);
            original.setTeam(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTeam(null);
            assertFalse(detectChangedFields(original, updated).contains(WOField.TEAM));
        }

        @Test
        void customersUnchanged() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            assertFalse(detectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersNullOnBothSides() {
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(null);
            assertFalse(detectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersDifferentOrderSameElements() {
            Customer c1 = buildCustomer(60L);
            Customer c2 = buildCustomer(61L);
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(new ArrayList<>(List.of(c1, c2)));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(new ArrayList<>(List.of(c2, c1)));
            assertFalse(detectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }

        @Test
        void assigneesNullOnOneSide() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAssignedTo(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            assertTrue(detectChangedFields(original, updated).contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesNullOnBothSides() {
            WorkOrder original = buildWorkOrder(1L);
            original.setAssignedTo(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAssignedTo(null);
            assertFalse(detectChangedFields(original, updated).contains(WOField.ASSIGNEES));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // collectionsMatch
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CollectionsMatch {

        private final java.util.function.Function<User, Long> idExtractor = User::getId;

        @Test
        void bothNull() {
            assertTrue(workOrderService.collectionsMatch(null, null, idExtractor));
        }

        @Test
        void firstNullSecondEmpty() {
            assertFalse(workOrderService.collectionsMatch(null, new ArrayList<>(), idExtractor));
        }

        @Test
        void firstEmptySecondNull() {
            assertFalse(workOrderService.collectionsMatch(new ArrayList<>(), null, idExtractor));
        }

        @Test
        void sameElementsDifferentOrder() {
            User u1 = buildUser(1L);
            User u2 = buildUser(2L);
            List<User> a = List.of(u1, u2);
            List<User> b = List.of(u2, u1);
            assertTrue(workOrderService.collectionsMatch(a, b, idExtractor));
        }

        @Test
        void differentSizes() {
            List<User> a = List.of(buildUser(1L));
            List<User> b = List.of(buildUser(1L), buildUser(2L));
            assertFalse(workOrderService.collectionsMatch(a, b, idExtractor));
        }

        @Test
        void sameSizeDifferentElements() {
            List<User> a = List.of(buildUser(1L));
            List<User> b = List.of(buildUser(2L));
            assertFalse(workOrderService.collectionsMatch(a, b, idExtractor));
        }

        @Test
        void singleElementMatch() {
            List<User> a = List.of(buildUser(1L));
            List<User> b = List.of(buildUser(1L));
            assertTrue(workOrderService.collectionsMatch(a, b, idExtractor));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // checkUsageBasedLimit
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CheckUsageBasedLimit {

        @Test
        void underLimit_noException() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(false);
            when(workOrderRepository.hasMoreActiveThan(eq(1L),
                    eq((long) (Consts.usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS) - 1)))).thenReturn(false);
            assertDoesNotThrow(() -> workOrderService.checkUsageBasedLimit(company));
        }

        @Test
        void atLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(false);
            when(workOrderRepository.hasMoreActiveThan(eq(1L),
                    eq((long) (Consts.usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS) - 1)))).thenReturn(true);
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.checkUsageBasedLimit(company));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void entitlementPresent_bypassesLimit() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            assertDoesNotThrow(() -> workOrderService.checkUsageBasedLimit(company));
            verify(workOrderRepository, never()).hasMoreActiveThan(anyLong(), anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // changeStatus
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class ChangeStatus {

        private WorkOrder wo;
        private WorkOrderChangeStatusDTO dto;

        @BeforeEach
        void init() {
            wo = buildWorkOrder(1L);
            wo.setAsset(buildAsset(10L));
            wo.setStatus(Status.OPEN);
            wo.setFirstTimeToReact(null);

            dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(Status.IN_PROGRESS);
        }

        private void stubBasic() {
            lenient().when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            lenient().when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            lenient().doNothing().when(em).refresh(any());
            lenient().when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());
            lenient().when(licenseService.hasEntitlement(LicenseEntitlement.SIGNATURE_CAPTURE)).thenReturn(true);
            lenient().when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
        }

        @Test
        void signatureWithoutLicense_throwsForbidden() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            dto.setSignature("sig");
            when(licenseService.hasEntitlement(LicenseEntitlement.SIGNATURE_CAPTURE)).thenReturn(false);
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.changeStatus(dto, 1L, user, "ios"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void signatureWithLicenseAndPlanFeature_proceeds() {
            stubBasic();
            dto.setSignature("base64sig");
            assertDoesNotThrow(() -> workOrderService.changeStatus(dto, 1L, user, "ios"));
        }

        @Test
        void signatureWithEntitlementButPlanLacksFeature_throwsForbidden() {
            stubBasic();
            dto.setSignature("base64sig");
            SubscriptionPlan planWithoutSignature = SubscriptionPlan.builder()
                    .id(2L)
                    .name("Basic")
                    .features(new HashSet<>(Collections.singletonList(PlanFeatures.WEBHOOK)))
                    .build();
            Subscription subscriptionWithoutSignature = Subscription.builder()
                    .id(2L)
                    .subscriptionPlan(planWithoutSignature)
                    .build();
            company.setSubscription(subscriptionWithoutSignature);
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.changeStatus(dto, 1L, user, "ios"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void transitionToComplete_setsCompletedByAndCompletedOn() {
            wo.setStatus(Status.OPEN);
            dto.setStatus(Status.COMPLETE);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(assetService).stopDownTime(anyLong(), any());
            when(workOrderRepository.findByAsset_Id(10L)).thenReturn(Collections.emptyList());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());

            workOrderService.changeStatus(dto, 1L, user, "ios");
            verify(assetService).stopDownTime(eq(10L), any());
        }

        @Test
        void transitionToComplete_skipsDownTimeIfOtherIncompleteWOSharesAsset() {
            wo.setStatus(Status.OPEN);
            dto.setStatus(Status.COMPLETE);
            WorkOrder otherWO = buildWorkOrder(2L);
            otherWO.setAsset(buildAsset(10L));
            otherWO.setStatus(Status.IN_PROGRESS);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(workOrderRepository.findByAsset_Id(10L)).thenReturn(List.of(wo, otherWO));
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());

            workOrderService.changeStatus(dto, 1L, user, "ios");
            verify(assetService, never()).stopDownTime(anyLong(), any());
        }

        @Test
        void laborAutoStop_skippedWhenInProgress() {
            dto.setStatus(Status.IN_PROGRESS);
            stubBasic();
            workOrderService.changeStatus(dto, 1L, user, "ios");
            verify(laborService, never()).stop(any());
        }

        @Test
        void requesterNotification_fallbackToContactEmailWhenRequesterIdNull() {
            wo.setStatus(Status.OPEN);
            dto.setStatus(Status.COMPLETE);
            Request req = new Request();
            req.setCreatedBy(null);
            req.setContact("fallback@test.com");
            wo.setParentRequest(req);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(assetService).stopDownTime(anyLong(), any());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            generalPreferences.setWoUpdateForRequesters(true);

            workOrderService.changeStatus(dto, 1L, user, "ios");
            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{"fallback@test.com"}),
                    any(), anyMap(), any(), any(), any());
        }

        @Test
        void reviewEligibilityIncrement_firesOnFirstCompleteFromIosPlatform() {
            wo.setStatus(Status.OPEN);
            dto.setStatus(Status.COMPLETE);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(assetService).stopDownTime(anyLong(), any());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(reviewEligibilityService.getOrCreate(any())).thenReturn(new UserAppStats());

            workOrderService.changeStatus(dto, 1L, user, "ios");
            verify(reviewEligibilityService).incrementWorkOrder(any());
        }

        @Test
        void reviewEligibilityIncrement_firesOnAndroidPlatform() {
            wo.setStatus(Status.OPEN);
            dto.setStatus(Status.COMPLETE);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(assetService).stopDownTime(anyLong(), any());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(reviewEligibilityService.getOrCreate(any())).thenReturn(new UserAppStats());

            workOrderService.changeStatus(dto, 1L, user, "android");
            verify(reviewEligibilityService).incrementWorkOrder(any());
        }

        @Test
        void reviewEligibility_notIncrementedOnWebPlatform() {
            wo.setStatus(Status.OPEN);
            dto.setStatus(Status.COMPLETE);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(assetService).stopDownTime(anyLong(), any());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());

            workOrderService.changeStatus(dto, 1L, user, "web");
            verify(reviewEligibilityService, never()).incrementWorkOrder(any());
        }

        @Test
        void reviewEligibility_notIncrementedOnSecondCompleteTransition() {
            wo.setStatus(Status.COMPLETE);
            wo.setCompletedBy(user);
            wo.setCompletedOn(new Date());
            dto.setStatus(Status.COMPLETE);
            stubBasic();
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            workOrderService.changeStatus(dto, 1L, user, "ios");
            verify(reviewEligibilityService, never()).incrementWorkOrder(any());
        }

        @Test
        void statusChange_firesWorkOrderStatusChangeWebhook() {
            WorkOrder originalSnapshot = buildWorkOrder(1L);
            originalSnapshot.setAsset(buildAsset(10L));
            originalSnapshot.setStatus(Status.OPEN);

            WorkOrder mutableCopy = buildWorkOrder(1L);
            mutableCopy.setAsset(buildAsset(10L));
            mutableCopy.setStatus(Status.OPEN);

            when(workOrderRepository.findById(1L))
                    .thenReturn(Optional.of(originalSnapshot))
                    .thenReturn(Optional.of(mutableCopy));
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            doNothing().when(em).refresh(any());
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());
            doNothing().when(assetService).stopDownTime(anyLong(), any());
            when(workOrderRepository.findByAsset_Id(10L)).thenReturn(Collections.emptyList());

            dto.setStatus(Status.COMPLETE);
            workOrderService.changeStatus(dto, 1L, user, "ios");

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company), eq(WebhookEvent.WORK_ORDER_STATUS_CHANGE),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void statusUnchanged_doesNotFireWorkOrderStatusChangeWebhook() {
            WorkOrder originalSnapshot = buildWorkOrder(1L);
            originalSnapshot.setAsset(buildAsset(10L));
            originalSnapshot.setStatus(Status.OPEN);

            WorkOrder mutableCopy = buildWorkOrder(1L);
            mutableCopy.setAsset(buildAsset(10L));
            mutableCopy.setStatus(Status.OPEN);

            when(workOrderRepository.findById(1L))
                    .thenReturn(Optional.of(originalSnapshot))
                    .thenReturn(Optional.of(mutableCopy));
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            doNothing().when(em).refresh(any());
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());

            dto.setStatus(Status.OPEN);
            workOrderService.changeStatus(dto, 1L, user, "ios");

            verify(webhookDispatchService, never()).dispatchWebhook(
                    eq(company), eq(WebhookEvent.WORK_ORDER_STATUS_CHANGE),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // getSearchCriteria
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class GetSearchCriteria {

        @Test
        void clientRoleWithSuperAccountRelations_addsCompanyInFilter() {
            User childUser1 = buildUser(10L);
            Company childCompany = new Company("ChildCo", 5, subscription);
            childCompany.setId(2L);
            childUser1.setCompany(childCompany);

            SuperAccountRelation rel = SuperAccountRelation.builder()
                    .superUser(user)
                    .childUser(childUser1)
                    .build();
            user.setSuperAccountRelations(new ArrayList<>(List.of(rel)));

            SearchCriteria criteria = new SearchCriteria();
            SearchCriteria result = workOrderService.getSearchCriteria(user, criteria);

            Optional<FilterField> companyFilter = result.getFilterFields().stream()
                    .filter(f -> "company".equals(f.getField()))
                    .findFirst();
            assertTrue(companyFilter.isPresent());
            assertEquals("inm", companyFilter.get().getOperation());
        }

        @Test
        void clientRoleWithoutSuperAccountRelations_filtersByCompany() {
            user.setSuperAccountRelations(new ArrayList<>());
            SearchCriteria criteria = new SearchCriteria();
            SearchCriteria result = workOrderService.getSearchCriteria(user, criteria);

            Optional<FilterField> companyFilter = result.getFilterFields().stream()
                    .filter(f -> "company".equals(f.getField()))
                    .findFirst();
            assertTrue(companyFilter.isPresent());
            assertEquals("eq", companyFilter.get().getOperation());
        }

        @Test
        void canViewOthersTrue_noCreatorFilter() {
            user.setSuperAccountRelations(new ArrayList<>());
            SearchCriteria criteria = new SearchCriteria();
            workOrderService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = criteria.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()));
            assertFalse(hasCreatedByFilter);
        }

        @Test
        void canViewOthersFalse_addsCreatorFilter() {
            user.setSuperAccountRelations(new ArrayList<>());
            role.getViewOtherPermissions().clear();
            when(teamService.findByUser(1L)).thenReturn(Collections.emptyList());
            SearchCriteria criteria = new SearchCriteria();
            workOrderService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = criteria.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()));
            assertTrue(hasCreatedByFilter);
        }

        @Test
        void requesterRole_addsParentRequestCreatedByFilter() {
            user.setSuperAccountRelations(new ArrayList<>());
            role.getViewPermissions().clear();
            role.setCode(RoleCode.REQUESTER);
            SearchCriteria criteria = new SearchCriteria();
            workOrderService.getSearchCriteria(user, criteria);

            boolean hasParentRequestFilter = criteria.getFilterFields().stream()
                    .anyMatch(f -> "parentRequest.createdBy".equals(f.getField()));
            assertTrue(hasParentRequestFilter);
        }

        @Test
        void assignedToUserFilter_alwaysStrippedAtTheEnd() {
            user.setSuperAccountRelations(new ArrayList<>());
            SearchCriteria criteria = new SearchCriteria();
            criteria.getFilterFields().add(FilterField.builder()
                    .field("assignedToUser")
                    .value(1L)
                    .operation("eq")
                    .values(new ArrayList<>())
                    .build());
            workOrderService.getSearchCriteria(user, criteria);

            boolean hasAssignedToUser = criteria.getFilterFields().stream()
                    .anyMatch(f -> "assignedToUser".equals(f.getField()));
            assertFalse(hasAssignedToUser);
        }

        @Test
        void canViewOthersTrueWithAssignedToUser_addsAssignedToAndStripsAssignedToUser() {
            user.setSuperAccountRelations(new ArrayList<>());
            when(teamService.findByUser(1L)).thenReturn(Collections.emptyList());
            SearchCriteria criteria = new SearchCriteria();
            criteria.getFilterFields().add(FilterField.builder()
                    .field("assignedToUser")
                    .value(1L)
                    .operation("eq")
                    .values(new ArrayList<>())
                    .build());
            workOrderService.getSearchCriteria(user, criteria);

            boolean hasAssignedToFilter = criteria.getFilterFields().stream()
                    .anyMatch(f -> "assignedTo".equals(f.getField()));
            assertTrue(hasAssignedToFilter);
            boolean hasAssignedToUserFilter = criteria.getFilterFields().stream()
                    .anyMatch(f -> "assignedToUser".equals(f.getField()));
            assertFalse(hasAssignedToUserFilter);
        }

        @Test
        void nonClientRole_returnsUnmodifiedCriteria() {
            role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            SearchCriteria criteria = new SearchCriteria();
            List<FilterField> originalFilters = new ArrayList<>(criteria.getFilterFields());
            SearchCriteria result = workOrderService.getSearchCriteria(user, criteria);
            assertEquals(originalFilters.size(), result.getFilterFields().size());
            assertTrue(result.getFilterFields().isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // patch – notification skip logic
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class PatchNotification {

        private WorkOrder wo;

        @BeforeEach
        void init() {
            wo = buildWorkOrder(1L);
            wo.setStatus(Status.OPEN);
        }

        private void stubPatch(WorkOrder woAfterPatch, boolean disableNotif, Status newStatus) {
            woAfterPatch.setStatus(newStatus);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.existsById(1L)).thenReturn(true);
            when(workOrderMapper.updateWorkOrder(any(), any())).thenReturn(woAfterPatch);
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(em).refresh(any());
            generalPreferences.setDisableClosedWorkOrdersNotif(disableNotif);
            lenient().when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
        }

        @Test
        void notificationSkipped_onlyWhenDisableNotifTrueAndStatusComplete() {
            WorkOrder woAfter = buildWorkOrder(1L);
            stubPatch(woAfter, true, Status.COMPLETE);
            woAfter.setAssignedTo(new ArrayList<>());
            woAfter.setCustomers(new ArrayList<>());

            workOrderService.patch(1L, new WorkOrderPatchDTO(), user);
            verify(notificationService, never()).createMultiple(anyList(), anyBoolean(), any());
        }

        @Test
        void notificationSent_whenDisableNotifTrueButStatusNotComplete() {
            WorkOrder woAfter = buildWorkOrder(1L);
            stubPatch(woAfter, true, Status.IN_PROGRESS);
            woAfter.setAssignedTo(new ArrayList<>());
            woAfter.setCustomers(new ArrayList<>());

            workOrderService.patch(1L, new WorkOrderPatchDTO(), user);
            verify(notificationService).createMultiple(anyList(), anyBoolean(), any());
        }

        @Test
        void notificationSent_whenDisableNotifFalseAndStatusComplete() {
            WorkOrder woAfter = buildWorkOrder(1L);
            stubPatch(woAfter, false, Status.COMPLETE);
            woAfter.setAssignedTo(new ArrayList<>());
            woAfter.setCustomers(new ArrayList<>());

            workOrderService.patch(1L, new WorkOrderPatchDTO(), user);
            verify(notificationService).createMultiple(anyList(), anyBoolean(), any());
        }

        @Test
        void notificationSent_whenDisableNotifFalseAndStatusNotComplete() {
            WorkOrder woAfter = buildWorkOrder(1L);
            stubPatch(woAfter, false, Status.IN_PROGRESS);
            woAfter.setAssignedTo(new ArrayList<>());
            woAfter.setCustomers(new ArrayList<>());

            workOrderService.patch(1L, new WorkOrderPatchDTO(), user);
            verify(notificationService).createMultiple(anyList(), anyBoolean(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // update – category-change webhook
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class UpdateCategoryWebhook {

        private WorkOrder wo;

        @BeforeEach
        void init() {
            wo = buildWorkOrder(1L);
            wo.setCategory(null);
        }

        private void stubUpdate(WorkOrderCategory prevCat, WorkOrderCategory newCat) {
            wo.setCategory(prevCat);
            when(workOrderRepository.existsById(1L)).thenReturn(true);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            WorkOrder patched = buildWorkOrder(1L);
            patched.setCategory(newCat);
            when(workOrderMapper.updateWorkOrder(any(), any())).thenReturn(patched);
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(em).refresh(any());
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
        }

        @Test
        void firesOnNullToValue() {
            stubUpdate(null, buildCategory(10L, "New"));
            workOrderService.update(1L, new WorkOrderPatchDTO(), user);
            verify(webhookDispatchService, times(1)).dispatchWebhook(
                    eq(company), eq(com.grash.model.enums.webhook.WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void firesOnValueToNull() {
            stubUpdate(buildCategory(10L, "Old"), null);
            workOrderService.update(1L, new WorkOrderPatchDTO(), user);
            verify(webhookDispatchService, times(1)).dispatchWebhook(
                    eq(company), eq(com.grash.model.enums.webhook.WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void firesOnValueToDifferentValue() {
            stubUpdate(buildCategory(10L, "Old"), buildCategory(11L, "New"));
            workOrderService.update(1L, new WorkOrderPatchDTO(), user);
            verify(webhookDispatchService, times(1)).dispatchWebhook(
                    eq(company), eq(com.grash.model.enums.webhook.WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void doesNotFireOnUnchangedCategory() {
            WorkOrderCategory cat = buildCategory(10L, "Same");
            stubUpdate(cat, cat);
            workOrderService.update(1L, new WorkOrderPatchDTO(), user);
            verify(webhookDispatchService, never()).dispatchWebhook(
                    eq(company), eq(com.grash.model.enums.webhook.WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // create
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class Create {

        private void stubCreateBase(WorkOrder mapped) {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            when(workOrderMapper.fromPostDto(any())).thenReturn(mapped);
            when(customSequenceService.getNextWorkOrderSequence(any())).thenReturn(1L);
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(em).refresh(any());
            doNothing().when(notificationService).createMultiple(anyList(), anyBoolean(), any());
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("msg");
            lenient().when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            lenient().when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
        }

        @Test
        void assetStatusUpdatedOnCreate() {
            WorkOrderPostDTO postDto = new WorkOrderPostDTO();
            postDto.setTitle("New WO");
            postDto.setAsset(buildAsset(10L));
            postDto.setAssetStatus(AssetStatus.DOWN);

            Asset asset = buildAsset(10L);
            WorkOrder mapped = buildWorkOrder(1L);
            mapped.setAsset(asset);
            stubCreateBase(mapped);

            when(assetService.findById(10L)).thenReturn(Optional.of(asset));

            workOrderService.create(postDto, company);

            verify(assetService).save(argThat(a -> a.getStatus() == AssetStatus.DOWN));
        }

        @Test
        void customFieldsSetOnCreate() {
            WorkOrderPostDTO postDto = new WorkOrderPostDTO();
            postDto.setTitle("New WO");

            CustomFieldValuePostDTO cf = new CustomFieldValuePostDTO();
            cf.setId(1L);
            cf.setValue("val");
            postDto.setCustomFields(List.of(cf));

            WorkOrder mapped = buildWorkOrder(1L);
            mapped.setCustomFieldValues(new ArrayList<>());
            stubCreateBase(mapped);

            workOrderService.create(postDto, company);

            verify(customFieldValueService).setCustomFields(
                    any(), anyList(), eq(postDto.getCustomFields()),
                    eq(company), eq(CustomFieldEntityType.WORK_ORDER), any());
        }

        @Test
        void customFieldsEmpty_skipsSetWOCustomFields() {
            WorkOrderPostDTO postDto = new WorkOrderPostDTO();
            postDto.setTitle("New WO");
            postDto.setCustomFields(new ArrayList<>());

            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateBase(mapped);

            workOrderService.create(postDto, company);

            verify(customFieldValueService, never()).setCustomFields(
                    any(), anyList(), anyList(), any(), any(), any());
        }

        @Test
        void dispatchesNewWorkOrderWebhook() {
            WorkOrderPostDTO postDto = new WorkOrderPostDTO();
            postDto.setTitle("New WO");

            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateBase(mapped);

            workOrderService.create(postDto, company);

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company), eq(WebhookEvent.NEW_WORK_ORDER),
                    anyMap(), eq("newWorkOrder"), any(), isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        void generatesCustomIdFromSequence() {
            WorkOrderPostDTO postDto = new WorkOrderPostDTO();
            postDto.setTitle("New WO");

            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateBase(mapped);

            workOrderService.create(postDto, company);

            verify(workOrderRepository).saveAndFlush(argThat(wo -> "WO000001".equals(wo.getCustomId())));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // deleteByIdAndUser
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class DeleteByIdAndUser {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.deleteByIdAndUser(1L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void creatorCanDelete() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userService.findByCompany(anyLong())).thenReturn(Collections.emptyList());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("deleted");
            when(mailServiceFactory.getMailService()).thenReturn(mailService);

            assertDoesNotThrow(() -> workOrderService.deleteByIdAndUser(1L, user));
            verify(workOrderRepository).deleteById(1L);
        }

        @Test
        void deleteOtherPermission_allowsDelete() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userService.findByCompany(anyLong())).thenReturn(Collections.emptyList());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("deleted");
            when(mailServiceFactory.getMailService()).thenReturn(mailService);

            assertDoesNotThrow(() -> workOrderService.deleteByIdAndUser(1L, user));
            verify(workOrderRepository).deleteById(1L);
        }

        @Test
        void noPermission_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            role.getDeleteOtherPermissions().clear();
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.deleteByIdAndUser(1L, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(workOrderRepository, never()).deleteById(anyLong());
        }

        @Test
        void sendsDeletionEmailToSettingsAdmins() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("deleted");

            User admin = buildUser(2L);
            admin.getRole().getViewPermissions().add(PermissionEntity.SETTINGS);
            admin.getUserSettings().setEmailNotified(true);
            admin.setEnabled(true);
            when(userService.findByCompany(anyLong())).thenReturn(List.of(admin));
            when(mailServiceFactory.getMailService()).thenReturn(mailService);

            workOrderService.deleteByIdAndUser(1L, user);

            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{admin.getEmail()}),
                    eq("deleted"), anyMap(), eq("deleted-work-order.html"), any(), isNull());
        }

        @Test
        void dispatchesDeleteWebhook() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(userService.findByCompany(anyLong())).thenReturn(Collections.emptyList());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("deleted");
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());

            workOrderService.deleteByIdAndUser(1L, user);

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company), eq(WebhookEvent.WORK_ORDER_DELETE),
                    anyMap(), eq("deleteWorkOrder"), any(), isNull(), isNull(), isNull(), isNull(), isNull());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // createByUser
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CreateByUser {

        private WorkOrderPostDTO basePostDto() {
            WorkOrderPostDTO dto = new WorkOrderPostDTO();
            dto.setTitle("New WO");
            return dto;
        }

        private void stubCreateForUser(WorkOrder mapped) {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            when(workOrderMapper.fromPostDto(any())).thenReturn(mapped);
            when(customSequenceService.getNextWorkOrderSequence(any())).thenReturn(1L);
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(em).refresh(any());
            doNothing().when(notificationService).createMultiple(anyList(), anyBoolean(), any());
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("msg");
            lenient().when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            lenient().when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
        }

        @Test
        void noCreatePermission_throwsForbidden() {
            role.getCreatePermissions().clear();
            WorkOrderPostDTO postDto = basePostDto();

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.createByUser(postDto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void signatureWithoutPlanFeature_throwsForbidden() {
            SubscriptionPlan planWithoutSignature = SubscriptionPlan.builder()
                    .id(2L).name("Basic")
                    .features(new HashSet<>(Collections.singletonList(PlanFeatures.WEBHOOK)))
                    .build();
            Subscription subWithoutSignature = Subscription.builder()
                    .id(2L).subscriptionPlan(planWithoutSignature).build();
            company.setSubscription(subWithoutSignature);

            WorkOrderPostDTO postDto = basePostDto();
            postDto.setSignature("base64sig");

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.createByUser(postDto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void signatureWithPlanFeature_proceeds() {
            WorkOrderPostDTO postDto = basePostDto();
            postDto.setSignature("base64sig");
            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateForUser(mapped);

            assertDoesNotThrow(() -> workOrderService.createByUser(postDto, user));
        }

        @Test
        void autoAssignWorkOrders_setsPrimaryUserToRequester() {
            generalPreferences.setAutoAssignWorkOrders(true);

            WorkOrderPostDTO postDto = basePostDto();
            postDto.setPrimaryUser(null);
            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateForUser(mapped);

            workOrderService.createByUser(postDto, user);

            verify(workOrderMapper).fromPostDto(argThat(dto -> {
                WorkOrderPostDTO captured = (WorkOrderPostDTO) dto;
                return user.equals(captured.getPrimaryUser());
            }));
        }

        @Test
        void autoAssignWorkOrders_preservesExistingPrimaryUser() {
            generalPreferences.setAutoAssignWorkOrders(true);

            User existingPrimary = buildUser(50L);
            WorkOrderPostDTO postDto = basePostDto();
            postDto.setPrimaryUser(existingPrimary);
            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateForUser(mapped);

            workOrderService.createByUser(postDto, user);

            verify(workOrderMapper).fromPostDto(argThat(dto -> {
                WorkOrderPostDTO captured = (WorkOrderPostDTO) dto;
                return existingPrimary.equals(captured.getPrimaryUser());
            }));
        }

        @Test
        void firstWorkOrderCreated_firesIntercomEvent() {
            company.setFirstWorkOrderCreated(false);

            WorkOrderPostDTO postDto = basePostDto();
            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateForUser(mapped);

            workOrderService.createByUser(postDto, user);

            verify(intercomService).createCompanyActivationEvent(
                    eq("first-work-order-created"), eq(1L), eq(user.getEmail()), anyMap());
            verify(companyService).update(company);
            assertTrue(company.isFirstWorkOrderCreated());
        }

        @Test
        void subsequentWorkOrder_skipsIntercomEvent() {
            company.setFirstWorkOrderCreated(true);

            WorkOrderPostDTO postDto = basePostDto();
            WorkOrder mapped = buildWorkOrder(1L);
            stubCreateForUser(mapped);

            workOrderService.createByUser(postDto, user);

            verify(intercomService, never()).createCompanyActivationEvent(
                    anyString(), anyLong(), anyString(), anyMap());
            verify(companyService, never()).update(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // checkAccessToWorkOrderId
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CheckAccessToWorkOrderId {

        @Test
        void foundAndAccessible_returnsWorkOrder() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            WorkOrder result = workOrderService.checkAccessToWorkOrderId(1L, user);
            assertEquals(wo, result);
        }

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.checkAccessToWorkOrderId(1L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void foundButInaccessible_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            role.getViewOtherPermissions().clear();
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.checkAccessToWorkOrderId(1L, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // notify
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class NotifyTests {

        private void stubNotifyBase() {
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("msg");
            lenient().when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class))).thenReturn("msg");
            lenient().when(mailServiceFactory.getMailService()).thenReturn(mailService);
        }

        @Test
        void createsNotificationsForAllAssignedUsers() {
            stubNotifyBase();
            WorkOrder wo = buildWorkOrder(1L);
            User u1 = buildUser(10L);
            User u2 = buildUser(11L);
            wo.setAssignedTo(new ArrayList<>(List.of(u1, u2)));

            workOrderService.notify(wo, java.util.Locale.ENGLISH);

            verify(notificationService).createMultiple(argThat(list -> list.size() == 2), eq(true), anyString());
        }

        @Test
        void sendsEmailOnlyToEnabledUsersWithEmailPreference() {
            stubNotifyBase();
            WorkOrder wo = buildWorkOrder(1L);
            User enabled = buildUser(10L);
            enabled.setEnabled(true);
            enabled.getUserSettings().setEmailUpdatesForWorkOrders(true);
            User disabled = buildUser(11L);
            disabled.setEnabled(false);
            wo.setAssignedTo(new ArrayList<>(List.of(enabled, disabled)));

            workOrderService.notify(wo, java.util.Locale.ENGLISH);

            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{enabled.getEmail()}), any(), anyMap(), anyString(), any());
        }

        @Test
        void noEmailUsers_skipsMail() {
            stubNotifyBase();
            WorkOrder wo = buildWorkOrder(1L);
            User noEmail = buildUser(10L);
            noEmail.setEnabled(true);
            noEmail.getUserSettings().setEmailUpdatesForWorkOrders(false);
            wo.setAssignedTo(new ArrayList<>(List.of(noEmail)));

            workOrderService.notify(wo, java.util.Locale.ENGLISH);

            verify(mailService, never()).sendMessageUsingThymeleafTemplate(
                    any(), any(), anyMap(), anyString(), any());
        }

        @Test
        void emptyAssignedTo_sendsNoNotifications() {
            stubNotifyBase();
            WorkOrder wo = buildWorkOrder(1L);
            wo.setAssignedTo(new ArrayList<>());

            workOrderService.notify(wo, java.util.Locale.ENGLISH);

            verify(notificationService).createMultiple(argThat(List::isEmpty), eq(true), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // patchNotify
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class PatchNotifyTests {

        private void stubPatchNotifyBase() {
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("msg");
            lenient().when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class))).thenReturn("msg");
            lenient().when(mailServiceFactory.getMailService()).thenReturn(mailService);
        }

        @Test
        void notifiesOnlyNewlyAssignedUsers() {
            stubPatchNotifyBase();
            User existing = buildUser(10L);
            User newUser = buildUser(20L);
            WorkOrder oldWo = buildWorkOrder(1L);
            oldWo.setPrimaryUser(existing);
            oldWo.setAssignedTo(new ArrayList<>());
            WorkOrder newWo = buildWorkOrder(1L);
            newWo.setPrimaryUser(existing);
            newWo.setAssignedTo(new ArrayList<>(List.of(newUser)));

            workOrderService.patchNotify(oldWo, newWo, java.util.Locale.ENGLISH);

            verify(notificationService).createMultiple(argThat(list -> list.size() == 1), eq(true), any());
        }

        @Test
        void noNewUsers_sendsNoNotifications() {
            stubPatchNotifyBase();
            User existing = buildUser(10L);
            WorkOrder oldWo = buildWorkOrder(1L);
            oldWo.setPrimaryUser(existing);
            oldWo.setAssignedTo(new ArrayList<>());
            WorkOrder newWo = buildWorkOrder(1L);
            newWo.setPrimaryUser(existing);
            newWo.setAssignedTo(new ArrayList<>());

            workOrderService.patchNotify(oldWo, newWo, java.util.Locale.ENGLISH);

            verify(notificationService).createMultiple(argThat(List::isEmpty), eq(true), any());
        }

        @Test
        void sendsEmailOnlyToEnabledNewUsers() {
            stubPatchNotifyBase();
            User enabled = buildUser(20L);
            enabled.setEnabled(true);
            enabled.getUserSettings().setEmailUpdatesForWorkOrders(true);
            User disabled = buildUser(30L);
            disabled.setEnabled(false);

            WorkOrder oldWo = buildWorkOrder(1L);
            oldWo.setPrimaryUser(user);
            oldWo.setAssignedTo(new ArrayList<>());
            WorkOrder newWo = buildWorkOrder(1L);
            newWo.setPrimaryUser(user);
            newWo.setAssignedTo(new ArrayList<>(List.of(enabled, disabled)));

            workOrderService.patchNotify(oldWo, newWo, java.util.Locale.ENGLISH);

            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{enabled.getEmail()}), any(), anyMap(), anyString(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // getWorkOrderFromWorkOrderBase
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class GetWorkOrderFromWorkOrderBase {

        @Test
        void copiesAllFields() {
            WorkOrder source = buildWorkOrder(1L);
            source.setTitle("title");
            source.setDescription("desc");
            source.setPriority(Priority.HIGH);
            source.setEstimatedDuration(5.0);
            source.setRequiredSignature(true);
            source.setAsset(buildAsset(10L));
            source.setLocation(buildLocation(20L));
            source.setTeam(buildTeam(30L));
            source.setCategory(buildCategory(40L, "Cat"));
            User pu = buildUser(50L);
            source.setPrimaryUser(pu);
            source.setAssignedTo(new ArrayList<>(List.of(buildUser(60L))));
            source.setCustomers(new ArrayList<>(List.of(buildCustomer(70L))));

            WorkOrderPostDTO result = workOrderService.getWorkOrderFromWorkOrderBase(source);

            assertEquals("title", result.getTitle());
            assertEquals("desc", result.getDescription());
            assertEquals(Priority.HIGH, result.getPriority());
            assertEquals(5.0, result.getEstimatedDuration());
            assertTrue(result.isRequiredSignature());
            assertEquals(10L, result.getAsset().getId());
            assertEquals(20L, result.getLocation().getId());
            assertEquals(30L, result.getTeam().getId());
            assertEquals(40L, result.getCategory().getId());
            assertEquals(pu, result.getPrimaryUser());
            assertEquals(1, result.getAssignedTo().size());
        }

        @Test
        void transformsCustomFieldValues() {
            WorkOrder source = buildWorkOrder(1L);
            CustomField cf = new CustomField();
            cf.setId(99L);
            CustomFieldValue cfv = new CustomFieldValue();
            cfv.setCustomField(cf);
            cfv.setValue("test-value");
            source.setCustomFieldValues(new ArrayList<>(List.of(cfv)));

            WorkOrderPostDTO result = workOrderService.getWorkOrderFromWorkOrderBase(source);

            assertEquals(1, result.getCustomFields().size());
            assertEquals(99L, result.getCustomFields().get(0).getId());
            assertEquals("test-value", result.getCustomFields().get(0).getValue());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // getEvents
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class GetEvents {

        private DateRange buildDateRange(Date start, Date end) {
            return DateRange.builder().start(start).end(end).build();
        }

        private CalendarEvent<PreventiveMaintenance> buildPMCalendarEvent(PreventiveMaintenance pm, Date date) {
            CalendarEvent<PreventiveMaintenance> ce = new CalendarEvent<>();
            ce.setType("PREVENTIVE_MAINTENANCE");
            ce.setEvent(pm);
            ce.setDate(date);
            return ce;
        }

        private PreventiveMaintenance buildPM(long id, Long createdBy) {
            PreventiveMaintenance pm = new PreventiveMaintenance();
            pm.setId(id);
            pm.setCreatedBy(createdBy);
            return pm;
        }

        @Test
        void noPermission_throwsAccessDenied() {
            role.getViewPermissions().remove(PermissionEntity.WORK_ORDERS);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));

            assertThrows(CustomException.class,
                    () -> workOrderService.getEvents(range, null, user));
        }

        @Test
        void simpleFetch_returnsWorkOrdersAndPMs() {
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
            role.getViewOtherPermissions().add(PermissionEntity.PREVENTIVE_MAINTENANCES);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            Date futureDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

            PreventiveMaintenance pm = buildPM(10L, 99L);
            CalendarEvent<PreventiveMaintenance> pmEvent = buildPMCalendarEvent(pm, futureDate);
            when(preventiveMaintenanceService.getEvents(range.getEnd(), company.getId()))
                    .thenReturn(List.of(pmEvent));

            WorkOrder wo = buildWorkOrder(1L);
            wo.setDueDate(futureDate);
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(range.getStart(), range.getEnd(),
                    company.getId()))
                    .thenReturn(List.of(wo));

            WorkOrderBaseMiniDTO pmDto = new WorkOrderBaseMiniDTO();
            when(preventiveMaintenanceMapper.toBaseMiniDto(pm)).thenReturn(pmDto);

            WorkOrderBaseMiniDTO woDto = new WorkOrderBaseMiniDTO();
            when(workOrderMapper.toBaseMiniDto(wo)).thenReturn(woDto);

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertEquals(2, result.size());
        }

        @Test
        void filtersOutPastPMEvents() {
            role.getViewOtherPermissions().add(PermissionEntity.PREVENTIVE_MAINTENANCES);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            Date pastDate = new Date(1000);

            PreventiveMaintenance pm = buildPM(10L, 99L);
            CalendarEvent<PreventiveMaintenance> pmEvent = buildPMCalendarEvent(pm, pastDate);
            when(preventiveMaintenanceService.getEvents(range.getEnd(), company.getId()))
                    .thenReturn(List.of(pmEvent));

            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertTrue(result.isEmpty());
        }

        @Test
        void filtersWOs_userWithoutViewOtherPermission() {
            role.getViewOtherPermissions().remove(PermissionEntity.WORK_ORDERS);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            Date futureDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

            when(preventiveMaintenanceService.getEvents(any(), any()))
                    .thenReturn(Collections.emptyList());

            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(999L);
            wo.setPrimaryUser(buildUser(888L));
            wo.setAssignedTo(new ArrayList<>());
            wo.setDueDate(futureDate);
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(range.getStart(), range.getEnd(),
                    company.getId()))
                    .thenReturn(List.of(wo));

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertTrue(result.isEmpty());
        }

        @Test
        void userCanViewOwnCreatedWO() {
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            Date futureDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

            when(preventiveMaintenanceService.getEvents(any(), any()))
                    .thenReturn(Collections.emptyList());

            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            wo.setPrimaryUser(buildUser(888L));
            wo.setAssignedTo(new ArrayList<>());
            wo.setDueDate(futureDate);
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(range.getStart(), range.getEnd(),
                    company.getId()))
                    .thenReturn(List.of(wo));

            WorkOrderBaseMiniDTO woDto = new WorkOrderBaseMiniDTO();
            when(workOrderMapper.toBaseMiniDto(wo)).thenReturn(woDto);

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertEquals(1, result.size());
            assertEquals(woDto, result.iterator().next().getEvent());
        }

        @Test
        void userCanViewAssignedWO() {
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            Date futureDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

            when(preventiveMaintenanceService.getEvents(any(), any()))
                    .thenReturn(Collections.emptyList());

            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(999L);
            wo.setPrimaryUser(user);
            wo.setAssignedTo(new ArrayList<>());
            wo.setDueDate(futureDate);
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(range.getStart(), range.getEnd(),
                    company.getId()))
                    .thenReturn(List.of(wo));

            WorkOrderBaseMiniDTO woDto = new WorkOrderBaseMiniDTO();
            when(workOrderMapper.toBaseMiniDto(wo)).thenReturn(woDto);

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertEquals(1, result.size());
        }

        @Test
        void companyIdSpecified_filtersToOnlyThatCompany() {
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));

            when(preventiveMaintenanceService.getEvents(range.getEnd(), company.getId()))
                    .thenReturn(Collections.emptyList());
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(range.getStart(), range.getEnd(),
                    company.getId()))
                    .thenReturn(Collections.emptyList());

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, company.getId(), user);

            verify(workOrderRepository).findByDueDateBetweenAndCompany_Id(range.getStart(), range.getEnd(),
                    company.getId());
            assertTrue(result.isEmpty());
        }

        @Test
        void companyIdSpecified_noAccess_throwsAccessDenied() {
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));

            assertThrows(CustomException.class,
                    () -> workOrderService.getEvents(range, 999L, user));
        }

        @Test
        void emptyResults_returnsEmptyList() {
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));

            when(preventiveMaintenanceService.getEvents(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertTrue(result.isEmpty());
        }

        @Test
        void canViewAssignedPM_viaTeamMembership() {
            role.getViewOtherPermissions().add(PermissionEntity.PREVENTIVE_MAINTENANCES);
            DateRange range = buildDateRange(new Date(0),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            Date futureDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

            PreventiveMaintenance pm = buildPM(10L, 999L);
            pm.setPrimaryUser(user);
            pm.setAssignedTo(new ArrayList<>());
            CalendarEvent<PreventiveMaintenance> pmEvent = buildPMCalendarEvent(pm, futureDate);
            when(preventiveMaintenanceService.getEvents(range.getEnd(), company.getId()))
                    .thenReturn(List.of(pmEvent));

            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            WorkOrderBaseMiniDTO pmDto = new WorkOrderBaseMiniDTO();
            when(preventiveMaintenanceMapper.toBaseMiniDto(pm)).thenReturn(pmDto);

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            assertEquals(1, result.size());
        }

        @Test
        void withSuperAccountRelations_queriesChildCompanyInsteadOfOwn() {
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
            DateRange range = DateRange.builder()
                    .start(new Date(0))
                    .end(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)))
                    .build();

            Company childCompany = new Company("Child", 10, subscription);
            childCompany.setId(2L);
            childCompany.setCompanySettings(companySettings);
            User childUser = buildUser(2L);
            childUser.setCompany(childCompany);
            SuperAccountRelation relation = new SuperAccountRelation();
            relation.setChildUser(childUser);
            user.setSuperAccountRelations(List.of(relation));

            when(preventiveMaintenanceService.getEvents(any(), any())).thenReturn(Collections.emptyList());
            when(workOrderRepository.findByDueDateBetweenAndCompany_Id(any(), any(), anyLong()))
                    .thenReturn(Collections.emptyList());

            Collection<CalendarEvent<WorkOrderBaseMiniDTO>> result =
                    workOrderService.getEvents(range, null, user);

            verify(workOrderRepository, never())
                    .findByDueDateBetweenAndCompany_Id(any(), any(), eq(company.getId()));
            verify(workOrderRepository)
                    .findByDueDateBetweenAndCompany_Id(any(), any(), eq(2L));
            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // cost computation
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CostComputation {

        private WorkOrder wo1;
        private WorkOrder wo2;

        @BeforeEach
        void init() {
            wo1 = buildWorkOrder(1L);
            wo2 = buildWorkOrder(2L);
        }

        private Labor buildLabor(long hourlyRate, long duration) {
            Labor labor = new Labor();
            labor.setHourlyRate(hourlyRate);
            labor.setDuration(duration);
            return labor;
        }

        private AdditionalCost buildAdditionalCost(double cost) {
            AdditionalCost ac = new AdditionalCost();
            ac.setCost(cost);
            return ac;
        }

        private PartQuantity buildPartQuantity(double partCost, double quantity) {
            Part part = new Part();
            part.setCost(partCost);
            PartQuantity pq = new PartQuantity();
            pq.setPart(part);
            pq.setQuantity(quantity);
            return pq;
        }

        @Test
        void getLaborCostAndTime_multipleWorkOrders() {
            when(laborService.findByWorkOrder(1L)).thenReturn(List.of(
                    buildLabor(100, 7200),
                    buildLabor(50, 3600)));
            when(laborService.findByWorkOrder(2L)).thenReturn(List.of(
                    buildLabor(200, 1800)));

            Pair<Long, Long> result = workOrderService.getLaborCostAndTime(List.of(wo1, wo2));

            long expectedCost1 = 100 * 7200 / 3600 + 50 * 3600 / 3600;
            long expectedCost2 = 200 * 1800 / 3600;
            assertEquals(expectedCost1 + expectedCost2, result.getFirst());
            assertEquals(7200 + 3600 + 1800, result.getSecond());
        }

        @Test
        void getLaborCostAndTime_emptyLabors() {
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());

            Pair<Long, Long> result = workOrderService.getLaborCostAndTime(List.of(wo1));

            assertEquals(0L, result.getFirst());
            assertEquals(0L, result.getSecond());
        }

        @Test
        void getLaborCostAndTime_emptyWorkOrders() {
            Pair<Long, Long> result = workOrderService.getLaborCostAndTime(Collections.emptyList());

            assertEquals(0L, result.getFirst());
            assertEquals(0L, result.getSecond());
        }

        @Test
        void getAdditionalCost_multipleWorkOrders() {
            when(additionalCostService.findByWorkOrder(1L)).thenReturn(List.of(
                    buildAdditionalCost(100.0), buildAdditionalCost(50.5)));
            when(additionalCostService.findByWorkOrder(2L)).thenReturn(List.of(
                    buildAdditionalCost(25.0)));

            double result = workOrderService.getAdditionalCost(List.of(wo1, wo2));

            assertEquals(175.5, result, 0.001);
        }

        @Test
        void getAdditionalCost_emptyCosts() {
            when(additionalCostService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());

            assertEquals(0.0, workOrderService.getAdditionalCost(List.of(wo1)), 0.001);
        }

        @Test
        void getPartCost_multipleWorkOrders() {
            when(partQuantityService.findByWorkOrder(1L)).thenReturn(List.of(
                    buildPartQuantity(10.0, 3), buildPartQuantity(5.0, 2)));
            when(partQuantityService.findByWorkOrder(2L)).thenReturn(List.of(
                    buildPartQuantity(20.0, 1)));

            double result = workOrderService.getPartCost(List.of(wo1, wo2));

            assertEquals(10.0 * 3 + 5.0 * 2 + 20.0 * 1, result, 0.001);
        }

        @Test
        void getPartCost_emptyParts() {
            when(partQuantityService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());

            assertEquals(0.0, workOrderService.getPartCost(List.of(wo1)), 0.001);
        }

        @Test
        void getAllCost_withLaborCost() {
            when(partQuantityService.findByWorkOrder(anyLong())).thenReturn(Collections.emptyList());
            when(additionalCostService.findByWorkOrder(anyLong())).thenReturn(Collections.emptyList());
            when(laborService.findByWorkOrder(1L)).thenReturn(List.of(buildLabor(100, 7200)));

            double result = workOrderService.getAllCost(List.of(wo1), true);

            assertEquals(200.0, result, 0.001);
        }

        @Test
        void getAllCost_withoutLaborCost() {
            when(partQuantityService.findByWorkOrder(anyLong())).thenReturn(Collections.emptyList());
            when(additionalCostService.findByWorkOrder(1L)).thenReturn(List.of(buildAdditionalCost(50.0)));

            double result = workOrderService.getAllCost(List.of(wo1), false);

            assertEquals(50.0, result, 0.001);
        }

        @Test
        void getAllCost_emptyWorkOrders() {
            assertEquals(0.0, workOrderService.getAllCost(Collections.emptyList(), true), 0.001);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // getWorkOrdersByPart
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class GetWorkOrdersByPart {

        @Test
        void deduplicatesSameWorkOrder() {
            WorkOrder wo = buildWorkOrder(1L);
            PartQuantity pq1 = new PartQuantity();
            pq1.setWorkOrder(wo);
            PartQuantity pq2 = new PartQuantity();
            pq2.setWorkOrder(wo);
            when(partQuantityService.findByPart(10L)).thenReturn(List.of(pq1, pq2));

            List<WorkOrder> result = workOrderService.getWorkOrdersByPart(10L);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }

        @Test
        void filtersNullWorkOrders() {
            WorkOrder wo = buildWorkOrder(1L);
            PartQuantity pq1 = new PartQuantity();
            pq1.setWorkOrder(wo);
            PartQuantity pq2 = new PartQuantity();
            pq2.setWorkOrder(null);
            when(partQuantityService.findByPart(10L)).thenReturn(List.of(pq1, pq2));

            List<WorkOrder> result = workOrderService.getWorkOrdersByPart(10L);

            assertEquals(1, result.size());
        }

        @Test
        void emptyPartQuantities_returnsEmptyList() {
            when(partQuantityService.findByPart(10L)).thenReturn(Collections.emptyList());

            List<WorkOrder> result = workOrderService.getWorkOrdersByPart(10L);

            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // addFiles
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class AddFilesTests {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(CustomException.class,
                    () -> workOrderService.addFiles(1L, List.of(new com.grash.model.File()), user));
        }

        @Test
        void cannotEdit_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            role.getEditOtherPermissions().clear();
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThrows(CustomException.class,
                    () -> workOrderService.addFiles(1L, List.of(new com.grash.model.File()), user));
        }

        @Test
        void addsFilesAndReturnsUpdatedList() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            com.grash.model.File f = new com.grash.model.File();
            f.setId(10L);
            f.setName("file.txt");

            List<com.grash.model.File> result = workOrderService.addFiles(1L, List.of(f), user);

            assertEquals(1, result.size());
            assertEquals("file.txt", result.get(0).getName());
            verify(workOrderRepository).save(wo);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // removeFile
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class RemoveFileTests {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(CustomException.class,
                    () -> workOrderService.removeFile(1L, 10L, user));
        }

        @Test
        void cannotEdit_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            role.getEditOtherPermissions().clear();
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            assertThrows(CustomException.class,
                    () -> workOrderService.removeFile(1L, 10L, user));
        }

        @Test
        void removesExistingFile() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            com.grash.model.File f = new com.grash.model.File();
            f.setId(10L);
            wo.setFiles(new ArrayList<>(List.of(f)));
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<com.grash.model.File> result = workOrderService.removeFile(1L, 10L, user);

            assertTrue(result.isEmpty());
            verify(workOrderRepository).save(wo);
        }

        @Test
        void fileNotFoundInList_stillSaves() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(user.getId());
            wo.setFiles(new ArrayList<>());
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<com.grash.model.File> result = workOrderService.removeFile(1L, 99L, user);

            assertTrue(result.isEmpty());
            verify(workOrderRepository).save(wo);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // countUrgent
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CountUrgent {

        @Test
        void countUrgent_returnsCount() {
            when(workOrderRepository.count(any(Specification.class))).thenReturn(3L);
            Integer result = workOrderService.countUrgent(user);
            assertEquals(3, result);
        }

        @Test
        void countUrgent_zeroWhenNone() {
            when(workOrderRepository.count(any(Specification.class))).thenReturn(0L);
            Integer result = workOrderService.countUrgent(user);
            assertEquals(0, result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // patch - forbidden and not found branches
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class PatchForbiddenNotFound {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.patch(1L, new WorkOrderPatchDTO(), user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotEdit_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            role.getEditOtherPermissions().clear();
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.patch(1L, new WorkOrderPatchDTO(), user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void archived_workflowExecuted() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setArchived(false);
            WorkOrder woAfterPatch = buildWorkOrder(1L);
            woAfterPatch.setArchived(true);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.existsById(1L)).thenReturn(true);
            when(workOrderMapper.updateWorkOrder(any(), any())).thenReturn(woAfterPatch);
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(em).refresh(any());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong()))
                    .thenReturn(List.of(new Workflow()));

            workOrderService.patch(1L, new WorkOrderPatchDTO(), user);

            verify(workflowService).runWorkOrder(any(Workflow.class), any(WorkOrder.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // update - not found branch
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class UpdateNotFound {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.existsById(1L)).thenReturn(false);
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.update(1L, new WorkOrderPatchDTO(), user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // update - custom fields branch
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class UpdateCustomFields {

        @Test
        void customFieldsNotEmpty_callsSetCustomFields() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCompany(company);
            WorkOrderPatchDTO patch = new WorkOrderPatchDTO();
            patch.setCustomFields(List.of(new CustomFieldValuePostDTO()));

            when(workOrderRepository.existsById(1L)).thenReturn(true);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderMapper.updateWorkOrder(any(), any())).thenReturn(wo);
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(em).refresh(any());
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());

            workOrderService.update(1L, patch, user);

            verify(customFieldValueService).setCustomFields(
                    any(), anyList(), anyList(), any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // saveAndFlushWithWebhook - category changed branch
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class SaveAndFlushWithWebhookCategoryChanged {

        private WorkOrder original;
        private WorkOrder updated;

        @BeforeEach
        void init() {
            original = buildWorkOrder(1L);
            original.setCategory(buildCategory(10L, "Old"));
            updated = buildWorkOrder(1L);
            updated.setCategory(buildCategory(11L, "New"));
        }

        private void stubBase() {
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            doNothing().when(em).refresh(any());
        }

        @Test
        void firesNewCategoryWebhook_whenCategoryChanged() {
            stubBase();
            workOrderService.saveAndFlushWithWebhook(updated, company, original);
            verify(webhookDispatchService, atLeastOnce()).dispatchWebhook(
                    eq(company), eq(WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void doesNotFireCategoryWebhook_whenCategoryUnchanged() {
            updated.setCategory(buildCategory(10L, "Old"));
            stubBase();
            workOrderService.saveAndFlushWithWebhook(updated, company, original);
            verify(webhookDispatchService, never()).dispatchWebhook(
                    eq(company), eq(WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER),
                    anyMap(), anyString(), any(), any(), any(), any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // changeStatus - PM schedule next job
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class ChangeStatusPMSchedule {

        @Test
        void transitionToComplete_withParentPM_schedulesNextJob() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setAsset(buildAsset(10L));
            wo.setStatus(Status.OPEN);
            wo.setFirstTimeToReact(null);
            Schedule schedule = new Schedule();
            schedule.setId(42L);
            PreventiveMaintenance pm = new PreventiveMaintenance();
            pm.setSchedule(schedule);
            wo.setParentPreventiveMaintenance(pm);

            WorkOrderChangeStatusDTO dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(Status.COMPLETE);

            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                saved.setCompletedOn(new Date());
                return saved;
            });
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            doNothing().when(em).refresh(any());
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(workOrderRepository.findByAsset_Id(10L)).thenReturn(Collections.emptyList());

            workOrderService.changeStatus(dto, 1L, user, "ios");

            verify(scheduleService).scheduleNextWorkOrderJobAfterCompletion(eq(42L), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // changeStatus - requester notification with ID
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class ChangeStatusRequesterWithId {

        @Test
        void requesterWithId_sendsNotification() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setAsset(buildAsset(10L));
            wo.setStatus(Status.OPEN);
            wo.setFirstTimeToReact(null);
            Request req = new Request();
            req.setCreatedBy(5L);
            wo.setParentRequest(req);
            User requester = buildUser(5L);

            WorkOrderChangeStatusDTO dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(Status.COMPLETE);

            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
                WorkOrder saved = inv.getArgument(0);
                saved.setId(1L);
                saved.setCompletedOn(new Date());
                return saved;
            });
            when(workOrderMapper.toShowDto(any())).thenReturn(new com.grash.dto.workOrder.WorkOrderShowDTO());
            doNothing().when(em).refresh(any());
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(workOrderRepository.findByAsset_Id(10L)).thenReturn(Collections.emptyList());
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            when(userService.findById(5L)).thenReturn(Optional.of(requester));
            generalPreferences.setWoUpdateForRequesters(true);

            workOrderService.changeStatus(dto, 1L, user, "ios");

            verify(notificationService).create(any(Notification.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // sendReport - not found and access denied
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class SendReportNotFoundAndDenied {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            WorkOrderSendReportDTO dto = new WorkOrderSendReportDTO();
            dto.setCustomers(List.of(buildCustomer(1L)));
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.sendReport(1L, dto, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void noPermission_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            role.getViewPermissions().remove(PermissionEntity.WORK_ORDERS);
            WorkOrderSendReportDTO dto = new WorkOrderSendReportDTO();
            dto.setCustomers(List.of(buildCustomer(1L)));
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.sendReport(1L, dto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void noViewOtherAndNotCreatorOrAssigned_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            wo.setAssignedTo(new ArrayList<>());
            wo.setPrimaryUser(buildUser(99L));
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            role.getViewOtherPermissions().remove(PermissionEntity.WORK_ORDERS);
            WorkOrderSendReportDTO dto = new WorkOrderSendReportDTO();
            dto.setCustomers(List.of(buildCustomer(1L)));
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.sendReport(1L, dto, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // generateReport - not found and access denied
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class GenerateReportNotFoundAndDenied {

        @Test
        void notFound_throwsNotFound() {
            when(workOrderRepository.findById(1L)).thenReturn(Optional.empty());
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.generateReport(1L, user, new ReportConfig()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void noPermission_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            role.getViewPermissions().remove(PermissionEntity.WORK_ORDERS);
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.generateReport(1L, user, new ReportConfig()));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void noViewOtherAndNotCreatorOrAssigned_throwsForbidden() {
            WorkOrder wo = buildWorkOrder(1L);
            wo.setCreatedBy(99L);
            wo.setAssignedTo(new ArrayList<>());
            wo.setPrimaryUser(buildUser(99L));
            when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
            role.getViewOtherPermissions().remove(PermissionEntity.WORK_ORDERS);
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.generateReport(1L, user, new ReportConfig()));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // importWorkOrder
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class ImportWorkOrderTests {

        private WorkOrder givenWorkOrder() {
            return new WorkOrder();
        }

        @Test
        void importWorkOrder_setsAllFields() {
            WorkOrder workOrder = givenWorkOrder();
            User completedBy = buildUser(20L);
            completedBy.setEmail("tech@company.com");
            Customer customer = buildCustomer(30L);
            customer.setName("Client Inc");

            WorkOrderImportDTO dto = WorkOrderImportDTO.builder()
                    .title("Imported WO")
                    .description("Desc")
                    .status("Open")
                    .priority("HIGH")
                    .estimatedDuration(3.0)
                    .dueDate(45000.0)
                    .requiredSignature("Yes")
                    .category("Electrical")
                    .locationName("Warehouse")
                    .teamName("Team Alpha")
                    .primaryUserEmail("lead@company.com")
                    .assignedToEmails(List.of("tech@company.com"))
                    .assetName("Generator")
                    .completedByEmail("tech@company.com")
                    .completedOn(46000.0)
                    .archived("false")
                    .feedback("All good")
                    .customersNames(List.of("Client Inc"))
                    .build();

            lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            lenient().when(customSequenceService.getNextWorkOrderSequence(any())).thenReturn(1L);
            when(workOrderCategoryService.getOrCreate("Electrical", company.getCompanySettings()))
                    .thenReturn(buildCategory(100L, "Electrical"));
            Location expectedLoc = new Location();
            expectedLoc.setId(200L);
            expectedLoc.setName("Warehouse");
            expectedLoc.setCompany(company);
            when(locationService.findByNameIgnoreCaseAndCompany("Warehouse", company.getId()))
                    .thenReturn(List.of(expectedLoc));
            Team expectedTeam = new Team();
            expectedTeam.setId(300L);
            expectedTeam.setName("Team Alpha");
            expectedTeam.setCompany(company);
            expectedTeam.setUsers(new ArrayList<>());
            when(teamService.findByNameIgnoreCaseAndCompany("Team Alpha", company.getId()))
                    .thenReturn(Optional.of(expectedTeam));
            User expectedPrimaryUser = new User();
            expectedPrimaryUser.setId(99L);
            expectedPrimaryUser.setEmail("lead@company.com");
            expectedPrimaryUser.setCompany(company);
            expectedPrimaryUser.setRole(role);
            expectedPrimaryUser.setEnabled(true);
            expectedPrimaryUser.setUserSettings(new UserSettings());
            when(userService.findByEmailAndCompany("lead@company.com", company.getId()))
                    .thenReturn(Optional.of(expectedPrimaryUser));
            when(userService.findByEmailAndCompany("tech@company.com", company.getId()))
                    .thenReturn(Optional.of(completedBy));
            Asset expectedAsset = new Asset();
            expectedAsset.setId(400L);
            expectedAsset.setName("Generator");
            expectedAsset.setCompany(company);
            when(assetService.findByNameIgnoreCaseAndCompany("Generator", company.getId()))
                    .thenReturn(List.of(expectedAsset));
            when(customerService.findByNameIgnoreCaseAndCompany("Client Inc", company.getId()))
                    .thenReturn(Optional.of(customer));

            workOrderService.importWorkOrder(workOrder, dto, company);

            assertEquals("Imported WO", workOrder.getTitle());
            assertEquals("Desc", workOrder.getDescription());
            assertEquals(Priority.HIGH, workOrder.getPriority());
            assertEquals(3.0, workOrder.getEstimatedDuration(), 0.001);
            assertNotNull(workOrder.getCategory());
            assertEquals("Electrical", workOrder.getCategory().getName());
            assertNotNull(workOrder.getLocation());
            assertEquals("Warehouse", workOrder.getLocation().getName());
            assertNotNull(workOrder.getTeam());
            assertEquals("Team Alpha", workOrder.getTeam().getName());
            assertNotNull(workOrder.getPrimaryUser());
            assertEquals(1, workOrder.getAssignedTo().size());
            assertNotNull(workOrder.getAsset());
            assertEquals("Generator", workOrder.getAsset().getName());
            assertEquals(company, workOrder.getCompany());
            assertNotNull(workOrder.getDueDate());
            assertEquals("WO000001", workOrder.getCustomId());
            assertTrue(workOrder.isRequiredSignature());
            assertNotNull(workOrder.getCompletedBy());
            assertEquals("tech@company.com", workOrder.getCompletedBy().getEmail());
            assertNotNull(workOrder.getCompletedOn());
            assertFalse(workOrder.isArchived());
            assertEquals(Status.OPEN, workOrder.getStatus());
            assertEquals("All good", workOrder.getFeedback());
            assertEquals(1, workOrder.getCustomers().size());
            assertEquals("Client Inc", workOrder.getCustomers().get(0).getName());
            verify(licenseService).hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS);
        }

        @Test
        void importWorkOrder_whenCompletedByNotFound_doesNotSetCompletedBy() {
            WorkOrder workOrder = givenWorkOrder();
            WorkOrderImportDTO dto = WorkOrderImportDTO.builder()
                    .title("WO no completed")
                    .description("Desc")
                    .status("Complete")
                    .priority("LOW")
                    .estimatedDuration(1.0)
                    .requiredSignature("No")
                    .archived("true")
                    .primaryUserEmail("lead@company.com")
                    .completedByEmail("nonexistent@company.com")
                    .customersNames(new ArrayList<>())
                    .assignedToEmails(new ArrayList<>())
                    .build();

            lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            lenient().when(customSequenceService.getNextWorkOrderSequence(any())).thenReturn(1L);
            lenient().when(workOrderCategoryService.findByNameIgnoreCaseAndCompanySettings(any(), any()))
                    .thenReturn(Optional.empty());
            lenient().when(locationService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(new ArrayList<>());
            lenient().when(teamService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Optional.empty());
            lenient().when(userService.findByEmailAndCompany("lead@company.com", company.getId()))
                    .thenReturn(Optional.of(buildUser(99L)));
            lenient().when(userService.findByEmailAndCompany("nonexistent@company.com", company.getId()))
                    .thenReturn(Optional.empty());
            lenient().when(assetService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(new ArrayList<>());

            workOrderService.importWorkOrder(workOrder, dto, company);

            assertNull(workOrder.getCompletedBy());
            assertNull(workOrder.getCompletedOn());
            assertEquals(Status.COMPLETE, workOrder.getStatus());
            assertTrue(workOrder.isArchived());
            assertFalse(workOrder.isRequiredSignature());
            assertTrue(workOrder.getCustomers().isEmpty());
            assertNull(workOrder.getDueDate());
        }

        @Test
        void importWorkOrder_whenCustomerNotFound_doesNotAddCustomer() {
            WorkOrder workOrder = givenWorkOrder();
            WorkOrderImportDTO dto = WorkOrderImportDTO.builder()
                    .title("WO customer miss")
                    .description("Desc")
                    .status("Open")
                    .priority("NONE")
                    .estimatedDuration(1.0)
                    .requiredSignature("No")
                    .archived("false")
                    .primaryUserEmail("lead@company.com")
                    .customersNames(List.of("Mystery Corp"))
                    .assignedToEmails(new ArrayList<>())
                    .build();

            lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            lenient().when(customSequenceService.getNextWorkOrderSequence(any())).thenReturn(1L);
            lenient().when(workOrderCategoryService.findByNameIgnoreCaseAndCompanySettings(any(), any()))
                    .thenReturn(Optional.empty());
            lenient().when(locationService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(new ArrayList<>());
            lenient().when(teamService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(Optional.empty());
            lenient().when(userService.findByEmailAndCompany("lead@company.com", company.getId()))
                    .thenReturn(Optional.of(buildUser(99L)));
            lenient().when(assetService.findByNameIgnoreCaseAndCompany(any(), any())).thenReturn(new ArrayList<>());
            lenient().when(customerService.findByNameIgnoreCaseAndCompany("Mystery Corp", company.getId()))
                    .thenReturn(Optional.empty());

            workOrderService.importWorkOrder(workOrder, dto, company);

            assertTrue(workOrder.getCustomers().isEmpty());
        }

        @Test
        void importWorkOrder_throwsWhenUsageLimitExceeded() {
            WorkOrder workOrder = givenWorkOrder();
            WorkOrderImportDTO dto = WorkOrderImportDTO.builder()
                    .title("Limit WO")
                    .description("Desc")
                    .status("Open")
                    .priority("NONE")
                    .estimatedDuration(1.0)
                    .requiredSignature("No")
                    .archived("false")
                    .customersNames(new ArrayList<>())
                    .assignedToEmails(new ArrayList<>())
                    .build();

            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(false);
            when(workOrderRepository.hasMoreActiveThan(eq(company.getId()), anyLong())).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.importWorkOrder(workOrder, dto, company));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }
}
