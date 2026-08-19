package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.ReportConfig;
import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.workOrder.WorkOrderSendReportDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.*;
import com.grash.service.TeamService;
import com.grash.service.WebhookDispatchService;
import com.grash.service.WorkOrderService;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.grash.utils.Consts.usageBasedFreeLimits;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@Transactional
class WorkOrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WorkOrderRepository workOrderRepository;
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
    private EntityManager em;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private GeneralPreferencesRepository generalPreferencesRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private TeamService teamService;
    @Autowired
    private LaborRepository laborRepository;
    @Autowired
    private AdditionalCostRepository additionalCostRepository;
    @Autowired
    private PartQuantityRepository partQuantityRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private WorkOrderCategoryRepository workOrderCategoryRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private PreventiveMaintenanceRepository preventiveMaintenanceRepository;

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
                .features(new HashSet<>(Arrays.asList(PlanFeatures.SIGNATURE, PlanFeatures.WEBHOOK)))
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
        gp.setDateFormat(com.grash.model.enums.DateFormat.MMDDYY);
        gp.setTimeZone("UTC");
        generalPreferencesRepository.save(gp);

        adminRole = Role.builder()
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .companySettings(settings)
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.WORK_ORDERS)))
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

    @Nested
    class CreateTests {

        private WorkOrderPostDTO buildPostDTO(String title) {
            WorkOrderPostDTO dto = new WorkOrderPostDTO();
            dto.setTitle(title);
            dto.setStatus(Status.OPEN);
            dto.setPriority(Priority.NONE);
            dto.setEstimatedDuration(1.0);
            dto.setAssignedTo(new ArrayList<>());
            dto.setCustomers(new ArrayList<>());
            dto.setFiles(new ArrayList<>());
            dto.setCustomFieldValues(new ArrayList<>());
            return dto;
        }

        @Test
        void create_sequentialCustomIds() {
            WorkOrderPostDTO dto1 = buildPostDTO("First WO");
            WorkOrderPostDTO dto2 = buildPostDTO("Second WO");

            WorkOrder result1 = workOrderService.create(dto1, company);
            WorkOrder result2 = workOrderService.create(dto2, company);

            assertNotNull(result1.getCustomId());
            assertNotNull(result2.getCustomId());
            assertNotEquals(result1.getCustomId(), result2.getCustomId());
            assertTrue(result1.getCustomId().startsWith("WO"));
            assertTrue(result2.getCustomId().startsWith("WO"));
        }

        @Test
        void create_dispatchesNewWorkOrderWebhook() {
            WorkOrderPostDTO dto = buildPostDTO("Webhook WO");

            workOrderService.create(dto, company);

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company),
                    eq(WebhookEvent.NEW_WORK_ORDER),
                    anyMap(),
                    eq("newWorkOrder"),
                    any(),
                    isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        void create_throwsForbiddenWhenUsageLimitExceeded() {
            for (int i = 0; i < usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS); i++) {
                WorkOrder wo = new WorkOrder();
                wo.setTitle("Active WO " + i);
                wo.setStatus(Status.OPEN);
                wo.setPriority(Priority.NONE);
                wo.setEstimatedDuration(1.0);
                wo.setCompany(company);
                wo.setCreatedBy(user.getId());
                wo.setAssignedTo(new ArrayList<>());
                wo.setCustomers(new ArrayList<>());
                wo.setFiles(new ArrayList<>());
                wo.setCustomFieldValues(new ArrayList<>());
                workOrderRepository.saveAndFlush(wo);
            }

            WorkOrderPostDTO dto = buildPostDTO("Should Fail WO");
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.create(dto, company));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class ChangeStatusTests {

        private WorkOrder createInitialWO() {
            WorkOrder wo = new WorkOrder();
            wo.setTitle("Status Change WO");
            wo.setDescription("Test detach/re-fetch");
            wo.setStatus(Status.OPEN);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void changeStatus_detachRefetchDoesNotLeakStaleState() {
            WorkOrder saved = createInitialWO();
            Long id = saved.getId();

            WorkOrderChangeStatusDTO dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(Status.COMPLETE);

            WorkOrder result = workOrderService.changeStatus(dto, id, user, "ios");

            assertNotNull(result);
            assertEquals(Status.COMPLETE, result.getStatus());
            assertNotNull(result.getCompletedBy());
            assertEquals(user.getId(), result.getCompletedBy().getId());
            assertNotNull(result.getCompletedOn());

            em.clear();
            WorkOrder fromDb = workOrderRepository.findById(id).get();
            assertEquals(Status.COMPLETE, fromDb.getStatus());
            assertEquals(user.getId(), fromDb.getCompletedBy().getId());
            assertNotNull(fromDb.getCompletedOn());
        }

        @Test
        void changeStatus_toInProgress_setsStatus() {
            WorkOrder saved = createInitialWO();

            WorkOrderChangeStatusDTO dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(Status.IN_PROGRESS);

            WorkOrder result = workOrderService.changeStatus(dto, saved.getId(), user, "ios");

            assertEquals(Status.IN_PROGRESS, result.getStatus());
            assertNull(result.getCompletedBy());
            assertNull(result.getCompletedOn());
        }

        @Test
        void changeStatus_toOnHold_setsStatus() {
            WorkOrder saved = createInitialWO();

            WorkOrderChangeStatusDTO dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(Status.ON_HOLD);

            WorkOrder result = workOrderService.changeStatus(dto, saved.getId(), user, "ios");

            assertEquals(Status.ON_HOLD, result.getStatus());
        }

        @Test
        void changeStatus_nullStatus_throwsNotAcceptable() {
            WorkOrder saved = createInitialWO();

            WorkOrderChangeStatusDTO dto = new WorkOrderChangeStatusDTO();
            dto.setStatus(null);

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.changeStatus(dto, saved.getId(), user, "ios"));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class SearchCriteriaTests {

        private WorkOrder createWO(String title, Status status, Long createdBy) {
            return createWO(title, status, createdBy, Priority.NONE);
        }

        private WorkOrder createWO(String title, Status status, Long createdBy, Priority priority) {
            WorkOrder wo = new WorkOrder();
            wo.setTitle(title);
            wo.setStatus(status);
            wo.setPriority(priority);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(createdBy);
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void searchByStatus_openOnly() {
            createWO("Open WO 1", Status.OPEN, user.getId());
            createWO("Open WO 2", Status.OPEN, user.getId());
            createWO("Complete WO", Status.COMPLETE, user.getId());

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("status")
                    .value(Status.OPEN)
                    .operation("eq")
                    .values(new ArrayList<>())
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            Page<WorkOrder> result = workOrderService.findBySearchCriteria(criteria);
            assertEquals(2, result.getTotalElements());
        }

        @Test
        void searchByTitle_contains() {
            createWO("Pump Maintenance", Status.OPEN, user.getId());
            createWO("HVAC Repair", Status.OPEN, user.getId());

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("title")
                    .value("Pump")
                    .operation("cn")
                    .values(new ArrayList<>())
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            Page<WorkOrder> result = workOrderService.findBySearchCriteria(criteria);
            assertEquals(1, result.getTotalElements());
            assertEquals("Pump Maintenance", result.getContent().get(0).getTitle());
        }

        @Test
        void searchByPriority_in() {
            createWO("High WO", Status.OPEN, user.getId(), Priority.HIGH);
            createWO("Low WO", Status.OPEN, user.getId(), Priority.LOW);
            createWO("None WO", Status.OPEN, user.getId(), Priority.NONE);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("priority")
                    .operation("in")
                    .values(new ArrayList<>(Arrays.asList(Priority.HIGH, Priority.LOW)))
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            Page<WorkOrder> result = workOrderService.findBySearchCriteria(criteria);
            assertEquals(2, result.getTotalElements());
        }

        @Test
        void getSearchCriteria_clientRole_filtersByCompany() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = workOrderService.getSearchCriteria(user, criteria);

            boolean hasCompanyFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "company".equals(f.getField()) && f.getValue().equals(company.getId()));
            assertTrue(hasCompanyFilter);
        }

        @Test
        void getSearchCriteria_canViewOthersFalse_addsCreatedByOrAssignedToFilter() {
            adminRole.getViewOtherPermissions().clear();
            roleRepository.save(adminRole);
            user.setRole(adminRole);
            userRepository.save(user);
            teamService.findByUser(user.getId());

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = workOrderService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()) && f.getValue().equals(user.getId()));
            assertTrue(hasCreatedByFilter);
        }

        @Test
        void getSearchCriteria_canViewOthersTrue_noCreatedByFilter() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = workOrderService.getSearchCriteria(user, criteria);

            boolean hasCreatedByFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()));
            assertFalse(hasCreatedByFilter);
        }

        @Test
        void getSearchCriteria_assignedToUserFilterIsStripped() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("assignedToUser")
                    .value(user.getId())
                    .operation("eq")
                    .values(new ArrayList<>())
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            workOrderService.getSearchCriteria(user, criteria);

            boolean hasAssignedToUser = criteria.getFilterFields().stream()
                    .anyMatch(f -> "assignedToUser".equals(f.getField()));
            assertFalse(hasAssignedToUser);
        }

        @Test
        void search_combinedFilters() {
            createWO("Pump A", Status.OPEN, user.getId());
            createWO("Pump B", Status.COMPLETE, user.getId());
            createWO("HVAC A", Status.OPEN, user.getId());

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("title")
                    .value("Pump")
                    .operation("cn")
                    .values(new ArrayList<>())
                    .build());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("status")
                    .value(Status.OPEN)
                    .operation("eq")
                    .values(new ArrayList<>())
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            Page<WorkOrder> result = workOrderService.findBySearchCriteria(criteria);
            assertEquals(1, result.getTotalElements());
            assertEquals("Pump A", result.getContent().get(0).getTitle());
        }
    }

    @Nested
    class SendReportTests {

        private WorkOrder createReportableWO() {
            WorkOrder wo = new WorkOrder();
            wo.setTitle("Report WO");
            wo.setDescription("Test send report");
            wo.setStatus(Status.COMPLETE);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setCompletedBy(user);
            wo.setCompletedOn(new Date());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void sendReport_noCustomerEmails_throwsBadRequest() {
            WorkOrder wo = createReportableWO();

            Customer customer = new Customer();
            customer.setName("NoEmail Customer");
            customer.setCompany(company);
            Customer savedCustomer = customerRepository.save(customer);

            WorkOrderSendReportDTO request = new WorkOrderSendReportDTO();
            request.setCustomers(List.of(savedCustomer));
            request.setMessage("Please review");

            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.sendReport(wo.getId(), request, user));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        }

        @Test
        void sendReport_validEmails_invokesMailService() {
            WorkOrder wo = createReportableWO();

            Customer customer = new Customer();
            customer.setName("Email Customer");
            customer.setEmail("customer@test.com");
            customer.setCompany(company);
            Customer savedCustomer = customerRepository.save(customer);

            WorkOrderSendReportDTO request = new WorkOrderSendReportDTO();
            request.setCustomers(List.of(savedCustomer));
            request.setMessage("Review this report");

            workOrderService.sendReport(wo.getId(), request, user);

            verify(mailService).sendMessageUsingThymeleafTemplate(
                    argThat(recipients -> {
                        Set<String> set = new HashSet<>(Arrays.asList(recipients));
                        return set.contains("customer@test.com") && set.contains(user.getEmail());
                    }),
                    anyString(),
                    anyMap(),
                    eq("work-order-report-email.html"),
                    eq(Helper.getLocale(user)),
                    argThat(attachments -> attachments != null && !attachments.isEmpty())
            );
        }

        @Test
        void sendReport_workOrderNotFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.sendReport(99999L, new WorkOrderSendReportDTO(), user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void sendReport_pdfBytesNonEmpty() {
            WorkOrder wo = createReportableWO();

            Customer customer = new Customer();
            customer.setName("PDF Customer");
            customer.setEmail("pdf@test.com");
            customer.setCompany(company);
            Customer savedCustomer = customerRepository.save(customer);

            WorkOrderSendReportDTO request = new WorkOrderSendReportDTO();
            request.setCustomers(List.of(savedCustomer));
            request.setConfig(new ReportConfig());

            workOrderService.sendReport(wo.getId(), request, user);

            verify(mailService).sendMessageUsingThymeleafTemplate(
                    any(),
                    anyString(),
                    anyMap(),
                    anyString(),
                    any(Locale.class),
                    argThat(attachments -> {
                        if (attachments == null || attachments.isEmpty()) return false;
                        byte[] data = attachments.get(0).getAttachmentData();
                        return data != null && data.length > 0;
                    })
            );
        }
    }

    @Nested
    class DeleteTests {

        private WorkOrder createDeletableWO() {
            WorkOrder wo = new WorkOrder();
            wo.setTitle("Delete Me");
            wo.setStatus(Status.OPEN);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void deleteByIdAndUser_removesFromDB() {
            WorkOrder wo = createDeletableWO();
            Long id = wo.getId();

            workOrderService.deleteByIdAndUser(id, user);

            em.flush();
            em.clear();
            assertFalse(workOrderRepository.findById(id).isPresent());
        }

        @Test
        void deleteByIdAndUser_cascadesLabor() {
            WorkOrder wo = createDeletableWO();

            Labor labor = new Labor();
            labor.setCompany(company);
            labor.setWorkOrder(wo);
            labor.setStartedAt(new Date());
            labor.setDuration(3600);
            labor.setHourlyRate(50);
            laborRepository.saveAndFlush(labor);

            Long woId = wo.getId();
            workOrderService.deleteByIdAndUser(woId, user);

            em.flush();
            em.clear();
            assertTrue(laborRepository.findByWorkOrder_Id(woId).isEmpty());
        }

        @Test
        void deleteByIdAndUser_cascadesAdditionalCost() {
            WorkOrder wo = createDeletableWO();

            AdditionalCost cost = new AdditionalCost();
            cost.setWorkOrder(wo);
            cost.setCost(100.0);
            cost.setDescription("Test cost");
            additionalCostRepository.saveAndFlush(cost);

            Long woId = wo.getId();
            workOrderService.deleteByIdAndUser(woId, user);

            em.flush();
            em.clear();
            assertTrue(additionalCostRepository.findByWorkOrder_Id(woId).isEmpty());
        }

        @Test
        void deleteByIdAndUser_sendsEmailToSettingsAdmins() {
            adminRole.getViewPermissions().add(PermissionEntity.SETTINGS);
            roleRepository.save(adminRole);
            user.setRole(adminRole);
            userRepository.save(user);

            WorkOrder wo = createDeletableWO();

            workOrderService.deleteByIdAndUser(wo.getId(), user);

            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{user.getEmail()}),
                    anyString(),
                    anyMap(),
                    eq("deleted-work-order.html"),
                    any(java.util.Locale.class),
                    isNull());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> workOrderService.deleteByIdAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class FileTests {

        private WorkOrder createFileWO() {
            WorkOrder wo = new WorkOrder();
            wo.setTitle("File WO");
            wo.setStatus(Status.OPEN);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void addFiles_persistsAndReturns() {
            WorkOrder wo = createFileWO();
            em.clear();

            com.grash.model.File file = new com.grash.model.File();
            file.setName("doc.pdf");
            file.setPath("/uploads/doc.pdf");
            file.setCompany(company);
            em.persist(file);
            em.flush();

            workOrderService.addFiles(wo.getId(), List.of(file), user);

            em.flush();
            em.clear();
            WorkOrder fromDb = workOrderRepository.findById(wo.getId()).get();
            assertEquals(1, fromDb.getFiles().size());
            assertEquals("doc.pdf", fromDb.getFiles().get(0).getName());
        }

        @Test
        void addFiles_notFound_throwsNotFound() {
            com.grash.model.File file = new com.grash.model.File();
            file.setName("x.txt");
            file.setPath("/x.txt");
            file.setCompany(company);
            em.persist(file);
            em.flush();

            assertThrows(CustomException.class,
                    () -> workOrderService.addFiles(99999L, List.of(file), user));
        }

        @Test
        void removeFile_persistsRemoval() {
            WorkOrder wo = createFileWO();

            com.grash.model.File file = new com.grash.model.File();
            file.setName("remove-me.txt");
            file.setPath("/remove-me.txt");
            file.setCompany(company);
            em.persist(file);
            em.flush();

            WorkOrder refetched = workOrderRepository.findById(wo.getId()).get();
            refetched.getFiles().add(file);
            workOrderRepository.saveAndFlush(refetched);
            em.clear();

            WorkOrder saved = workOrderRepository.findById(wo.getId()).get();
            assertEquals(1, saved.getFiles().size());
            Long fileId = saved.getFiles().get(0).getId();

            workOrderService.removeFile(wo.getId(), fileId, user);

            em.flush();
            em.clear();
            WorkOrder fromDb = workOrderRepository.findById(wo.getId()).get();
            assertTrue(fromDb.getFiles().isEmpty());
        }

        @Test
        void addFiles_multipleFiles_persistsAll() {
            WorkOrder wo = createFileWO();
            em.clear();

            com.grash.model.File f1 = new com.grash.model.File();
            f1.setName("a.txt");
            f1.setPath("/a.txt");
            f1.setCompany(company);
            em.persist(f1);

            com.grash.model.File f2 = new com.grash.model.File();
            f2.setName("b.txt");
            f2.setPath("/b.txt");
            f2.setCompany(company);
            em.persist(f2);
            em.flush();

            workOrderService.addFiles(wo.getId(), List.of(f1, f2), user);

            em.flush();
            em.clear();
            WorkOrder fromDb = workOrderRepository.findById(wo.getId()).get();
            assertEquals(2, fromDb.getFiles().size());
        }
    }

    @Nested
    class CostComputationTests {

        private WorkOrder createCostWO() {
            WorkOrder wo = new WorkOrder();
            wo.setTitle("Cost WO");
            wo.setStatus(Status.OPEN);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void getLaborCostAndTime_withRealData() {
            WorkOrder wo = createCostWO();

            Labor l1 = new Labor();
            l1.setCompany(company);
            l1.setWorkOrder(wo);
            l1.setStartedAt(new Date());
            l1.setDuration(3600);
            l1.setHourlyRate(50);
            laborRepository.saveAndFlush(l1);

            Labor l2 = new Labor();
            l2.setCompany(company);
            l2.setWorkOrder(wo);
            l2.setStartedAt(new Date());
            l2.setDuration(7200);
            l2.setHourlyRate(25);
            laborRepository.saveAndFlush(l2);

            em.clear();
            Collection<WorkOrder> wos = List.of(workOrderRepository.findById(wo.getId()).get());
            var result = workOrderService.getLaborCostAndTime(wos);

            assertEquals(50 * 1 + 25 * 2, result.getFirst());
            assertEquals(3600 + 7200, result.getSecond());
        }

        @Test
        void getAdditionalCost_withRealData() {
            WorkOrder wo = createCostWO();

            AdditionalCost ac1 = new AdditionalCost();
            ac1.setWorkOrder(wo);
            ac1.setCost(150.50);
            additionalCostRepository.saveAndFlush(ac1);

            AdditionalCost ac2 = new AdditionalCost();
            ac2.setWorkOrder(wo);
            ac2.setCost(49.50);
            additionalCostRepository.saveAndFlush(ac2);

            em.clear();
            Collection<WorkOrder> wos = List.of(workOrderRepository.findById(wo.getId()).get());
            double total = workOrderService.getAdditionalCost(wos);

            assertEquals(200.0, total, 0.001);
        }

        @Test
        void getPartCost_withRealData() {
            WorkOrder wo = createCostWO();

            Part part = new Part();
            part.setName("Bearing");
            part.setCost(25.0);
            part.setCompany(company);
            part.setQuantity(100);
            part.setMinQuantity(10);
            part.setFiles(new ArrayList<>());
            part.setAssignedTo(new ArrayList<>());
            part.setCustomers(new ArrayList<>());
            part.setVendors(new ArrayList<>());
            part.setTeams(new ArrayList<>());
            part.setAssets(new ArrayList<>());
            part.setCustomFieldValues(new ArrayList<>());
            part = partRepository.saveAndFlush(part);

            PartQuantity pq = new PartQuantity();
            pq.setPart(part);
            pq.setWorkOrder(wo);
            pq.setQuantity(4);
            pq.setCompany(company);
            partQuantityRepository.saveAndFlush(pq);

            em.clear();
            Collection<WorkOrder> wos = List.of(workOrderRepository.findById(wo.getId()).get());
            double total = workOrderService.getPartCost(wos);

            assertEquals(100.0, total, 0.001);
        }

        @Test
        void getAllCost_includesLabor() {
            WorkOrder wo = createCostWO();

            Labor l = new Labor();
            l.setCompany(company);
            l.setWorkOrder(wo);
            l.setStartedAt(new Date());
            l.setDuration(3600);
            l.setHourlyRate(100);
            laborRepository.saveAndFlush(l);

            AdditionalCost ac = new AdditionalCost();
            ac.setWorkOrder(wo);
            ac.setCost(50.0);
            additionalCostRepository.saveAndFlush(ac);

            em.clear();
            Collection<WorkOrder> wos = List.of(workOrderRepository.findById(wo.getId()).get());
            double total = workOrderService.getAllCost(wos, true);

            assertEquals(150.0, total, 0.001);
        }

        @Test
        void getAllCost_excludesLabor() {
            WorkOrder wo = createCostWO();

            Labor l = new Labor();
            l.setCompany(company);
            l.setWorkOrder(wo);
            l.setStartedAt(new Date());
            l.setDuration(3600);
            l.setHourlyRate(100);
            laborRepository.saveAndFlush(l);

            em.clear();
            Collection<WorkOrder> wos = List.of(workOrderRepository.findById(wo.getId()).get());
            double total = workOrderService.getAllCost(wos, false);

            assertEquals(0.0, total, 0.001);
        }
    }

    @Nested
    class QueryTests {

        private WorkOrder createQueryWO(String title, Status status) {
            WorkOrder wo = new WorkOrder();
            wo.setTitle(title);
            wo.setStatus(status);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            return workOrderRepository.saveAndFlush(wo);
        }

        @Test
        void findByCompany_returnsAllForCompany() {
            createQueryWO("WO A", Status.OPEN);
            createQueryWO("WO B", Status.COMPLETE);

            Collection<WorkOrder> result = workOrderService.findByCompany(company.getId());

            assertTrue(result.size() >= 2);
            assertTrue(result.stream().allMatch(wo -> wo.getCompany().getId().equals(company.getId())));
        }

        @Test
        void findByCompanyWithTimeAndCost_onlyWorkOrdersWithRecords() {
            WorkOrder withLabor = createQueryWO("With Labor", Status.OPEN);

            Labor labor = new Labor();
            labor.setCompany(company);
            labor.setWorkOrder(withLabor);
            labor.setStartedAt(new Date());
            labor.setDuration(1800);
            labor.setHourlyRate(40);
            laborRepository.saveAndFlush(labor);

            createQueryWO("No Records", Status.OPEN);

            em.clear();
            Page<WorkOrder> result = workOrderService.findByCompanyWithTimeAndCost(
                    company.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

            assertTrue(result.getContent().stream()
                    .anyMatch(wo -> wo.getId().equals(withLabor.getId())));
        }

        @Test
        void findByDueDateBetweenAndCompany_filtersCorrectly() {
            WorkOrder wo1 = createQueryWO("Due Soon", Status.OPEN);
            Date now = new Date();
            Date tomorrow = new Date(now.getTime() + TimeUnit.DAYS.toMillis(1));
            wo1.setDueDate(tomorrow);
            workOrderRepository.saveAndFlush(wo1);

            WorkOrder wo2 = createQueryWO("Due Far", Status.OPEN);
            Date nextMonth = new Date(now.getTime() + TimeUnit.DAYS.toMillis(60));
            wo2.setDueDate(nextMonth);
            workOrderRepository.saveAndFlush(wo2);

            em.clear();
            Collection<WorkOrder> result = workOrderService.findByDueDateBetweenAndCompany(
                    now, new Date(now.getTime() + TimeUnit.DAYS.toMillis(2)), company.getId());

            assertTrue(result.stream().anyMatch(wo -> wo.getId().equals(wo1.getId())));
            assertFalse(result.stream().anyMatch(wo -> wo.getId().equals(wo2.getId())));
        }

        @Test
        void findByCompletedOnBetweenAndCompany_filtersCorrectly() {
            WorkOrder wo1 = createQueryWO("Completed Recently", Status.COMPLETE);
            wo1.setCompletedOn(new Date());
            wo1.setCompletedBy(user);
            workOrderRepository.saveAndFlush(wo1);

            WorkOrder wo2 = createQueryWO("Old Complete", Status.COMPLETE);
            Date longAgo = new Date(0);
            wo2.setCompletedOn(longAgo);
            wo2.setCompletedBy(user);
            workOrderRepository.saveAndFlush(wo2);

            em.clear();
            Date now = new Date();
            Date start = new Date(now.getTime() - TimeUnit.DAYS.toMillis(1));
            Collection<WorkOrder> result = workOrderService.findByCompletedOnBetweenAndCompany(
                    start, now, company.getId());

            assertTrue(result.stream().anyMatch(wo -> wo.getId().equals(wo1.getId())));
            assertFalse(result.stream().anyMatch(wo -> wo.getId().equals(wo2.getId())));
        }

        @Test
        void findByCreatedBy_returnsCorrectUser() {
            createQueryWO("My WO", Status.OPEN);

            Collection<WorkOrder> result = workOrderService.findByCreatedBy(user.getId());

            assertTrue(result.stream().allMatch(wo -> user.getId().equals(wo.getCreatedBy())));
        }

        @Test
        void findByIdAndCompany_scopedToCompany() {
            WorkOrder wo = createQueryWO("Scoped WO", Status.OPEN);

            Optional<WorkOrder> found = workOrderService.findByIdAndCompany(
                    wo.getId(), company.getId());
            assertTrue(found.isPresent());

            Optional<WorkOrder> notFound = workOrderService.findByIdAndCompany(
                    wo.getId(), 99999L);
            assertFalse(notFound.isPresent());
        }

        @Test
        void findByIdsAndCompany_multipleIds() {
            WorkOrder wo1 = createQueryWO("Batch 1", Status.OPEN);
            WorkOrder wo2 = createQueryWO("Batch 2", Status.OPEN);

            Collection<WorkOrder> result = workOrderService.findByIdsAndCompany(
                    List.of(wo1.getId(), wo2.getId()), company.getId());

            assertEquals(2, result.size());
        }

        @Test
        void getWorkOrdersByPart_withPartQuantities() {
            WorkOrder wo = createQueryWO("Part WO", Status.OPEN);

            Part part = new Part();
            part.setName("Filter");
            part.setCost(15.0);
            part.setCompany(company);
            part.setQuantity(50);
            part.setMinQuantity(5);
            part.setFiles(new ArrayList<>());
            part.setAssignedTo(new ArrayList<>());
            part.setCustomers(new ArrayList<>());
            part.setVendors(new ArrayList<>());
            part.setTeams(new ArrayList<>());
            part.setAssets(new ArrayList<>());
            part.setCustomFieldValues(new ArrayList<>());
            part = partRepository.saveAndFlush(part);

            PartQuantity pq = new PartQuantity();
            pq.setPart(part);
            pq.setWorkOrder(wo);
            pq.setQuantity(2);
            pq.setCompany(company);
            partQuantityRepository.saveAndFlush(pq);

            em.clear();
            Collection<WorkOrder> result = workOrderService.getWorkOrdersByPart(part.getId());

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void getWorkOrdersByPart_noQuantities_returnsEmpty() {
            Part part = new Part();
            part.setName("Unused Part");
            part.setCost(10.0);
            part.setCompany(company);
            part.setQuantity(0);
            part.setMinQuantity(0);
            part.setFiles(new ArrayList<>());
            part.setAssignedTo(new ArrayList<>());
            part.setCustomers(new ArrayList<>());
            part.setVendors(new ArrayList<>());
            part.setTeams(new ArrayList<>());
            part.setAssets(new ArrayList<>());
            part.setCustomFieldValues(new ArrayList<>());
            part = partRepository.saveAndFlush(part);

            em.clear();
            Collection<WorkOrder> result = workOrderService.getWorkOrdersByPart(part.getId());

            assertTrue(result.isEmpty());
        }

        @Test
        void findByCompanyForExport_eagerlyFetchesRelations() {
            Asset asset = new Asset();
            asset.setName("Pump");
            asset.setCompany(company);
            asset.setStatus(AssetStatus.OPERATIONAL);
            asset.setFiles(new ArrayList<>());
            asset.setCustomers(new ArrayList<>());
            asset.setTeams(new ArrayList<>());
            asset.setParts(new ArrayList<>());
            asset.setCustomFieldValues(new ArrayList<>());
            asset = assetRepository.saveAndFlush(asset);

            Location location = new Location();
            location.setName("Building A");
            location.setCompany(company);
            location = locationRepository.saveAndFlush(location);

            WorkOrder wo = new WorkOrder();
            wo.setTitle("Export WO");
            wo.setStatus(Status.COMPLETE);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAsset(asset);
            wo.setLocation(location);
            wo.setCompletedBy(user);
            wo.setCompletedOn(new Date());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Page<WorkOrder> result = workOrderService.findByCompanyForExport(
                    company.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

            assertFalse(result.isEmpty());
            WorkOrder fetched = result.getContent().get(0);
            assertNotNull(fetched.getAsset());
            assertNotNull(fetched.getLocation());
        }

        @Test
        void findByAssetAndCreatedAtBetween_filtersCorrectly() {
            Asset asset = new Asset();
            asset.setName("Filter Asset");
            asset.setCompany(company);
            asset.setStatus(AssetStatus.OPERATIONAL);
            asset.setFiles(new ArrayList<>());
            asset.setCustomers(new ArrayList<>());
            asset.setTeams(new ArrayList<>());
            asset.setParts(new ArrayList<>());
            asset.setCustomFieldValues(new ArrayList<>());
            asset = assetRepository.saveAndFlush(asset);

            WorkOrder wo1 = createQueryWO("Asset WO", Status.OPEN);
            wo1.setAsset(asset);
            workOrderRepository.saveAndFlush(wo1);

            WorkOrder wo2 = createQueryWO("Other WO", Status.OPEN);
            workOrderRepository.saveAndFlush(wo2);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);
            Collection<WorkOrder> result = workOrderService.findByAssetAndCreatedAtBetween(
                    asset.getId(), start, end);

            assertTrue(result.stream().anyMatch(wo -> wo.getId().equals(wo1.getId())));
            assertFalse(result.stream().anyMatch(wo -> wo.getId().equals(wo2.getId())));
        }

        @Test
        void findByLocation_filtersCorrectly() {
            Location location = new Location();
            location.setName("Test Location");
            location.setCompany(company);
            location = locationRepository.saveAndFlush(location);

            WorkOrder wo = createQueryWO("Located WO", Status.OPEN);
            wo.setLocation(location);
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Collection<WorkOrder> result = workOrderService.findByLocation(location.getId());

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByAssignedToUser_returnsOrdersForPrimaryUser() {
            WorkOrder wo = createQueryWO("Assigned WO", Status.OPEN);
            wo.setPrimaryUser(user);
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Collection<WorkOrder> result = workOrderService.findByAssignedToUser(user.getId());

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByAssignedToUser_returnsOrdersViaTeam() {
            Team team = new Team();
            team.setName("Test Team");
            team.setCompany(company);
            team.setUsers(new ArrayList<>());
            team.getUsers().add(user);
            team = teamRepository.saveAndFlush(team);

            WorkOrder wo = createQueryWO("Team WO", Status.OPEN);
            wo.setTeam(team);
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Collection<WorkOrder> result = workOrderService.findByAssignedToUser(user.getId());

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByAssignedToUserAndCreatedAtBetween_filtersByUserAndDate() {
            WorkOrder wo = createQueryWO("Date Assigned WO", Status.OPEN);
            wo.setPrimaryUser(user);
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);
            Collection<WorkOrder> result = workOrderService.findByAssignedToUserAndCreatedAtBetween(
                    user.getId(), start, end);

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByPriorityAndCompanyAndCreatedAtBetween_filtersCorrectly() {
            WorkOrder wo = createQueryWO("High Priority", Status.OPEN);
            wo.setPriority(Priority.HIGH);
            workOrderRepository.saveAndFlush(wo);

            WorkOrder wo2 = createQueryWO("Low Priority", Status.OPEN);
            wo2.setPriority(Priority.LOW);
            workOrderRepository.saveAndFlush(wo2);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);
            Collection<WorkOrder> result = workOrderService.findByPriorityAndCompanyAndCreatedAtBetween(
                    Priority.HIGH, company.getId(), start, end);

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
            assertFalse(result.stream().anyMatch(w -> w.getId().equals(wo2.getId())));
        }

        @Test
        void findByCategoryAndCreatedAtBetween_filtersCorrectly() {
            WorkOrderCategory category = new WorkOrderCategory();
            category.setName("Electrical");
            category.setCompanySettings(company.getCompanySettings());
            category = workOrderCategoryRepository.saveAndFlush(category);

            WorkOrder wo = createQueryWO("Category WO", Status.OPEN);
            wo.setCategory(category);
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);
            Collection<WorkOrder> result = workOrderService.findByCategoryAndCreatedAtBetween(
                    category.getId(), start, end);

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByCreatedByAndCreatedAtBetween_filtersCorrectly() {
            WorkOrder wo = createQueryWO("Created by me", Status.OPEN);
            wo.setCreatedBy(user.getId());
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);
            Collection<WorkOrder> result = workOrderService.findByCreatedByAndCreatedAtBetween(
                    user.getId(), start, end);

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByCompletedByAndCreatedAtBetween_filtersCorrectly() {
            WorkOrder wo = createQueryWO("Done WO", Status.OPEN);
            wo.setCompletedBy(user);
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);
            Collection<WorkOrder> result = workOrderService.findByCompletedByAndCreatedAtBetween(
                    user.getId(), start, end);

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findUnscheduledByCompany_returnsUnscheduled() {
            WorkOrder scheduled = createQueryWO("Scheduled", Status.OPEN);
            scheduled.setEstimatedStartDate(new Date());
            scheduled.setEstimatedDuration(1.0);
            scheduled.setPrimaryUser(user);
            workOrderRepository.saveAndFlush(scheduled);

            WorkOrder unscheduled = createQueryWO("Unscheduled", Status.OPEN);
            unscheduled.setEstimatedDuration(1.0);
            unscheduled.setPrimaryUser(user);
            unscheduled.setEstimatedStartDate(null);
            workOrderRepository.saveAndFlush(unscheduled);

            em.clear();
            Collection<WorkOrder> result = workOrderRepository.findUnscheduledByCompany(company.getId());

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(unscheduled.getId())));
            assertFalse(result.stream().anyMatch(w -> w.getId().equals(scheduled.getId())));
        }

        @Test
        void findByCompanyWithTimeAndCost_queriesCorrectly() {
            WorkOrder wo = createQueryWO("Cost WO", Status.OPEN);

            Labor labor = new Labor();
            labor.setWorkOrder(wo);
            labor.setCompany(company);
            labor.setHourlyRate(5000L);
            labor.setStartedAt(new Date());
            labor.setDuration(7200L);
            laborRepository.saveAndFlush(labor);

            em.clear();
            Page<WorkOrder> result = workOrderService.findByCompanyWithTimeAndCost(
                    company.getId(), PageRequest.of(0, 10));

            assertTrue(result.getContent().stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void countUrgent_returnsCorrectCount() {
            WorkOrder urgent = createQueryWO("Urgent", Status.OPEN);
            urgent.setDueDate(Helper.addSeconds(new Date(), 3600));
            workOrderRepository.saveAndFlush(urgent);

            em.clear();
            Integer count = workOrderService.countUrgent(user);

            assertNotNull(count);
            assertTrue(count > 0);
        }

        @Test
        void findLastByPM_returnsLatest() {
            PreventiveMaintenance pm = new PreventiveMaintenance();
            pm.setName("Test PM");
            pm.setTitle("PM Title");
            pm.setCustomFieldValues(new ArrayList<>());
            pm = preventiveMaintenanceRepository.saveAndFlush(pm);

            WorkOrder oldWo = createQueryWO("Old PM WO", Status.OPEN);
            oldWo.setParentPreventiveMaintenance(pm);
            workOrderRepository.saveAndFlush(oldWo);

            WorkOrder newWo = createQueryWO("New PM WO", Status.OPEN);
            newWo.setParentPreventiveMaintenance(pm);
            workOrderRepository.saveAndFlush(newWo);

            em.clear();
            Page<WorkOrder> result = workOrderService.findLastByPM(pm.getId(), 10);

            assertEquals(2, result.getTotalElements());
        }

        @Test
        void findByCompanyAndCreatedAtBetween_filtersByCompanyAndDate() {
            WorkOrder wo = createQueryWO("Date Filter WO", Status.OPEN);

            em.flush();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            Collection<WorkOrder> result = workOrderService.findByCompanyAndCreatedAtBetween(
                    company.getId(), start, end);

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(wo.getId())));
        }

        @Test
        void findByCompanyAndCreatedAtBetween_wrongCompany_returnsEmpty() {
            createQueryWO("Wrong Co WO", Status.OPEN);

            em.flush();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            Collection<WorkOrder> result = workOrderService.findByCompanyAndCreatedAtBetween(
                    99999L, start, end);

            assertTrue(result.isEmpty());
        }

        @Test
        void findTopNAssetsByIncompleteWO_returnsRankedByCount() {
            Asset asset1 = new Asset();
            asset1.setName("HighIncidents");
            asset1.setCompany(company);
            asset1.setStatus(AssetStatus.OPERATIONAL);
            asset1.setFiles(new ArrayList<>());
            asset1.setCustomers(new ArrayList<>());
            asset1.setTeams(new ArrayList<>());
            asset1.setParts(new ArrayList<>());
            asset1.setCustomFieldValues(new ArrayList<>());
            asset1 = assetRepository.saveAndFlush(asset1);

            Asset asset2 = new Asset();
            asset2.setName("LowIncidents");
            asset2.setCompany(company);
            asset2.setStatus(AssetStatus.OPERATIONAL);
            asset2.setFiles(new ArrayList<>());
            asset2.setCustomers(new ArrayList<>());
            asset2.setTeams(new ArrayList<>());
            asset2.setParts(new ArrayList<>());
            asset2.setCustomFieldValues(new ArrayList<>());
            asset2 = assetRepository.saveAndFlush(asset2);

            for (int i = 0; i < 3; i++) {
                WorkOrder wo = createQueryWO("Inc WO " + i, Status.OPEN);
                wo.setAsset(asset1);
                workOrderRepository.saveAndFlush(wo);
            }

            WorkOrder woOther = createQueryWO("Other Inc", Status.IN_PROGRESS);
            woOther.setAsset(asset2);
            workOrderRepository.saveAndFlush(woOther);

            WorkOrder complete = createQueryWO("Complete", Status.COMPLETE);
            complete.setAsset(asset1);
            workOrderRepository.saveAndFlush(complete);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            List<Object[]> result = workOrderService.findTopNAssetsByIncompleteWO(
                    company.getId(), start, end, 5);

            assertFalse(result.isEmpty());
            Object[] top = result.get(0);
            assertEquals("HighIncidents", top[1]);
            assertEquals(3L, ((Number) top[2]).longValue());
        }

        @Test
        void findTopNAssetsByIncompleteWO_respectsLimit() {
            Asset asset1 = new Asset();
            asset1.setName("A1");
            asset1.setCompany(company);
            asset1.setStatus(AssetStatus.OPERATIONAL);
            asset1.setFiles(new ArrayList<>());
            asset1.setCustomers(new ArrayList<>());
            asset1.setTeams(new ArrayList<>());
            asset1.setParts(new ArrayList<>());
            asset1.setCustomFieldValues(new ArrayList<>());
            asset1 = assetRepository.saveAndFlush(asset1);

            Asset asset2 = new Asset();
            asset2.setName("A2");
            asset2.setCompany(company);
            asset2.setStatus(AssetStatus.OPERATIONAL);
            asset2.setFiles(new ArrayList<>());
            asset2.setCustomers(new ArrayList<>());
            asset2.setTeams(new ArrayList<>());
            asset2.setParts(new ArrayList<>());
            asset2.setCustomFieldValues(new ArrayList<>());
            asset2 = assetRepository.saveAndFlush(asset2);

            for (int i = 0; i < 3; i++) {
                WorkOrder wo = createQueryWO("WO " + i, Status.OPEN);
                wo.setAsset(i % 2 == 0 ? asset1 : asset2);
                workOrderRepository.saveAndFlush(wo);
            }

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            List<Object[]> result = workOrderService.findTopNAssetsByIncompleteWO(
                    company.getId(), start, end, 1);

            assertEquals(1, result.size());
        }

        @Test
        void findTopNAssetsTimeCost_returnsCostAggregation() {
            Asset asset = new Asset();
            asset.setName("CostlyAsset");
            asset.setCompany(company);
            asset.setStatus(AssetStatus.OPERATIONAL);
            asset.setAcquisitionCost(50000.0);
            asset.setFiles(new ArrayList<>());
            asset.setCustomers(new ArrayList<>());
            asset.setTeams(new ArrayList<>());
            asset.setParts(new ArrayList<>());
            asset.setCustomFieldValues(new ArrayList<>());
            asset = assetRepository.saveAndFlush(asset);

            WorkOrder wo = createQueryWO("Cost WO", Status.COMPLETE);
            wo.setAsset(asset);
            wo.setCompletedBy(user);
            wo.setCompletedOn(new Date());
            workOrderRepository.saveAndFlush(wo);

            Labor labor = new Labor();
            labor.setWorkOrder(wo);
            labor.setCompany(company);
            labor.setStartedAt(new Date());
            labor.setDuration(3600L);
            labor.setHourlyRate(50L);
            laborRepository.saveAndFlush(labor);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            List<Object[]> result = workOrderService.findTopNAssetsTimeCost(
                    company.getId(), start, end, 5);

            assertFalse(result.isEmpty());
            Object[] row = result.get(0);
            assertEquals("CostlyAsset", row[1]);
            assertEquals(3600L, ((Number) row[2]).longValue());
            assertEquals(50.0, ((Number) row[3]).doubleValue(), 0.001);
        }

        @Test
        void findWOCostsByDateRange_returnsCostsByCompletedDate() {
            WorkOrder wo = createQueryWO("Cost By Date", Status.COMPLETE);
            wo.setCompletedBy(user);
            wo.setCompletedOn(new Date());
            workOrderRepository.saveAndFlush(wo);

            Labor labor = new Labor();
            labor.setWorkOrder(wo);
            labor.setCompany(company);
            labor.setStartedAt(new Date());
            labor.setDuration(1800L);
            labor.setHourlyRate(100L);
            laborRepository.saveAndFlush(labor);

            AdditionalCost ac = new AdditionalCost();
            ac.setWorkOrder(wo);
            ac.setCost(200.0);
            additionalCostRepository.saveAndFlush(ac);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            List<Object[]> result = workOrderService.findWOCostsByDateRange(
                    company.getId(), start, end);

            assertFalse(result.isEmpty());
            Object[] row = result.get(0);
            assertEquals(wo.getId(), row[0]);
            assertNotNull(row[1]);
            assertEquals(50.0, ((Number) row[2]).doubleValue(), 0.001);
            assertEquals(200.0, ((Number) row[4]).doubleValue(), 0.001);
        }

        @Test
        void findTopNAssetsRepairTime_returnsRankedByAvgDuration() {
            Asset asset = new Asset();
            asset.setName("SlowRepairAsset");
            asset.setCompany(company);
            asset.setStatus(AssetStatus.OPERATIONAL);
            asset.setFiles(new ArrayList<>());
            asset.setCustomers(new ArrayList<>());
            asset.setTeams(new ArrayList<>());
            asset.setParts(new ArrayList<>());
            asset.setCustomFieldValues(new ArrayList<>());
            asset = assetRepository.saveAndFlush(asset);

            WorkOrder wo = createQueryWO("Repair WO", Status.COMPLETE);
            wo.setAsset(asset);
            wo.setCompletedBy(user);
            wo.setCompletedOn(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)));
            workOrderRepository.saveAndFlush(wo);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));

            List<Object[]> result = workOrderService.findTopNAssetsRepairTime(
                    company.getId(), start, end, 5);

            assertFalse(result.isEmpty());
            assertEquals("SlowRepairAsset", result.get(0)[1]);
        }

        @Test
        void findTotalWOCosts_returnsAggregatedSums() {
            Asset asset = new Asset();
            asset.setName("TotalCostAsset");
            asset.setCompany(company);
            asset.setStatus(AssetStatus.OPERATIONAL);
            asset.setAcquisitionCost(100000.0);
            asset.setFiles(new ArrayList<>());
            asset.setCustomers(new ArrayList<>());
            asset.setTeams(new ArrayList<>());
            asset.setParts(new ArrayList<>());
            asset.setCustomFieldValues(new ArrayList<>());
            asset = assetRepository.saveAndFlush(asset);

            WorkOrder wo1 = createQueryWO("Total WO 1", Status.COMPLETE);
            wo1.setAsset(asset);
            wo1.setCompletedBy(user);
            wo1.setCompletedOn(new Date());
            workOrderRepository.saveAndFlush(wo1);

            WorkOrder wo2 = createQueryWO("Total WO 2", Status.COMPLETE);
            wo2.setAsset(asset);
            wo2.setCompletedBy(user);
            wo2.setCompletedOn(new Date());
            workOrderRepository.saveAndFlush(wo2);

            Labor labor1 = new Labor();
            labor1.setWorkOrder(wo1);
            labor1.setCompany(company);
            labor1.setStartedAt(new Date());
            labor1.setDuration(3600L);
            labor1.setHourlyRate(100L);
            laborRepository.saveAndFlush(labor1);

            Labor labor2 = new Labor();
            labor2.setWorkOrder(wo2);
            labor2.setCompany(company);
            labor2.setStartedAt(new Date());
            labor2.setDuration(3600L);
            labor2.setHourlyRate(50L);
            laborRepository.saveAndFlush(labor2);

            AdditionalCost ac = new AdditionalCost();
            ac.setWorkOrder(wo1);
            ac.setCost(300.0);
            additionalCostRepository.saveAndFlush(ac);

            em.clear();
            Date start = new Date(0);
            Date end = new Date(System.currentTimeMillis() + 86400000);

            List<Object[]> result = workOrderService.findTotalWOCosts(
                    company.getId(), start, end);

            assertFalse(result.isEmpty());
            Object[] row = result.get(0);
            assertEquals(150.0, ((Number) row[0]).doubleValue(), 0.001);
            assertEquals(300.0, ((Number) row[2]).doubleValue(), 0.001);
        }
    }
}
