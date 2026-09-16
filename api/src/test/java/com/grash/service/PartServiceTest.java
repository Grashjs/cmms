package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartPostDTO;
import com.grash.dto.PartRestockDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.imports.PartImportDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.PartMapper;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.repository.PartRepository;
import com.grash.utils.Consts;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @InjectMocks
    private PartService partService;

    @Mock
    private PartRepository partRepository;
    @Mock
    private PartCategoryService partCategoryService;
    @Mock
    private PartTransactionService partTransactionService;
    @Mock
    private CompanyService companyService;
    @Mock
    private CustomerService customerService;
    @Mock
    private VendorService vendorService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private LocationService locationService;
    @Mock
    private PartMapper partMapper;
    @Mock
    private EntityManager em;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserService userService;
    @Mock
    private TeamService teamService;
    @Mock
    private LicenseService licenseService;
    @Mock
    private WebhookDispatchService webhookDispatchService;
    @Mock
    private CustomFieldValueService customFieldValueService;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private MailService mailService;

    private Company company;
    private CompanySettings companySettings;
    private GeneralPreferences generalPreferences;
    private User user;
    private Role adminRole;
    private Role restrictedRole;
    private PartShowDTO showDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(partService, "frontendUrl", "http://localhost:3000");

        SubscriptionPlan subscriptionPlan = SubscriptionPlan.builder()
                .id(1L)
                .name("Pro")
                .features(new HashSet<>())
                .build();
        Subscription subscription = Subscription.builder()
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

        adminRole = adminRole();
        restrictedRole = restrictedRole();

        user = buildUser(1L);
        showDto = new PartShowDTO();
        showDto.setId(1L);
        showDto.setName("Part1");
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
        Set<PermissionEntity> parts = new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS));
        return buildRole(parts, parts, parts, parts, parts);
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
        u.setRole(id.equals(1L) ? adminRole : adminRole());
        u.setCompany(company);
        u.setEnabled(true);
        u.setSuperAccountRelations(new ArrayList<>());
        u.setUserSettings(new UserSettings());
        return u;
    }

    private Part buildPart(Long id) {
        Part p = new Part();
        p.setId(id);
        p.setName("Part" + id);
        p.setCompany(company);
        p.setCreatedBy(1L);
        p.setQuantity(0);
        p.setAssignedTo(new ArrayList<>());
        p.setTeams(new ArrayList<>());
        p.setCustomFieldValues(new ArrayList<>());
        return p;
    }

    private PartPatchDTO buildPatchDTO() {
        return new PartPatchDTO();
    }

    private WorkOrder buildWorkOrder() {
        WorkOrder wo = new WorkOrder();
        wo.setId(10L);
        wo.setTitle("WO");
        wo.setCompany(company);
        return wo;
    }

    private void stubPartLimit(boolean hasEntitlement, boolean hasMore) {
        lenient().when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PARTS)).thenReturn(hasEntitlement);
        lenient().when(partRepository.hasMoreThan(eq(company.getId()), anyLong())).thenReturn(hasMore);
    }

    private void stubShowDto() {
        lenient().when(partMapper.toShowDto(any(Part.class))).thenReturn(showDto);
    }

    private void stubMessageKeys() {
        lenient().when(messageSource.getMessage(eq("new_assignment"), isNull(), any(Locale.class)))
                .thenReturn("New assignment");
        lenient().when(messageSource.getMessage(eq("notification_part_assigned"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Part assigned");
        lenient().when(messageSource.getMessage(eq("notification_part_low"), any(Object[].class),
                        any(Locale.class)))
                .thenReturn("Low stock");
        lenient().when(messageSource.getMessage(eq("low_stock"), isNull(), any(Locale.class)))
                .thenReturn("Low stock");
    }

    @Nested
    class Create {

        @Test
        void savesAndDispatchesNewPartWebhook() {
            Part part = buildPart(1L);
            stubPartLimit(false, false);
            when(partRepository.saveAndFlush(part)).thenReturn(part);
            stubShowDto();

            Part result = partService.create(part, user);

            assertSame(part, result);
            verify(em).refresh(part);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.NEW_PART), anyMap(),
                    eq("newPart"), eq(showDto), isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        void overFreeLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PARTS)).thenReturn(false);
            when(partRepository.hasMoreThan(eq(1L),
                    eq((long) (Consts.usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_PARTS) - 1))))
                    .thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.create(buildPart(1L), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(partRepository, never()).saveAndFlush(any());
        }

        @Test
        void withEntitlement_bypassesLimit() {
            Part part = buildPart(1L);
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PARTS)).thenReturn(true);
            when(partRepository.saveAndFlush(part)).thenReturn(part);
            stubShowDto();

            partService.create(part, user);

            verify(partRepository, never()).hasMoreThan(anyLong(), anyLong());
        }

        @Test
        void withPostDto_mapsFromPostDto() {
            PartPostDTO dto = new PartPostDTO();
            dto.setName("From DTO");
            Part mapped = buildPart(1L);
            when(partMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(partRepository.saveAndFlush(mapped)).thenReturn(mapped);
            stubShowDto();

            Part result = partService.create(dto, user);

            assertSame(mapped, result);
            verify(partMapper).fromPostDto(dto);
        }

        @Test
        void withPostDtoAndCustomFields_setsCustomFields() {
            PartPostDTO dto = new PartPostDTO();
            dto.setCustomFields(new ArrayList<>(Collections.singletonList(new CustomFieldValuePostDTO())));
            Part mapped = buildPart(1L);
            when(partMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(partRepository.saveAndFlush(mapped)).thenReturn(mapped);
            stubShowDto();

            partService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), eq(dto.getCustomFields()),
                    eq(company), eq(CustomFieldEntityType.PART), any());
        }

        @Test
        void withPostDtoEmptyCustomFields_skipsCustomFields() {
            PartPostDTO dto = new PartPostDTO();
            Part mapped = buildPart(1L);
            when(partMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(partRepository.saveAndFlush(mapped)).thenReturn(mapped);
            stubShowDto();

            partService.create(dto, user);

            verify(customFieldValueService, never()).setCustomFields(any(), any(), any(), any(), any(), any());
        }

        @Test
        void customFieldConsumer_setsPartOnCustomFieldValue() {
            PartPostDTO dto = new PartPostDTO();
            dto.setCustomFields(new ArrayList<>(Collections.singletonList(new CustomFieldValuePostDTO())));
            Part mapped = buildPart(1L);
            when(partMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(partRepository.saveAndFlush(mapped)).thenReturn(mapped);
            stubShowDto();

            doAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Consumer<CustomFieldValue> consumer = inv.getArgument(5);
                CustomFieldValue cfv = new CustomFieldValue();
                consumer.accept(cfv);
                assertSame(mapped, cfv.getPart());
                return null;
            }).when(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.PART), any());

            partService.create(dto, user);

            verify(customFieldValueService).setCustomFields(eq(mapped), any(), any(), eq(company),
                    eq(CustomFieldEntityType.PART), any());
        }
    }

    @Nested
    class CreateFromPostDtoAndUser {

        @Test
        void createsPartAndNotifies() {
            PartPostDTO dto = new PartPostDTO();
            dto.setName("New Part");
            Part mapped = buildPart(1L);
            when(partMapper.fromPostDto(dto)).thenReturn(mapped);
            stubPartLimit(false, false);
            when(partRepository.saveAndFlush(mapped)).thenReturn(mapped);
            stubShowDto();
            stubMessageKeys();

            Part result = partService.create(dto, user);

            assertSame(mapped, result);
            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
        }

        @Test
        void withoutCreatePermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.create(new PartPostDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void duplicateBarcode_throwsNotAcceptable() {
            PartPostDTO dto = new PartPostDTO();
            dto.setBarcode("BAR");
            when(partRepository.findByBarcodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildPart(2L)));

            CustomException ex = assertThrows(CustomException.class, () -> partService.create(dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class Update {

        @Test
        void updatesExistingPart() {
            Part saved = buildPart(1L);
            PartPatchDTO dto = buildPatchDTO();
            when(partRepository.existsById(1L)).thenReturn(true);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partMapper.updatePart(saved, dto)).thenReturn(saved);
            when(partRepository.saveAndFlush(saved)).thenReturn(saved);
            stubShowDto();

            Part result = partService.update(1L, dto, company);

            assertSame(saved, result);
            verify(em).refresh(saved);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.PART_CHANGE), anyMap(),
                    eq("changedPart"), eq(showDto), isNull(), isNull(), isNull(), isNull(), anyCollection());
        }

        @Test
        void notFound_throwsNotFound() {
            when(partRepository.existsById(1L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.update(1L, buildPatchDTO(), company));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void quantityChange_dispatchesQuantityWebhook() {
            Part saved = buildPart(1L);
            Part patched = buildPart(1L);
            patched.setQuantity(5);
            PartPatchDTO dto = buildPatchDTO();
            dto.setQuantity(5);
            when(partRepository.existsById(1L)).thenReturn(true);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partMapper.updatePart(saved, dto)).thenReturn(patched);
            when(partRepository.saveAndFlush(patched)).thenReturn(patched);
            stubShowDto();

            partService.update(1L, dto, company);

            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.PART_QUANTITY_CHANGED),
                    anyMap(), eq("changedPart"), eq(showDto), isNull(), isNull(), isNull(), isNull(),
                    eq(Collections.singletonList(PartField.QUANTITY)));
        }

        @Test
        void noQuantityChange_doesNotDispatchQuantityWebhook() {
            Part saved = buildPart(1L);
            PartPatchDTO dto = buildPatchDTO();
            when(partRepository.existsById(1L)).thenReturn(true);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partMapper.updatePart(saved, dto)).thenReturn(saved);
            when(partRepository.saveAndFlush(saved)).thenReturn(saved);
            stubShowDto();

            partService.update(1L, dto, company);

            verify(webhookDispatchService, never()).dispatchWebhook(any(), eq(WebhookEvent.PART_QUANTITY_CHANGED),
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void withCustomFields_appliesCustomFields() {
            Part saved = buildPart(1L);
            PartPatchDTO dto = buildPatchDTO();
            dto.setCustomFields(new ArrayList<>(Collections.singletonList(new CustomFieldValuePostDTO())));
            when(partRepository.existsById(1L)).thenReturn(true);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partMapper.updatePart(saved, dto)).thenReturn(saved);
            when(partRepository.saveAndFlush(saved)).thenReturn(saved);
            stubShowDto();

            partService.update(1L, dto, company);

            verify(customFieldValueService).setCustomFields(eq(saved), eq(saved.getCustomFieldValues()),
                    eq(dto.getCustomFields()), eq(company), eq(CustomFieldEntityType.PART), any());
        }
    }

    @Nested
    class Patch {

        @Test
        void patchesAndRunsWorkflows() {
            Part saved = buildPart(1L);
            Part patched = buildPart(1L);
            User assigned = buildUser(5L);
            patched.setAssignedTo(new ArrayList<>(Collections.singletonList(assigned)));
            PartPatchDTO dto = buildPatchDTO();
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partRepository.existsById(1L)).thenReturn(true);
            when(partMapper.updatePart(saved, dto)).thenReturn(patched);
            when(partRepository.saveAndFlush(patched)).thenReturn(patched);
            Workflow workflow = new Workflow();
            when(workflowService.findByMainConditionAndCompany(WFMainCondition.PART_UPDATED, 1L))
                    .thenReturn(Collections.singletonList(workflow));
            stubShowDto();
            stubMessageKeys();

            Part result = partService.patch(1L, dto, user);

            assertSame(patched, result);
            verify(em).detach(saved);
            verify(workflowService).runPart(workflow, patched);
            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
        }

        @Test
        void notFound_throwsNotFound() {
            when(partRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotEdit_throwsForbidden() {
            user.setRole(restrictedRole());
            Part saved = buildPart(1L);
            saved.setCreatedBy(999L);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.patch(1L, buildPatchDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void duplicateBarcode_throwsNotAcceptable() {
            Part saved = buildPart(1L);
            PartPatchDTO dto = buildPatchDTO();
            dto.setBarcode("BAR");
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partRepository.findByBarcodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildPart(2L)));

            CustomException ex = assertThrows(CustomException.class, () -> partService.patch(1L, dto, user));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void barcodeOnSamePart_isAllowed() {
            Part saved = buildPart(1L);
            PartPatchDTO dto = buildPatchDTO();
            dto.setBarcode("BAR");
            Part samePart = buildPart(1L);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partRepository.findByBarcodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(samePart));
            when(partRepository.existsById(1L)).thenReturn(true);
            when(partMapper.updatePart(saved, dto)).thenReturn(saved);
            when(partRepository.saveAndFlush(saved)).thenReturn(saved);
            stubShowDto();
            stubMessageKeys();

            Part result = partService.patch(1L, dto, user);

            assertSame(saved, result);
            verify(workflowService).findByMainConditionAndCompany(WFMainCondition.PART_UPDATED, 1L);
        }
    }

    @Nested
    class Restock {

        @Test
        void notFound_throwsNotFound() {
            when(partRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.restock(1L, new PartRestockDTO(), user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void cannotEdit_throwsForbidden() {
            user.setRole(restrictedRole());
            Part saved = buildPart(1L);
            saved.setCreatedBy(999L);
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.restock(1L, new PartRestockDTO(), user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void restocksPart() {
            Part saved = buildPart(1L);
            PartRestockDTO dto = new PartRestockDTO();
            dto.setQuantity(5);
            dto.setDescription("Restock");
            when(partRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(partRepository.save(saved)).thenReturn(saved);
            stubShowDto();

            partService.restock(1L, dto, user);

            assertEquals(5, saved.getQuantity());
            verify(partTransactionService).create(any(PartTransaction.class));
        }
    }

    @Nested
    class RestockPart {

        @Test
        void nonStock_returnsEarly() {
            Part part = buildPart(1L);
            part.setNonStock(true);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));

            partService.restockPart(1L, 5, "desc");

            verify(partRepository, never()).save(any());
            verify(partTransactionService, never()).create(any());
        }

        @Test
        void increasesQuantityAndCreatesTransaction() {
            Part part = buildPart(1L);
            part.setQuantity(3);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));
            when(partRepository.save(part)).thenReturn(part);
            stubShowDto();

            partService.restockPart(1L, 5, "desc");

            assertEquals(8, part.getQuantity());
            verify(partTransactionService).create(any(PartTransaction.class));
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.PART_QUANTITY_CHANGED),
                    anyMap(), eq("changedPart"), eq(showDto), isNull(), isNull(), isNull(), isNull(),
                    eq(Collections.singletonList(PartField.QUANTITY)));
        }
    }

    @Nested
    class ConsumePart {

        @Test
        void nonStock_returnsEarly() {
            Part part = buildPart(1L);
            part.setNonStock(true);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));

            partService.consumePart(1L, 5, buildWorkOrder(), Locale.ENGLISH, false);

            verify(partRepository, never()).save(any());
            verify(webhookDispatchService, never()).dispatchWebhook(any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any());
        }

        @Test
        void insufficientStock_throwsNotAcceptable() {
            Part part = buildPart(1L);
            part.setQuantity(5);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.consumePart(1L, 10, buildWorkOrder(), Locale.ENGLISH, false));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void consumePositive_createsTransaction() {
            Part part = buildPart(1L);
            part.setQuantity(10);
            WorkOrder wo = buildWorkOrder();
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));
            when(partRepository.save(part)).thenReturn(part);
            stubShowDto();

            partService.consumePart(1L, 3, wo, Locale.ENGLISH, false);

            assertEquals(7, part.getQuantity());
            verify(partTransactionService).create(any(PartTransaction.class));
            verify(partRepository).save(part);
            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.PART_QUANTITY_CHANGED),
                    anyMap(), eq("changedPart"), eq(showDto), isNull(), isNull(), isNull(), isNull(),
                    eq(Collections.singletonList(PartField.QUANTITY)));
        }

        @Test
        void consumeBelowMinWithLowStockAlerts_createsNotifications() {
            Part part = buildPart(1L);
            part.setQuantity(10);
            part.setMinQuantity(20);
            WorkOrder wo = buildWorkOrder();
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));
            when(partRepository.save(part)).thenReturn(part);
            when(licenseService.hasEntitlement(LicenseEntitlement.LOW_STOCK_ALERTS)).thenReturn(true);
            Role settingsRole = buildRole(new HashSet<>(Collections.singletonList(PermissionEntity.SETTINGS)),
                    new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
            User worker = buildUser(3L);
            worker.setRole(settingsRole);
            when(userService.findWorkersByCompany(1L)).thenReturn(Collections.singletonList(worker));
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            stubMessageKeys();
            stubShowDto();

            partService.consumePart(1L, 3, wo, Locale.ENGLISH, false);

            verify(notificationService).createMultiple(anyList(), eq(true), anyString());
            verify(mailService).sendMessageUsingThymeleafTemplate(any(String[].class), anyString(), anyMap(),
                    eq("low-stock.html"), any(Locale.class), isNull());
        }

        @Test
        void consumeZero_deletesLatestTransaction() {
            Part part = buildPart(1L);
            part.setQuantity(10);
            WorkOrder wo = buildWorkOrder();
            PartTransaction transaction = new PartTransaction(part, wo, 2);
            transaction.setId(3L);
            transaction.setCreatedAt(new Date(0L));
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));
            when(partTransactionService.findByWorkOrderAndPart(10L, 1L))
                    .thenReturn(Collections.singletonList(transaction));

            partService.consumePart(1L, 0, wo, Locale.ENGLISH, false);

            verify(partTransactionService).delete(3L);
            verify(partRepository, never()).save(any());
        }

        @Test
        void consumeNegativeWithoutDelete_increasesTransactionQuantity() {
            Part part = buildPart(1L);
            part.setQuantity(10);
            WorkOrder wo = buildWorkOrder();
            PartTransaction transaction = new PartTransaction(part, wo, 2);
            transaction.setId(3L);
            transaction.setCreatedAt(new Date(0L));
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));
            when(partTransactionService.findByWorkOrderAndPart(10L, 1L))
                    .thenReturn(Collections.singletonList(transaction));
            when(partRepository.save(part)).thenReturn(part);
            stubShowDto();

            partService.consumePart(1L, -1, wo, Locale.ENGLISH, false);

            assertEquals(1, transaction.getQuantity());
            verify(partTransactionService).save(transaction);
            verify(partTransactionService, never()).delete(anyLong());
        }
    }

    @Nested
    class DeleteAndAccess {

        @Test
        void delete_dispatchesWebhookAndDeletes() {
            Part part = buildPart(1L);
            stubShowDto();

            partService.delete(part);

            verify(webhookDispatchService).dispatchWebhook(eq(company), eq(WebhookEvent.PART_DELETE), anyMap(),
                    eq("deletePart"), eq(showDto), isNull(), isNull(), isNull(), isNull(), isNull());
            verify(partRepository).deleteById(1L);
        }

        @Test
        void getById_notFound_throwsNotFound() {
            when(partRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> partService.getById(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void getById_accessDenied_throwsForbidden() {
            user.setRole(restrictedRole());
            when(partRepository.findById(1L)).thenReturn(Optional.of(buildPart(1L)));

            CustomException ex = assertThrows(CustomException.class, () -> partService.getById(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void getById_returnsPart() {
            Part part = buildPart(1L);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));

            assertSame(part, partService.getById(1L, user));
        }

        @Test
        void deleteByIdAndUser_notFound_throwsNotFound() {
            when(partRepository.findById(1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_forbidden_throwsForbidden() {
            user.setRole(restrictedRole());
            Part part = buildPart(1L);
            part.setCreatedBy(999L);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.deleteByIdAndUser(1L, user));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void deleteByIdAndUser_deletes() {
            Part part = buildPart(1L);
            when(partRepository.findById(1L)).thenReturn(Optional.of(part));
            stubShowDto();

            partService.deleteByIdAndUser(1L, user);

            verify(partRepository).deleteById(1L);
        }

        @Test
        void getMini_returnsCompanyParts() {
            List<Part> parts = Collections.singletonList(buildPart(1L));
            when(partRepository.findByCompany_Id(1L)).thenReturn(parts);

            assertSame(parts, partService.getMini(user));
        }
    }

    @Nested
    class GetSearchCriteria {

        @Test
        void clientWithViewOther_filtersByCompanyOnly() {
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = partService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(1, result.getFilterFields().size());
            assertEquals("company", result.getFilterFields().get(0).getField());
        }

        @Test
        void clientWithoutViewOther_filtersByCompanyAndCreatedBy() {
            Set<PermissionEntity> partsOnly =
                    new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS));
            user.setRole(buildRole(partsOnly, new HashSet<>(), partsOnly, new HashSet<>(), new HashSet<>()));
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = partService.getSearchCriteria(user, criteria);

            assertEquals(2, result.getFilterFields().size());
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("company")));
            assertTrue(result.getFilterFields().stream().anyMatch(f -> f.getField().equals("createdBy")));
        }

        @Test
        void clientWithoutPermission_throwsForbidden() {
            user.setRole(restrictedRole());

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.getSearchCriteria(user, new SearchCriteria()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void superAdmin_returnsCriteriaUnchanged() {
            Role superAdmin = buildRole(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(),
                    new HashSet<>());
            superAdmin.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            user.setRole(superAdmin);
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = partService.getSearchCriteria(user, criteria);

            assertSame(criteria, result);
            assertEquals(0, result.getFilterFields().size());
        }
    }

    @Nested
    class FindBySearchCriteria {

        @Test
        void withoutFilters_buildsNullSpecification() {
            SearchCriteria criteria = new SearchCriteria();
            Page<Part> page = new PageImpl<>(Collections.singletonList(buildPart(1L)));
            when(partRepository.findAll((Specification<Part>) isNull(), any(Pageable.class))).thenReturn(page);

            Page<Part> result = partService.findBySearchCriteria(criteria);

            assertSame(page, result);
        }

        @Test
        void withFilters_buildsSpecification() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.getFilterFields().add(FilterField.builder()
                    .field("name")
                    .value("Part")
                    .operation("eq")
                    .build());
            Page<Part> page = new PageImpl<>(Collections.singletonList(buildPart(1L)));
            when(partRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<Part> result = partService.findBySearchCriteria(criteria);

            assertSame(page, result);
        }
    }

    @Nested
    class ImportPart {

        @Test
        void overFreeLimit_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PARTS)).thenReturn(false);
            when(partRepository.hasMoreThan(eq(1L), anyLong())).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.importPart(new Part(), new PartImportDTO(), company));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void setsFieldsAndAssociations() {
            stubPartLimit(false, false);
            Part part = new Part();
            PartImportDTO dto = PartImportDTO.builder()
                    .name("Imported")
                    .cost(25.5)
                    .category("Cat")
                    .description("Desc")
                    .quantity(4)
                    .additionalInfos("Infos")
                    .area("Area")
                    .minQuantity(2)
                    .assignedToEmails(Collections.singletonList("a@test.com"))
                    .teamsNames(Collections.singletonList("Team"))
                    .customersNames(Collections.singletonList("Customer"))
                    .vendorsNames(Collections.singletonList("Vendor"))
                    .build();
            PartCategory category = new PartCategory();
            when(partCategoryService.getOrCreate("Cat", companySettings)).thenReturn(category);
            User assigned = buildUser(7L);
            when(userService.findByEmailAndCompany("a@test.com", 1L)).thenReturn(Optional.of(assigned));
            Team team = new Team();
            when(teamService.findByNameIgnoreCaseAndCompany("Team", 1L)).thenReturn(Optional.of(team));
            Customer customer = new Customer();
            when(customerService.findByNameIgnoreCaseAndCompany("Customer", 1L)).thenReturn(Optional.of(customer));
            Vendor vendor = new Vendor();
            when(vendorService.findByNameIgnoreCaseAndCompany("Vendor", 1L)).thenReturn(Optional.of(vendor));

            partService.importPart(part, dto, company);

            assertEquals("Imported", part.getName());
            assertEquals(25.5, part.getCost());
            assertSame(category, part.getCategory());
            assertEquals(4, part.getQuantity());
            assertEquals(2, part.getMinQuantity());
            assertEquals(Collections.singletonList(assigned), part.getAssignedTo());
            assertEquals(Collections.singletonList(team), part.getTeams());
            assertEquals(Collections.singletonList(customer), part.getCustomers());
            assertEquals(Collections.singletonList(vendor), part.getVendors());
        }

        @Test
        void withoutCategory_skipsCategoryLookup() {
            stubPartLimit(false, false);
            PartImportDTO dto = PartImportDTO.builder().name("Imported").build();

            partService.importPart(new Part(), dto, company);

            verify(partCategoryService, never()).getOrCreate(anyString(), any());
        }

        @Test
        void duplicateBarcodeOnNewPart_throwsNotAcceptable() {
            stubPartLimit(false, false);
            PartImportDTO dto = PartImportDTO.builder().name("Imported").barcode("BAR").build();
            when(partRepository.findByBarcodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildPart(2L)));

            CustomException ex = assertThrows(CustomException.class,
                    () -> partService.importPart(new Part(), dto, company));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void duplicateBarcodeOnSamePart_isAllowed() {
            stubPartLimit(false, false);
            PartImportDTO dto = PartImportDTO.builder().name("Imported").barcode("BAR").id(1L).build();
            when(partRepository.findByBarcodeAndCompany_Id("BAR", 1L)).thenReturn(Optional.of(buildPart(1L)));

            assertDoesNotThrow(() -> partService.importPart(new Part(), dto, company));
        }
    }

    @Nested
    class DetectPatchDTOChangedFields {

        @SuppressWarnings("unchecked")
        private Collection<PartField> detect(Part original, PartPatchDTO dto) {
            return (Collection<PartField>) ReflectionTestUtils.invokeMethod(partService,
                    "detectPatchDTOChangedFields", original, dto);
        }

        private Part original() {
            Part p = buildPart(1L);
            p.setName("Name");
            p.setCost(10);
            p.setBarcode("BAR");
            p.setDescription("Desc");
            p.setAdditionalInfos("Infos");
            p.setArea("Area");
            p.setMinQuantity(5);
            p.setUnit("unit");
            return p;
        }

        private PartPatchDTO matchingPatch() {
            PartPatchDTO dto = new PartPatchDTO();
            dto.setName("Name");
            dto.setCost(10);
            dto.setBarcode("BAR");
            dto.setDescription("Desc");
            dto.setAdditionalInfos("Infos");
            dto.setArea("Area");
            dto.setMinQuantity(5);
            dto.setUnit("unit");
            return dto;
        }

        @Test
        void noChanges_returnsEmpty() {
            assertTrue(detect(original(), matchingPatch()).isEmpty());
        }

        @Test
        void nameChanged_detected() {
            PartPatchDTO dto = matchingPatch();
            dto.setName("Other");
            assertTrue(detect(original(), dto).contains(PartField.NAME));
        }

        @Test
        void costChanged_detected() {
            PartPatchDTO dto = matchingPatch();
            dto.setCost(99);
            assertTrue(detect(original(), dto).contains(PartField.COST));
        }

        @Test
        void categoryChanged_detected() {
            Part original = original();
            PartCategory oldCategory = new PartCategory();
            oldCategory.setId(1L);
            original.setCategory(oldCategory);
            PartPatchDTO dto = matchingPatch();
            PartCategory newCategory = new PartCategory();
            newCategory.setId(2L);
            dto.setCategory(newCategory);

            assertTrue(detect(original, dto).contains(PartField.CATEGORY));
        }

        @Test
        void nonStockChanged_detected() {
            PartPatchDTO dto = matchingPatch();
            dto.setNonStock(true);
            assertTrue(detect(original(), dto).contains(PartField.NON_STOCK));
        }

        @Test
        void barcodeDescriptionAreaAdditionalInfosUnitChanged_detected() {
            PartPatchDTO dto = matchingPatch();
            dto.setBarcode("OTHER");
            dto.setDescription("Other");
            dto.setArea("Other");
            dto.setAdditionalInfos("Other");
            dto.setUnit("other");

            Collection<PartField> changed = detect(original(), dto);

            assertTrue(changed.contains(PartField.BARCODE));
            assertTrue(changed.contains(PartField.DESCRIPTION));
            assertTrue(changed.contains(PartField.AREA));
            assertTrue(changed.contains(PartField.ADDITIONAL_INFOS));
            assertTrue(changed.contains(PartField.UNIT));
        }

        @Test
        void quantityChanged_detected() {
            Part original = original();
            original.setQuantity(1);
            PartPatchDTO dto = matchingPatch();
            dto.setQuantity(5);
            assertTrue(detect(original, dto).contains(PartField.QUANTITY));
        }

        @Test
        void minQuantityChanged_detected() {
            PartPatchDTO dto = matchingPatch();
            dto.setMinQuantity(50);
            assertTrue(detect(original(), dto).contains(PartField.MIN_QUANTITY));
        }

        @Test
        void collectionsChanged_detected() {
            Part original = original();
            original.setAssignedTo(new ArrayList<>(Collections.singletonList(buildUser(1L))));
            original.setCustomers(new ArrayList<>(Collections.singletonList(customer(1L))));
            original.setVendors(new ArrayList<>(Collections.singletonList(vendor(1L))));
            original.setTeams(new ArrayList<>(Collections.singletonList(team(1L))));

            PartPatchDTO dto = matchingPatch();
            dto.setAssignedTo(new ArrayList<>(Collections.singletonList(buildUser(2L))));
            dto.setCustomers(new ArrayList<>(Collections.singletonList(customer(2L))));
            dto.setVendors(new ArrayList<>(Collections.singletonList(vendor(2L))));
            dto.setTeams(new ArrayList<>(Collections.singletonList(team(2L))));

            Collection<PartField> changed = detect(original, dto);

            assertTrue(changed.contains(PartField.ASSIGNED_TO));
            assertTrue(changed.contains(PartField.CUSTOMERS));
            assertTrue(changed.contains(PartField.VENDORS));
            assertTrue(changed.contains(PartField.TEAMS));
        }

        @Test
        void sameCollections_notDetected() {
            Part original = original();
            User u = buildUser(1L);
            original.setAssignedTo(new ArrayList<>(Collections.singletonList(u)));
            original.setCustomers(new ArrayList<>(Collections.singletonList(customer(1L))));
            original.setVendors(new ArrayList<>(Collections.singletonList(vendor(1L))));
            original.setTeams(new ArrayList<>(Collections.singletonList(team(1L))));

            PartPatchDTO dto = matchingPatch();
            dto.setAssignedTo(new ArrayList<>(Collections.singletonList(buildUser(1L))));
            dto.setCustomers(new ArrayList<>(Collections.singletonList(customer(1L))));
            dto.setVendors(new ArrayList<>(Collections.singletonList(vendor(1L))));
            dto.setTeams(new ArrayList<>(Collections.singletonList(team(1L))));

            Collection<PartField> changed = detect(original, dto);

            assertFalse(changed.contains(PartField.ASSIGNED_TO));
            assertFalse(changed.contains(PartField.CUSTOMERS));
            assertFalse(changed.contains(PartField.VENDORS));
            assertFalse(changed.contains(PartField.TEAMS));
        }

        @Test
        void sizeMismatch_detected() {
            Part original = original();
            original.setAssignedTo(new ArrayList<>(Collections.singletonList(buildUser(1L))));

            PartPatchDTO dto = matchingPatch();
            dto.setAssignedTo(new ArrayList<>(Arrays.asList(buildUser(1L), buildUser(2L))));

            assertTrue(detect(original, dto).contains(PartField.ASSIGNED_TO));
        }

        @Test
        void nullOriginalCollection_detected() {
            Part original = original();
            original.setAssignedTo(null);

            PartPatchDTO dto = matchingPatch();
            dto.setAssignedTo(new ArrayList<>(Collections.singletonList(buildUser(1L))));

            assertTrue(detect(original, dto).contains(PartField.ASSIGNED_TO));
        }

        private Customer customer(Long id) {
            Customer c = new Customer();
            c.setId(id);
            return c;
        }

        private Vendor vendor(Long id) {
            Vendor v = new Vendor();
            v.setId(id);
            return v;
        }

        private Team team(Long id) {
            Team t = new Team();
            t.setId(id);
            return t;
        }
    }
}
