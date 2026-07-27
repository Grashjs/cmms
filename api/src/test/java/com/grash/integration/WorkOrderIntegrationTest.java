package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.ReportConfig;
import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.workOrder.WorkOrderSendReportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.*;
import com.grash.security.CustomUserDetail;
import com.grash.service.TeamService;
import com.grash.service.WebhookDispatchService;
import com.grash.service.WorkOrderService;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.grash.utils.Consts.usageBasedLicenseLimits;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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

    // ═══════════════════════════════════════════════════════════════════
    // create
    // ═══════════════════════════════════════════════════════════════════
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
            for (int i = 0; i < usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS); i++) {
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

    // ═══════════════════════════════════════════════════════════════════
    // changeStatus
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // findBySearchCriteria + getSearchCriteria
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // sendReport
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // deleteByIdAndUser
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // addFiles / removeFile
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // cost computation
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // JPA query verification
    // ═══════════════════════════════════════════════════════════════════
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

            assertTrue(result.stream().allMatch(wo -> wo.getCreatedBy().equals(user.getId())));
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
    }
}
