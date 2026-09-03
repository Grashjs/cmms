package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.RequestApproveDTO;
import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestPostDTO;
import com.grash.dto.RequestShowDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.RequestMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.RequestRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @InjectMocks
    private RequestService requestService;

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private RequestMapper requestMapper;
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
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private MailService mailService;
    @Mock
    private AssetService assetService;
    @Mock
    private RequestPortalService requestPortalService;
    @Mock
    private WorkflowService workflowService;
    /**
     * {@code RequestService} publishes {@link com.grash.event.RequestCreatedEvent} on create.
     * Without this mock the constructor injection leaves the publisher null and the create test
     * dies on the publish rather than on anything it means to assert.
     */
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Company company;
    private User user;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;
    private CompanySettings companySettings;
    private GeneralPreferences generalPreferences;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(requestService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(requestService, "workflowService", workflowService);

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
                .createPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .viewOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .editOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.REQUESTS)))
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

    private Request buildRequest(Long id) {
        Request r = new Request();
        r.setId(id);
        r.setTitle("Test Request");
        r.setDescription("desc");
        r.setPriority(Priority.NONE);
        r.setEstimatedDuration(1.0);
        r.setCompany(company);
        r.setCreatedBy(user.getId());
        r.setAssignedTo(new ArrayList<>());
        r.setCustomers(new ArrayList<>());
        r.setFiles(new ArrayList<>());
        r.setCustomFieldValues(new ArrayList<>());
        return r;
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

    private void stubSaveReturningSaved(Request request) {
        when(requestRepository.saveAndFlush(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(em).refresh(any(Request.class));
        when(requestMapper.toShowDto(any(Request.class))).thenReturn(new RequestShowDTO());
    }

    @Nested
    class CreateTests {

        @Test
        void create_assignsSequentialCustomId() {
            Request request = buildRequest(1L);
            request.setAudioDescription(null);
            when(customSequenceService.getNextRequestSequence(company)).thenReturn(5L);
            stubSaveReturningSaved(request);

            Request result = requestService.create(request, company);

            assertEquals("R000005", result.getCustomId());
            verify(requestRepository).saveAndFlush(request);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.NEW_REQUEST), anyMap(),
                    eq("newRequest"), any(), any(), any(), any(), any(), any());
        }

        @Test
        void create_voiceNotesWithoutLicense_throwsForbidden() {
            Request request = buildRequest(1L);
            request.setAudioDescription(new File());
            when(licenseService.hasEntitlement(LicenseEntitlement.VOICE_NOTES)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class, () -> requestService.create(request, company));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(requestRepository, never()).saveAndFlush(any());
        }

        @Test
        void create_fromPostDTO_setsCustomFields() {
            RequestPostDTO dto = new RequestPostDTO();
            dto.setTitle("Portal Request");
            dto.getCustomFields().add(new com.grash.dto.cutomField.CustomFieldValuePostDTO());
            Request mapped = buildRequest(1L);
            mapped.setAudioDescription(null);
            when(requestMapper.fromPostDTO(any(RequestPostDTO.class))).thenReturn(mapped);
            when(customSequenceService.getNextRequestSequence(company)).thenReturn(1L);
            stubSaveReturningSaved(mapped);

            Request result = requestService.create(dto, company);

            assertNotNull(result.getCustomId());
            verify(requestRepository).saveAndFlush(mapped);
        }
    }

    @Nested
    class CreateWithPortalTests {

        @Test
        void create_preservesSequenceAndPortal() {
            Request request = buildRequest(1L);
            request.setAudioDescription(null);
            RequestPortal portal = new RequestPortal();
            portal.setCompany(company);
            portal.setFields(new ArrayList<>());
            when(customSequenceService.getNextRequestSequence(company)).thenReturn(7L);
            stubSaveReturningSaved(request);

            Request result = requestService.create(request, company, portal);

            assertEquals("R000007", result.getCustomId());
            assertSame(portal, result.getRequestPortal());
            assertEquals(company, result.getCompany());
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void update_existing_patchesAndSaves() {
            Request saved = buildRequest(1L);
            when(requestRepository.existsById(1L)).thenReturn(true);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));
            Request updated = buildRequest(1L);
            when(requestMapper.updateRequest(any(), any())).thenReturn(updated);
            when(requestRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(em).refresh(any());

            Request result = requestService.update(1L, new RequestPatchDTO(), company);

            assertNotNull(result);
            verify(requestMapper).updateRequest(saved, new RequestPatchDTO());
        }

        @Test
        void update_notFound_throwsNotFound() {
            when(requestRepository.existsById(1L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.update(1L, new RequestPatchDTO(), company));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class FindByIdAndCompanyTests {

        @Test
        void findById_returnsOptional() {
            Request saved = buildRequest(1L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            assertEquals(Optional.of(saved), requestService.findById(1L));
        }

        @Test
        void findByCompany_returnsCollection() {
            when(requestRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(buildRequest(1L)));

            Collection<Request> result = requestService.findByCompany(1L);

            assertEquals(1, result.size());
        }
    }

    @Nested
    class GetByIdTests {

        @Test
        void getById_withViewPermission_returnsRequest() {
            Request saved = buildRequest(1L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            assertEquals(saved, requestService.getById(1L, user));
        }

        @Test
        void getById_withoutViewPermission_throwsForbidden() {
            role.getViewPermissions().clear();
            role.getViewOtherPermissions().clear();
            Request saved = buildRequest(1L);
            saved.setCreatedBy(999L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class, () -> requestService.getById(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getById_notFound_throwsNotFound() {
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> requestService.getById(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class CreateWithUserTests {

        @Test
        void create_userWithPermission_createsAndNotifies() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            RequestPostDTO dto = new RequestPostDTO();
            dto.setTitle("New");
            Request created = buildRequest(1L);
            when(requestMapper.fromPostDTO(any())).thenReturn(created);
            created.setCreatedBy(2L);
            when(customSequenceService.getNextRequestSequence(company)).thenReturn(1L);
            stubSaveReturningSaved(created);
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("t");
            when(userService.findByCompany(1L)).thenReturn(new ArrayList<>());
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(mailServiceFactory.getMailService()).thenReturn(mailService);

            Request result = requestService.create(dto, user);

            assertNotNull(result.getCustomId());
            verify(notificationService).createMultiple(anyList(), anyBoolean(), anyString());
        }
    }

    @Nested
    class PatchTests {

        @Test
        void patch_existingAndEditable_patches() {
            Request saved = buildRequest(1L);
            saved.setWorkOrder(null);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(requestRepository.existsById(1L)).thenReturn(true);
            when(requestMapper.updateRequest(any(), any())).thenReturn(saved);
            when(requestRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(em).refresh(any());

            Request result = requestService.patch(1L, new RequestPatchDTO(), user);

            assertNotNull(result);
            verify(requestRepository).saveAndFlush(any());
        }

        @Test
        void patch_approvedRequest_throwsNotAcceptable() {
            Request saved = buildRequest(1L);
            saved.setWorkOrder(new WorkOrder());
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.patch(1L, new RequestPatchDTO(), user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void patch_notEditable_throwsForbidden() {
            role.getEditOtherPermissions().clear();
            Request saved = buildRequest(1L);
            saved.setCreatedBy(999L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.patch(1L, new RequestPatchDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void patch_notFound_throwsNotFound() {
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.patch(1L, new RequestPatchDTO(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class ApproveTests {

        @Test
        void approve_noViewPermission_throwsForbidden() {
            role.getViewPermissions().clear();
            role.setCode(RoleCode.ADMIN);

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.approve(1L, new RequestApproveDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void approve_alreadyApproved_throwsNotAcceptable() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            Request saved = buildRequest(1L);
            saved.setWorkOrder(new WorkOrder());
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.approve(1L, new RequestApproveDTO(), user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class CancelTests {

        @Test
        void cancel_setsCancelledAndReason() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            Request saved = buildRequest(1L);
            saved.setWorkOrder(null);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("t");
            when(userService.findByCompany(1L)).thenReturn(new ArrayList<>());
            when(userService.findById(1L)).thenReturn(Optional.of(user));
            when(mailServiceFactory.getMailService()).thenReturn(mailService);

            Request result = requestService.cancel(1L, "Not needed", user);

            assertTrue(result.isCancelled());
            assertEquals("Not needed", result.getCancellationReason());
        }

        @Test
        void cancel_alreadyApproved_throwsNotAcceptable() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            Request saved = buildRequest(1L);
            saved.setWorkOrder(new WorkOrder());
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.cancel(1L, "reason", user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void cancel_withoutReason_throwsNotAcceptable() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            Request saved = buildRequest(1L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.cancel(1L, "  ", user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void cancel_withoutPermission_throwsForbidden() {
            role.getViewPermissions().clear();
            role.setCode(RoleCode.USER_CREATED);

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.cancel(1L, "reason", user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteByIdAndUserTests {

        @Test
        void deleteByIdAndUser_withPermission_deletes() {
            Request saved = buildRequest(1L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            requestService.deleteByIdAndUser(1L, user);

            verify(requestRepository).deleteById(1L);
        }

        @Test
        void deleteByIdAndUser_withoutPermission_throwsForbidden() {
            role.getDeleteOtherPermissions().clear();
            Request saved = buildRequest(1L);
            saved.setCreatedBy(999L);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class DeleteAndGetAllTests {

        @Test
        void delete_callsRepositoryDelete() {
            requestService.delete(1L);
            verify(requestRepository).deleteById(1L);
        }

        @Test
        void getAll_returnsAll() {
            when(requestRepository.findAll()).thenReturn(Collections.singletonList(buildRequest(1L)));

            assertEquals(1, requestService.getAll().size());
        }
    }

    @Nested
    class CountAndFindTests {

        @Test
        void countPending_delegatesToRepository() {
            when(requestRepository.countPending(1L)).thenReturn(3);

            assertEquals(3, requestService.countPending(1L));
        }

        @Test
        void findByCategoryAndCreatedAtBetween_delegates() {
            Date start = new Date(1000);
            Date end = new Date(2000);
            when(requestRepository.findByCategory_IdAndCreatedAtBetween(1L, start, end))
                    .thenReturn(Collections.singletonList(buildRequest(1L)));

            assertEquals(1, requestService.findByCategoryAndCreatedAtBetween(1L, start, end).size());
        }

        @Test
        void findByCreatedAtBetweenAndCompany_delegates() {
            Date start = new Date(1000);
            Date end = new Date(2000);
            when(requestRepository.findByCreatedAtBetweenAndCompany_Id(start, end, 1L))
                    .thenReturn(Collections.singletonList(buildRequest(1L)));

            assertEquals(1, requestService.findByCreatedAtBetweenAndCompany(start, end, 1L).size());
        }
    }

    @Nested
    class FindBySearchCriteriaTests {

        @Test
        void findBySearchCriteria_returnsMappedPage() {
            SearchCriteria criteria = new SearchCriteria();
            Request r = buildRequest(1L);
            when(requestRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(r)));

            Page<Request> result = requestService.findBySearchCriteria(criteria);

            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    class GetSearchCriteriaTests {

        @Test
        void nonClientRole_returnsUnmodifiedCriteria() {
            role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = requestService.getSearchCriteria(user, criteria);

            assertTrue(result.getFilterFields().isEmpty());
        }

        @Test
        void clientRole_withoutViewPermission_throwsForbidden() {
            role.setRoleType(RoleType.ROLE_CLIENT);
            role.getViewPermissions().clear();

            CustomException ex = assertThrows(CustomException.class,
                    () -> requestService.getSearchCriteria(user, new SearchCriteria()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void clientRole_withoutViewOthers_filtersByCreated() {
            role.setRoleType(RoleType.ROLE_CLIENT);
            role.getViewPermissions().add(PermissionEntity.REQUESTS);
            role.getViewOtherPermissions().clear();
            user.setSuperAccountRelations(new ArrayList<>());

            SearchCriteria result = requestService.getSearchCriteria(user, new SearchCriteria());

            boolean hasCreatedBy = result.getFilterFields().stream()
                    .anyMatch(f -> "createdBy".equals(f.getField()));
            assertTrue(hasCreatedBy);
        }
    }
}
