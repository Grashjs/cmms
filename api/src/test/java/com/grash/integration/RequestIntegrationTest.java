package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.RequestApproveDTO;
import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestPostDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.*;
import com.grash.service.RequestService;
import com.grash.service.WebhookDispatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@Transactional
class RequestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RequestRepository requestRepository;
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
    private RequestService requestService;
    @Autowired
    private GeneralPreferencesRepository generalPreferencesRepository;
    @Autowired
    private CurrencyRepository currencyRepository;

    @MockitoBean
    private WebhookDispatchService webhookDispatchService;

    private Company company;
    private User user;
    private User otherUser;
    private Role adminRole;
    private Role restrictedRole;

    @BeforeEach
    void setUpBase() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("Test Plan")
                .monthlyCostPerUser(10.0)
                .yearlyCostPerUser(100.0)
                .features(new HashSet<>(Arrays.asList(PlanFeatures.SIGNATURE, PlanFeatures.WEBHOOK,
                        PlanFeatures.REQUEST_PORTAL)))
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
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .viewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.REQUESTS, PermissionEntity.SETTINGS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
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

        restrictedRole = Role.builder()
                .name("Restricted")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.VIEW_ONLY)
                .companySettings(settings)
                .createPermissions(new HashSet<>())
                .viewPermissions(new HashSet<>())
                .viewOtherPermissions(new HashSet<>())
                .editOtherPermissions(new HashSet<>())
                .deleteOtherPermissions(new HashSet<>())
                .build();
        restrictedRole = roleRepository.save(restrictedRole);

        otherUser = new User();
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmail("other@test.com");
        otherUser.setUsername("otheruser");
        otherUser.setPassword("encoded");
        otherUser.setRole(restrictedRole);
        otherUser.setCompany(company);
        otherUser.setEnabled(true);
        otherUser.setSuperAccountRelations(new ArrayList<>());
        otherUser.setUserSettings(new UserSettings());
        otherUser = userRepository.save(otherUser);

        setCurrentUser(user);
    }

    private RequestPostDTO buildPostDTO(String title) {
        RequestPostDTO dto = new RequestPostDTO();
        dto.setTitle(title);
        dto.setDescription("Integration test request");
        dto.setEstimatedDuration(1.0);
        dto.setPriority(Priority.NONE);
        dto.setAssignedTo(new ArrayList<>());
        dto.setCustomers(new ArrayList<>());
        dto.setFiles(new ArrayList<>());
        dto.setCustomFields(new ArrayList<>());
        return dto;
    }

    private Request createPendingRequest(String title) {
        Request request = new Request();
        request.setTitle(title);
        request.setDescription("Pending request");
        request.setCompany(company);
        request.setCreatedBy(user.getId());
        request.setCustomFieldValues(new ArrayList<>());
        request.setAssignedTo(new ArrayList<>());
        request.setCustomers(new ArrayList<>());
        request.setFiles(new ArrayList<>());
        return requestRepository.saveAndFlush(request);
    }

    @Nested
    class CreateTests {

        @Test
        void create_sequentialCustomIds() {
            RequestPostDTO dto1 = buildPostDTO("First Request");
            RequestPostDTO dto2 = buildPostDTO("Second Request");

            Request result1 = requestService.create(dto1, user);
            Request result2 = requestService.create(dto2, user);

            assertNotNull(result1.getCustomId());
            assertNotNull(result2.getCustomId());
            assertNotEquals(result1.getCustomId(), result2.getCustomId());
            assertTrue(result1.getCustomId().startsWith("R"));
            assertTrue(result2.getCustomId().startsWith("R"));
        }

        @Test
        void create_persistsAndLinksCompany() {
            RequestPostDTO dto = buildPostDTO("Persisted Request");

            Request result = requestService.create(dto, user);

            em.flush();
            em.clear();
            Request fromDb = requestRepository.findById(result.getId()).get();
            assertEquals("Persisted Request", fromDb.getTitle());
            assertEquals(company.getId(), fromDb.getCompany().getId());
        }

        @Test
        void create_dispatchesNewRequestWebhook() {
            RequestPostDTO dto = buildPostDTO("Webhook Request");

            requestService.create(dto, user);

            verify(webhookDispatchService).dispatchWebhook(
                    eq(company),
                    eq(WebhookEvent.NEW_REQUEST),
                    any(),
                    eq("newRequest"),
                    any(),
                    isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        void create_withoutCreatePermission_throwsForbidden() {
            RequestPostDTO dto = buildPostDTO("Forbidden Request");

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.create(dto, otherUser));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class CountPendingTests {

        @Test
        void countPending_countsOnlyUncancelledAndUnapproved() {
            createPendingRequest("Pending 1");
            createPendingRequest("Pending 2");
            Request cancelled = createPendingRequest("Cancelled");
            cancelled.setCancelled(true);
            requestRepository.saveAndFlush(cancelled);

            Request approved = createPendingRequest("Approved");
            WorkOrder wo = new WorkOrder();
            wo.setTitle("Approved WO");
            wo.setStatus(Status.OPEN);
            wo.setPriority(Priority.NONE);
            wo.setEstimatedDuration(1.0);
            wo.setCompany(company);
            wo.setCreatedBy(user.getId());
            wo.setAssignedTo(new ArrayList<>());
            wo.setCustomers(new ArrayList<>());
            wo.setFiles(new ArrayList<>());
            wo.setCustomFieldValues(new ArrayList<>());
            wo = em.merge(wo);
            em.flush();
            approved.setWorkOrder(wo);
            requestRepository.saveAndFlush(approved);

            em.flush();
            Integer count = requestService.countPending(company.getId());

            assertEquals(2, count);
        }
    }

    @Nested
    class CreateFromPortalTests {

        @Test
        void createFromPortal_missingRecaptchaToken_throwsNotAcceptable() {
            Request request = new Request();
            request.setTitle("Portal Request");

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.createFromPortal(request, "any-uuid", null));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class ApproveTests {

        @Test
        void approve_createsWorkOrderAndLinks() {
            Request request = requestService.create(buildPostDTO("Approve Me"), user);

            RequestApproveDTO approveDTO = new RequestApproveDTO();

            WorkOrder workOrder = requestService.approve(request.getId(), approveDTO, user);

            assertNotNull(workOrder);
            assertNotNull(workOrder.getId());

            em.flush();
            em.clear();
            Request fromDb = requestRepository.findById(request.getId()).get();
            assertNotNull(fromDb.getWorkOrder());
            assertEquals(workOrder.getId(), fromDb.getWorkOrder().getId());
        }

        @Test
        void approve_alreadyApproved_throwsNotAcceptable() {
            Request request = requestService.create(buildPostDTO("Double Approve"), user);
            requestService.approve(request.getId(), new RequestApproveDTO(), user);

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.approve(request.getId(), new RequestApproveDTO(), user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void approve_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.approve(99999L, new RequestApproveDTO(), user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class CancelTests {

        @Test
        void cancel_setsCancelledAndReason() {
            Request request = requestService.create(buildPostDTO("Cancel Me"), user);

            Request result = requestService.cancel(request.getId(), "no longer needed", user);

            em.flush();
            em.clear();
            Request fromDb = requestRepository.findById(request.getId()).get();
            assertTrue(fromDb.isCancelled());
            assertEquals("no longer needed", fromDb.getCancellationReason());
        }

        @Test
        void cancel_emptyReason_throwsNotAcceptable() {
            Request request = requestService.create(buildPostDTO("No Reason"), user);

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.cancel(request.getId(), "   ", user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void cancel_alreadyApproved_throwsNotAcceptable() {
            Request request = requestService.create(buildPostDTO("Approved Then Cancel"), user);
            requestService.approve(request.getId(), new RequestApproveDTO(), user);

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.cancel(request.getId(), "too late", user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class GetByIdTests {

        @Test
        void getById_returnsRequestForViewer() {
            Request request = createPendingRequest("Viewable Request");

            Request result = requestService.getById(request.getId(), user);

            assertEquals(request.getId(), result.getId());
        }

        @Test
        void getById_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.getById(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void patch_updatesTitle() {
            Request request = requestService.create(buildPostDTO("Original Title"), user);

            RequestPatchDTO patch = new RequestPatchDTO();
            patch.setTitle("Updated Title");
            Request result = requestService.patch(request.getId(), patch, user);

            assertNotNull(result);
            em.flush();
            em.clear();
            Request fromDb = requestRepository.findById(request.getId()).get();
            assertEquals("Updated Title", fromDb.getTitle());
        }
    }

    @Nested
    class SearchTests {

        @Test
        void getSearchCriteria_clientRole_filtersByCompany() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            SearchCriteria result = requestService.getSearchCriteria(user, criteria);

            boolean hasCompanyFilter = result.getFilterFields().stream()
                    .anyMatch(f -> "company".equals(f.getField()) && f.getValue().equals(company.getId()));
            assertTrue(hasCompanyFilter);
        }

        @Test
        void findByCompany_returnsOnlyRequestsForCompany() {
            requestService.create(buildPostDTO("Company Request A"), user);

            em.flush();
            Collection<Request> result = requestService.findByCompany(company.getId());

            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(r -> company.getId().equals(r.getCompany().getId())));
        }

        @Test
        void findBySearchCriteria_filtersByStatusPending() {
            requestService.create(buildPostDTO("Searchable Request"), user);

            SearchCriteria criteria = new SearchCriteria();
            criteria.setFilterFields(new ArrayList<>());
            criteria.getFilterFields().add(FilterField.builder()
                    .field("status")
                    .operation("eq")
                    .values(new ArrayList<>(Collections.singletonList("PENDING")))
                    .build());
            criteria.setPageNum(0);
            criteria.setPageSize(10);
            criteria.setSortField("id");
            criteria.setDirection(Sort.Direction.DESC);

            em.flush();
            Page<Request> result = requestService.findBySearchCriteria(criteria);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteByIdAndUser_removesFromDB() {
            Request request = requestService.create(buildPostDTO("Delete Me"), user);
            Long id = request.getId();

            requestService.deleteByIdAndUser(id, user);

            em.flush();
            em.clear();
            assertFalse(requestRepository.findById(id).isPresent());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.deleteByIdAndUser(99999L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }
}
