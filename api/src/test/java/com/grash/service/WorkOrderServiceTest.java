package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPatchDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WOField;
import com.grash.repository.WorkOrderRepository;
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
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private LocationService locationService;
    @Mock
    private CustomerService customerService;
    @Mock
    private TeamService teamService;
    @Mock
    private AssetService assetService;
    @Mock
    private UserService userService;
    @Mock
    private CompanyService companyService;
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
    private WorkOrderCategoryService workOrderCategoryService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private CustomSequenceService customSequenceService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private WebhookDispatchService webhookDispatchService;
    @Mock
    private CustomFieldValueService customFieldValueService;
    @Mock
    private IntercomService intercomService;
    @Mock
    private ReviewEligibilityService reviewEligibilityService;
    @Mock
    private BrandingService brandingService;
    @Mock
    private Utils utils;
    @Mock
    private WorkOrderHistoryService workOrderHistoryService;
    @Mock
    private org.thymeleaf.spring5.SpringTemplateEngine thymeleafTemplateEngine;
    @Mock
    private com.grash.factory.StorageServiceFactory storageServiceFactory;
    @Mock
    private Environment environment;
    @Mock
    private ResourceBundleMessageSource emailMessageSource;
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

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(String methodName, Object... args) throws Exception {
        Method[] methods = workOrderService.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                try {
                    return (T) m.invoke(workOrderService, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception ex) throw ex;
                    throw e;
                }
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private Collection<WOField> invokeDetectChangedFields(WorkOrder original, WorkOrder updated) throws Exception {
        return invokePrivate("detectChangedFieldsFromEntity", original, updated);
    }

    private Collection<WOField> invokeDetectPatchDTO(WorkOrder original, WorkOrderPatchDTO patch) throws Exception {
        return invokePrivate("detectPatchDTOChangedFields", original, patch);
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
        void assetChanged() throws Exception {
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
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSET));
        }

        @Test
        void assetUnchanged() throws Exception {
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
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSET));
        }

        @Test
        void assetNullOnPatch() throws Exception {
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
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSET));
        }

        @Test
        void assetNullOnBothSides() throws Exception {
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
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSET));
        }

        @Test
        void assigneesChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L), buildUser(21L))));
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesNullOnPatchNullOnOriginal() throws Exception {
            original.setAssignedTo(null);
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(null);
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertFalse(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesNullOnOneSide() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setAssignedTo(null);
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.ASSIGNEES));
        }

        @Test
        void categoryChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCategory(buildCategory(99L, "New"));
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
            assertTrue(result.contains(WOField.CATEGORY));
        }

        @Test
        void categoryUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCategory(buildCategory(30L, "Cat"));
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.CATEGORY));
        }

        @Test
        void categoryNullOnOneSide() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCategory(null);
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.CATEGORY));
        }

        @Test
        void descriptionChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDescription("new desc");
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.DESCRIPTION));
        }

        @Test
        void descriptionUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDescription("original desc");
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.DESCRIPTION));
        }

        @Test
        void descriptionNullOnOneSide() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDescription(null);
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.DESCRIPTION));
        }

        @Test
        void dueDateChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDueDate(new Date(2000));
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.DUE_DATE));
        }

        @Test
        void dueDateUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setDueDate(new Date(1000));
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.DUE_DATE));
        }

        @Test
        void estimatedDurationChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setEstimatedDuration(5.0);
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void estimatedDurationUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setEstimatedDuration(2.0);
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void locationChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setLocation(buildLocation(99L));
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.LOCATION));
        }

        @Test
        void locationUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setLocation(buildLocation(40L));
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.LOCATION));
        }

        @Test
        void locationNullOnBothSides() throws Exception {
            original.setLocation(null);
            WorkOrderPatchDTO patch = basePatch();
            patch.setLocation(null);
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.LOCATION));
        }

        @Test
        void priorityChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setPriority(Priority.HIGH);
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.PRIORITY));
        }

        @Test
        void priorityUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setPriority(Priority.LOW);
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.PRIORITY));
        }

        @Test
        void titleChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTitle("new title");
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.TITLE));
        }

        @Test
        void titleUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTitle("original title");
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.TITLE));
        }

        @Test
        void teamChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTeam(buildTeam(99L));
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.TEAM));
        }

        @Test
        void teamUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setTeam(buildTeam(50L));
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.TEAM));
        }

        @Test
        void teamNullOnBothSides() throws Exception {
            original.setTeam(null);
            WorkOrderPatchDTO patch = basePatch();
            patch.setTeam(null);
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.TEAM));
        }

        @Test
        void customersChanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L), buildCustomer(61L))));
            assertTrue(invokeDetectPatchDTO(original, patch).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersUnchanged() throws Exception {
            WorkOrderPatchDTO patch = basePatch();
            patch.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersDifferentOrderSameElements() throws Exception {
            Customer c1 = buildCustomer(60L);
            Customer c2 = buildCustomer(61L);
            original.setCustomers(new ArrayList<>(List.of(c1, c2)));
            WorkOrderPatchDTO patch = basePatch();
            patch.setCustomers(new ArrayList<>(List.of(c2, c1)));
            assertFalse(invokeDetectPatchDTO(original, patch).contains(WOField.CUSTOMERS));
        }

        @Test
        void multipleFieldsChanged() throws Exception {
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
            Collection<WOField> result = invokeDetectPatchDTO(original, patch);
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
        void assetChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(buildAsset(10L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(buildAsset(99L));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assetUnchanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(buildAsset(10L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(buildAsset(10L));
            assertFalse(invokeDetectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assetNullOnOneSide() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(buildAsset(10L));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assetNullOnBothSides() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setAsset(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAsset(null);
            assertFalse(invokeDetectChangedFields(original, updated).contains(WOField.ASSET));
        }

        @Test
        void assigneesChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAssignedTo(new ArrayList<>(List.of(buildUser(20L), buildUser(21L))));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.ASSIGNEES));
        }

        @Test
        void assigneesUnchanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setAssignedTo(new ArrayList<>(List.of(buildUser(20L))));
            assertFalse(invokeDetectChangedFields(original, updated).contains(WOField.ASSIGNEES));
        }

        @Test
        void categoryChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setCategory(buildCategory(30L, "A"));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCategory(buildCategory(31L, "B"));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.CATEGORY));
        }

        @Test
        void categoryNullOnOneSide() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setCategory(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCategory(buildCategory(30L, "A"));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.CATEGORY));
        }

        @Test
        void descriptionChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setDescription("old");
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDescription("new");
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.DESCRIPTION));
        }

        @Test
        void dueDateChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setDueDate(new Date(1000));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setDueDate(new Date(2000));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.DUE_DATE));
        }

        @Test
        void estimatedDurationChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setEstimatedDuration(1.0);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setEstimatedDuration(2.0);
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.ESTIMATED_DURATION));
        }

        @Test
        void locationChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setLocation(buildLocation(40L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setLocation(buildLocation(41L));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.LOCATION));
        }

        @Test
        void priorityChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setPriority(Priority.LOW);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setPriority(Priority.HIGH);
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.PRIORITY));
        }

        @Test
        void titleChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setTitle("old");
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTitle("new");
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.TITLE));
        }

        @Test
        void teamChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setTeam(buildTeam(50L));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setTeam(buildTeam(51L));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.TEAM));
        }

        @Test
        void statusChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setStatus(Status.OPEN);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setStatus(Status.COMPLETE);
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.STATUS));
        }

        @Test
        void statusUnchanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setStatus(Status.OPEN);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setStatus(Status.OPEN);
            assertFalse(invokeDetectChangedFields(original, updated).contains(WOField.STATUS));
        }

        @Test
        void customersChanged() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(new ArrayList<>(List.of(buildCustomer(60L))));
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(new ArrayList<>(List.of(buildCustomer(61L))));
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }

        @Test
        void customersNullOnOneSide() throws Exception {
            WorkOrder original = buildWorkOrder(1L);
            original.setCustomers(null);
            WorkOrder updated = buildWorkOrder(1L);
            updated.setCustomers(new ArrayList<>());
            assertTrue(invokeDetectChangedFields(original, updated).contains(WOField.CUSTOMERS));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // collectionsMatch
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CollectionsMatch {

        private final java.util.function.Function<User, Long> idExtractor = User::getId;

        @Test
        void bothNull() throws Exception {
            assertTrue((Boolean) invokePrivate("collectionsMatch", null, null, idExtractor));
        }

        @Test
        void firstNullSecondEmpty() throws Exception {
            assertFalse((Boolean) invokePrivate("collectionsMatch", null, new ArrayList<>(), idExtractor));
        }

        @Test
        void firstEmptySecondNull() throws Exception {
            assertFalse((Boolean) invokePrivate("collectionsMatch", new ArrayList<>(), null, idExtractor));
        }

        @Test
        void sameElementsDifferentOrder() throws Exception {
            User u1 = buildUser(1L);
            User u2 = buildUser(2L);
            List<User> a = List.of(u1, u2);
            List<User> b = List.of(u2, u1);
            assertTrue((Boolean) invokePrivate("collectionsMatch", a, b, idExtractor));
        }

        @Test
        void differentSizes() throws Exception {
            List<User> a = List.of(buildUser(1L));
            List<User> b = List.of(buildUser(1L), buildUser(2L));
            assertFalse((Boolean) invokePrivate("collectionsMatch", a, b, idExtractor));
        }

        @Test
        void sameSizeDifferentElements() throws Exception {
            List<User> a = List.of(buildUser(1L));
            List<User> b = List.of(buildUser(2L));
            assertFalse((Boolean) invokePrivate("collectionsMatch", a, b, idExtractor));
        }

        @Test
        void singleElementMatch() throws Exception {
            List<User> a = List.of(buildUser(1L));
            List<User> b = List.of(buildUser(1L));
            assertTrue((Boolean) invokePrivate("collectionsMatch", a, b, idExtractor));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // checkUsageBasedLimit
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    class CheckUsageBasedLimit {

        @Test
        void exactlyAtThreshold_throwsException() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(false);
            when(workOrderRepository.hasMoreActiveThan(eq(1L), eq(29L))).thenReturn(false);
            assertDoesNotThrow(() -> invokePrivate("checkUsageBasedLimit", company));
        }

        @Test
        void oneBelowThreshold_noException() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(false);
            when(workOrderRepository.hasMoreActiveThan(eq(1L), eq(29L))).thenReturn(false);
            assertDoesNotThrow(() -> invokePrivate("checkUsageBasedLimit", company));
        }

        @Test
        void oneAboveThreshold_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(false);
            when(workOrderRepository.hasMoreActiveThan(eq(1L), eq(29L))).thenReturn(true);
            CustomException ex = assertThrows(CustomException.class,
                    () -> invokePrivate("checkUsageBasedLimit", company));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void entitlementPresent_bypassesLimit() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)).thenReturn(true);
            assertDoesNotThrow(() -> invokePrivate("checkUsageBasedLimit", company));
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
            assertDoesNotThrow(() -> workOrderService.changeStatus(dto, 1L, user, "ios"));
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
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
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
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
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
            role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
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
}
