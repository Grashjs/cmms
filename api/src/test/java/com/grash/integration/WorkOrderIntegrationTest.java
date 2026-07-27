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

import static com.grash.utils.Consts.usageBasedLicenseLimits;
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
}
