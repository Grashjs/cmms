package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.*;
import com.grash.dto.comment.CommentCriteria;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.workOrder.WorkOrderPatchDTO;
import com.grash.dto.imports.WorkOrderImportDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.workOrder.WorkOrderSendReportDTO;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.factory.MailServiceFactory;
import com.grash.model.*;
import com.grash.model.abstracts.Cost;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.model.enums.workflow.WFMainCondition;

import com.grash.repository.WorkOrderRepository;
import com.grash.utils.Helper;
import com.grash.utils.MultipartFileImpl;
import com.grash.utils.TenantAspectUtils;
import com.grash.utils.Utils;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ITagWorkerFactory;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.DefaultTagWorkerFactory;
import com.itextpdf.styledxmlparser.node.IElementNode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.grash.utils.Consts.usageBasedLicenseLimits;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final LocationService locationService;
    private final CustomerService customerService;
    private final TeamService teamService;
    private final AssetService assetService;
    private final UserService userService;
    private final CompanyService companyService;
    private LaborService laborService;
    private AdditionalCostService additionalCostService;
    private PartQuantityService partQuantityService;
    private final NotificationService notificationService;
    private final WorkOrderMapper workOrderMapper;
    private final EntityManager em;
    private final MailServiceFactory mailServiceFactory;
    private final WorkOrderCategoryService workOrderCategoryService;
    private WorkflowService workflowService;
    private final MessageSource messageSource;
    private final CustomSequenceService customSequenceService;

    @Value("${frontend.url}")
    private String frontendUrl;
    private final LicenseService licenseService;
    private WebhookDispatchService webhookDispatchService;
    private final CustomFieldValueService customFieldValueService;
    private final IntercomService intercomService;
    private final ReviewEligibilityService reviewEligibilityService;
    private final BrandingService brandingService;
    private final Utils utils;
    private final WorkOrderHistoryService workOrderHistoryService;
    private final SpringTemplateEngine thymeleafTemplateEngine;
    private final StorageServiceFactory storageServiceFactory;
    private final Environment environment;
    private final ResourceBundleMessageSource emailMessageSource;
    private TaskService taskService;
    private RelationService relationService;
    private CommentService commentService;
    private ScheduleService scheduleService;
    private PreventiveMaintenanceService preventiveMaintenanceService;
    private PreventiveMaintenanceMapper preventiveMaintenanceMapper;

    @Transactional
    public WorkOrder create(WorkOrder workOrder, Company company) {
        checkUsageBasedLimit(company);
        if (workOrder instanceof WorkOrderPostDTO workOrderPostDTO) {
            workOrder = workOrderMapper.fromPostDto(workOrderPostDTO);
            workOrder.setCustomFieldValues(new ArrayList<>());
            if (workOrderPostDTO.getAsset() != null && workOrderPostDTO.getAssetStatus() != null) {
                Asset asset = assetService.findById(workOrderPostDTO.getAsset().getId()).get();
                asset.setStatus(workOrderPostDTO.getAssetStatus());
                assetService.save(asset);
            }
            if (!workOrderPostDTO.getCustomFields().isEmpty()) {
                setWOCustomFields(workOrder, workOrderPostDTO.getCustomFields(), company);
            }
        }
        workOrder.setCustomId(getWorkOrderNumber(company));
        workOrder.setId(null);

        WorkOrder savedWorkOrder = workOrderRepository.saveAndFlush(workOrder);
        em.refresh(savedWorkOrder);
        notify(savedWorkOrder, Helper.getLocale(company));
        Collection<Workflow> workflows =
                workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CREATED, company.getId());
        workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, savedWorkOrder));
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("workOrderId", savedWorkOrder.getId());
        Object serializedWorkOrder = workOrderMapper.toShowDto(savedWorkOrder);
        webhookDispatchService.dispatchWebhook(company, WebhookEvent.NEW_WORK_ORDER, webhookPayload,
                "newWorkOrder", serializedWorkOrder, null, null, null, null, null);
        return savedWorkOrder;
    }

    private void setWOCustomFields(WorkOrder workOrder, List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
                                   Company company) {
        customFieldValueService.setCustomFields(
                workOrder,
                workOrder.getCustomFieldValues(),
                customFieldValuePostDTOS,
                company,
                CustomFieldEntityType.WORK_ORDER,
                cfv -> cfv.setWorkOrder(workOrder)
        );
    }

    public String getWorkOrderNumber(Company company) {
        Long nextSequence = customSequenceService.getNextWorkOrderSequence(company);
        return "WO" + String.format("%06d", nextSequence);
    }

    @Autowired
    public void setDeps(@Lazy LaborService laborService,
                        @Lazy AdditionalCostService additionalCostService,
                        @Lazy PartQuantityService partQuantityService,
                        @Lazy TaskService taskService,
                        @Lazy RelationService relationService,
                        @Lazy CommentService commentService,
                        @Lazy ScheduleService scheduleService,
                        @Lazy PreventiveMaintenanceService preventiveMaintenanceService,
                        @Lazy PreventiveMaintenanceMapper preventiveMaintenanceMapper,
                        @Lazy WorkflowService workflowService) {
        this.laborService = laborService;
        this.additionalCostService = additionalCostService;
        this.partQuantityService = partQuantityService;
        this.taskService = taskService;
        this.relationService = relationService;
        this.commentService = commentService;
        this.scheduleService = scheduleService;
        this.preventiveMaintenanceService = preventiveMaintenanceService;
        this.preventiveMaintenanceMapper = preventiveMaintenanceMapper;
        this.workflowService = workflowService;
    }

    private void checkUsageBasedLimit(Company company) {
        Integer threshold = usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS);
        if (!licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_ACTIVE_WORK_ORDERS)
                && workOrderRepository.hasMoreActiveThan(company.getId(), threshold.longValue() - 1
        ))
            throw new CustomException("You need a license to add a new work order. Free Limit of " + threshold + " " +
                    "incomplete " +
                    "work orders reached",
                    HttpStatus.FORBIDDEN);
    }

    @Transactional
    protected WorkOrder update(Long id, WorkOrderPatchDTO workOrder, User user) {
        if (workOrderRepository.existsById(id)) {
            WorkOrder savedWorkOrder = workOrderRepository.findById(id).get();
            if (savedWorkOrder.getFirstTimeToReact() == null) savedWorkOrder.setFirstTimeToReact(new Date());

            Collection<WOField> changedFields = detectPatchDTOChangedFields(savedWorkOrder, workOrder);
            Long previousCategoryId = savedWorkOrder.getCategory() != null ? savedWorkOrder.getCategory().getId() :
                    null;

            WorkOrder newWorkOrder = workOrderMapper.updateWorkOrder(savedWorkOrder, workOrder);
            if (!workOrder.getCustomFields().isEmpty()) {
                setWOCustomFields(newWorkOrder, workOrder.getCustomFields(), user.getCompany());
            }
            WorkOrder updatedWorkOrder =
                    workOrderRepository.saveAndFlush(newWorkOrder);
            em.refresh(updatedWorkOrder);
            Object serializedWorkOrder = workOrderMapper.toShowDto(updatedWorkOrder);
            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("workOrderId", updatedWorkOrder.getId());
            webhookPayload.put("workOrderTitle", updatedWorkOrder.getTitle());
            webhookDispatchService.dispatchWebhook(user.getCompany(), WebhookEvent.WORK_ORDER_CHANGE, webhookPayload,
                    "changedWorkOrder", serializedWorkOrder, changedFields, null, null, null, null);

            Long newCategoryId = updatedWorkOrder.getCategory() != null ? updatedWorkOrder.getCategory().getId() : null;
            if ((previousCategoryId == null && newCategoryId != null) ||
                    (previousCategoryId != null && !previousCategoryId.equals(newCategoryId))) {
                webhookPayload.put("previousCategoryId", previousCategoryId);
                webhookPayload.put("newCategoryId", newCategoryId);
                webhookPayload.put("newCategoryName", updatedWorkOrder.getCategory() != null ?
                        updatedWorkOrder.getCategory().getName() : null);
                WorkOrderCategory newCategory = updatedWorkOrder.getCategory();
                Collection<WorkOrderCategory> categories = newCategory != null ?
                        Collections.singletonList(newCategory) : Collections.emptyList();
                webhookDispatchService.dispatchWebhook(user.getCompany(), WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER,
                        webhookPayload,
                        "changedWorkOrder", serializedWorkOrder, changedFields, null, null, categories, null);
            }

            return updatedWorkOrder;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }


    @Transactional
    public WorkOrder patch(Long id, WorkOrderPatchDTO workOrder, User user) {
        Optional<WorkOrder> optionalWorkOrder = findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (savedWorkOrder.canBeEditedBy(user)) {
                em.detach(savedWorkOrder);
                WorkOrder patchedWorkOrder = update(id, workOrder, user);

                if (patchedWorkOrder.isArchived() && !savedWorkOrder.isArchived()) {
                    Collection<Workflow> workflows =
                            workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_ARCHIVED,
                                    user.getCompany().getId());
                    workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, patchedWorkOrder));
                }

                boolean shouldNotify =
                        !user.getCompany().getCompanySettings().getGeneralPreferences().isDisableClosedWorkOrdersNotif() || !patchedWorkOrder.getStatus().equals(Status.COMPLETE);
                if (shouldNotify)
                    patchNotify(savedWorkOrder, patchedWorkOrder, Helper.getLocale(user));
                return patchedWorkOrder;
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    public Collection<WorkOrder> getAll() {
        return workOrderRepository.findAll();
    }

    @Transactional
    protected void delete(WorkOrder workOrder, Company company) {
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("workOrderId", workOrder.getId());
        webhookPayload.put("workOrderTitle", workOrder.getTitle());
        Object serializedWorkOrder = workOrderMapper.toShowDto(workOrder);
        webhookDispatchService.dispatchWebhook(company, WebhookEvent.WORK_ORDER_DELETE, webhookPayload,
                "deleteWorkOrder", serializedWorkOrder, null, null, null, null, null);
        workOrderRepository.deleteById(workOrder.getId());
    }

    public Optional<WorkOrder> findById(Long id) {
        return workOrderRepository.findById(id);
    }

    public WorkOrder checkAccessToWorkOrderId(Long workOrderId, User user) {
        WorkOrder workOrder = findById(workOrderId).orElseThrow(() -> new CustomException("Work Order not found",
                HttpStatus.NOT_FOUND));
        if (!workOrder.isAccessibleBy(user))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        return workOrder;
    }

    public Optional<WorkOrder> findByIdAndCompany(Long id, Long companyId) {
        return workOrderRepository.findByIdAndCompany_Id(id, companyId);
    }

    public Collection<WorkOrder> findByIdsAndCompany(List<Long> ids, Long companyId) {
        return workOrderRepository.findByIdInAndCompany_Id(ids, companyId);
    }

    public Collection<WorkOrder> findByCompany(Long id) {
        return workOrderRepository.findByCompany_Id(id);
    }

    public Page<WorkOrder> findByCompanyForExport(Long companyId, Pageable pageable) {
        return workOrderRepository.findByCompanyForExport(companyId, pageable);
    }

    public void notify(WorkOrder workOrder, Locale locale) {
        String title = messageSource.getMessage("new_wo", null, locale);
        String message = messageSource.getMessage("notification_wo_assigned", new Object[]{workOrder.getTitle()},
                locale);
        Collection<User> users = workOrder.getUsers();
        notificationService.createMultiple(users.stream().map(user -> new Notification(message, user,
                NotificationType.WORK_ORDER, workOrder.getId())).collect(Collectors.toList()), true, title);

        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
            put("workOrderLink", frontendUrl + "/app/work-orders/" + workOrder.getId());
            put("workOrderTitle", workOrder.getTitle());
        }};
        Collection<User> usersToMail =
                users.stream().filter(user -> user.isEnabled() && user.getUserSettings().shouldEmailUpdatesForWorkOrders()).collect(Collectors.toList());
        if (!usersToMail.isEmpty()) {
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(User::getEmail).toArray(String[]::new), messageSource.getMessage("new_wo", null, locale), mailVariables, "new-work-order.html", Helper.getLocale(users.stream().findFirst().get()));
        }
    }

    public void patchNotify(WorkOrder oldWorkOrder, WorkOrder newWorkOrder, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_wo_assigned", new Object[]{newWorkOrder.getTitle()},
                Helper.getLocale(newWorkOrder.getCompany()));
        List<User> usersToNotify = oldWorkOrder.getNewUsersToNotify(newWorkOrder.getUsers());
        notificationService.createMultiple(usersToNotify.stream().map(user ->
                new Notification(message, user, NotificationType.WORK_ORDER, newWorkOrder.getId())).collect(Collectors.toList()), true, title);

        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
            put("workOrderLink", frontendUrl + "/app/work-orders/" + newWorkOrder.getId());
            put("workOrderTitle", newWorkOrder.getTitle());
        }};
        Collection<User> usersToMail =
                usersToNotify.stream().filter(user -> user.isEnabled() && user.getUserSettings().shouldEmailUpdatesForWorkOrders()).collect(Collectors.toList());
        if (!usersToMail.isEmpty()) {
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(User::getEmail).toArray(String[]::new), messageSource.getMessage("new_wo", null, locale), mailVariables, "new-work-order.html", Helper.getLocale(usersToMail.stream().findFirst().get()));
        }
    }

    public Collection<WorkOrder> findByAsset(Long id) {
        return workOrderRepository.findByAsset_Id(id);
    }

    public Collection<WorkOrder> findByAssetAndCreatedAtBetween(Long id, Date start, Date end) {
        return workOrderRepository.findByAsset_IdAndCreatedAtBetween(id, start, end);
    }

    public Page<WorkOrder> findLastByPM(Long id, int count) {
        return workOrderRepository.findByParentPreventiveMaintenance_Id(id, PageRequest.of(0, count,
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public Collection<WorkOrder> findByLocation(Long id) {
        return workOrderRepository.findByLocation_Id(id);
    }

    public Page<WorkOrder> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<WorkOrder> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return workOrderRepository.findAll(builder.build(), page);
    }

    public WorkOrder save(WorkOrder workOrder) {
        return workOrderRepository.save(workOrder);
    }

    public List<WorkOrder> saveAll(List<WorkOrder> workOrders) {
        return workOrderRepository.saveAll(workOrders);
    }


    @Transactional
    public WorkOrder saveAndFlushWithWebhook(WorkOrder workOrder, Company company, WorkOrder originalWorkOrder) {
        Collection<WOField> changedFields = detectChangedFieldsFromEntity(originalWorkOrder, workOrder);
        boolean statusChanged = !Objects.equals(originalWorkOrder.getStatus(), workOrder.getStatus());
        Long originalCategoryId = originalWorkOrder.getCategory() != null ? originalWorkOrder.getCategory().getId() :
                null;
        Long newCategoryId = workOrder.getCategory() != null ? workOrder.getCategory().getId() : null;
        boolean categoryChanged = !Objects.equals(originalCategoryId, newCategoryId);
        WorkOrder updatedWorkOrder = workOrderRepository.saveAndFlush(workOrder);
        em.refresh(updatedWorkOrder);
        Object serializedWorkOrder = workOrderMapper.toShowDto(updatedWorkOrder);
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("workOrderId", updatedWorkOrder.getId());
        webhookPayload.put("workOrderTitle", updatedWorkOrder.getTitle());
        webhookPayload.put("previousStatus", originalWorkOrder.getStatus());
        webhookPayload.put("newStatus", updatedWorkOrder.getStatus());

        webhookDispatchService.dispatchWebhook(company, WebhookEvent.WORK_ORDER_CHANGE, webhookPayload,
                "changedWorkOrder", serializedWorkOrder, changedFields, null, null, null, null);

        if (statusChanged) {
            webhookDispatchService.dispatchWebhook(company, WebhookEvent.WORK_ORDER_STATUS_CHANGE, webhookPayload,
                    "changedWorkOrder", serializedWorkOrder, changedFields, null,
                    updatedWorkOrder.getStatus(), null, null);
        }

        if (categoryChanged) {
            webhookPayload.put("previousCategoryId", originalCategoryId);
            webhookPayload.put("newCategoryId", newCategoryId);
            webhookPayload.put("newCategoryName", updatedWorkOrder.getCategory() != null ?
                    updatedWorkOrder.getCategory().getName() : null);
            WorkOrderCategory newCategory = updatedWorkOrder.getCategory();
            Collection<WorkOrderCategory> categories = newCategory != null ? Collections.singletonList(newCategory) :
                    Collections.emptyList();
            webhookDispatchService.dispatchWebhook(company, WebhookEvent.NEW_CATEGORY_ON_WORK_ORDER, webhookPayload,
                    "changedWorkOrder", serializedWorkOrder, changedFields, null, null, categories, null);
        }

        return updatedWorkOrder;
    }

    public WorkOrderPostDTO getWorkOrderFromWorkOrderBase(WorkOrderBase workOrderBase) {
        WorkOrderPostDTO workOrder = new WorkOrderPostDTO();
        workOrder.setTitle(workOrderBase.getTitle());
        workOrder.setDescription(workOrderBase.getDescription());
        workOrder.setPriority(workOrderBase.getPriority());
        workOrder.setImage(workOrderBase.getImage());
        workOrder.setCompany(workOrderBase.getCompany());
        workOrder.getFiles().addAll(workOrderBase.getFiles());
        workOrder.setAsset(workOrderBase.getAsset());
        workOrder.setLocation(workOrderBase.getLocation());
        workOrder.setPrimaryUser(workOrderBase.getPrimaryUser());
        workOrder.setTeam(workOrderBase.getTeam());
        workOrder.setCategory(workOrderBase.getCategory());
        workOrder.setRequiredSignature(workOrderBase.isRequiredSignature());
        workOrder.getAssignedTo().addAll(workOrderBase.getAssignedTo());
        workOrder.setEstimatedDuration(workOrderBase.getEstimatedDuration());
        workOrder.getCustomFieldValues().addAll(workOrderBase.getCustomFieldValues());
        workOrder.setCustomFields(workOrderBase.getCustomFieldValues().stream().map(customFieldValue -> {
            CustomFieldValuePostDTO customFieldValuePostDTO = new CustomFieldValuePostDTO();
            customFieldValuePostDTO.setId(customFieldValue.getCustomField().getId());
            customFieldValuePostDTO.setValue(customFieldValue.getValue());
            return customFieldValuePostDTO;
        }).collect(Collectors.toList()));
        return workOrder;
    }

    public Collection<WorkOrder> findByAssignedToUser(Long id) {
        return workOrderRepository.findByAssignedToUser(id);
    }

    public Collection<WorkOrder> findByCompletedBy(Long id) {
        return workOrderRepository.findByCompletedBy_Id(id);
    }

    public Collection<WorkOrder> findByPriorityAndCompanyAndCreatedAtBetween(Priority priority, Long companyId,
                                                                             Date start, Date end) {
        return workOrderRepository.findByPriorityAndCompany_IdAndCreatedAtBetween(priority, companyId, start, end);
    }

    public Collection<WorkOrder> findByCategoryAndCreatedAtBetween(Long id, Date start, Date end) {
        return workOrderRepository.findByCategory_IdAndCreatedAtBetween(id, start, end);
    }

    public Collection<WorkOrder> findByCompletedOnBetweenAndCompany(Date date1, Date date2, Long companyId) {
        return workOrderRepository.findByCompletedOnBetweenAndCompany_Id(date1, date2, companyId);
    }

    public Pair<Long, Long> getLaborCostAndTime(Collection<WorkOrder> workOrders) {
        Collection<Long> laborCostsArray = new ArrayList<>();
        Collection<Long> laborTimesArray = new ArrayList<>();
        workOrders.forEach(workOrder -> {
                    Collection<Labor> labors = laborService.findByWorkOrder(workOrder.getId());
                    long laborsCosts =
                            labors.stream().mapToLong(labor -> labor.getHourlyRate() * labor.getDuration() / 3600).sum();
                    long laborTimes = labors.stream().mapToLong(Labor::getDuration).sum();
                    laborCostsArray.add(laborsCosts);
                    laborTimesArray.add(laborTimes);
                }
        );
        long laborCost = laborCostsArray.stream().mapToLong(value -> value).sum();
        long laborTimes = laborTimesArray.stream().mapToLong(value -> value).sum();

        return Pair.of(laborCost, laborTimes);
    }

    public double getAdditionalCost(Collection<WorkOrder> workOrders) {
        Collection<Double> costs = workOrders.stream().map(workOrder -> {
                    Collection<AdditionalCost> additionalCosts =
                            additionalCostService.findByWorkOrder(workOrder.getId());
                    return additionalCosts.stream().mapToDouble(Cost::getCost).sum();
                }
        ).collect(Collectors.toList());
        return costs.stream().mapToDouble(value -> value).sum();
    }

    public double getPartCost(Collection<WorkOrder> workOrders) {
        Collection<Double> costs = workOrders.stream().map(workOrder -> {
                    Collection<PartQuantity> partQuantities = partQuantityService.findByWorkOrder(workOrder.getId());
                    return partQuantities.stream().mapToDouble(partQuantity -> partQuantity.getPart().getCost() * partQuantity.getQuantity()).sum();
                }
        ).collect(Collectors.toList());
        return costs.stream().mapToDouble(value -> value).sum();
    }

    public double getAllCost(Collection<WorkOrder> workOrders, boolean includeLaborCost) {
        return getPartCost(workOrders) + getAdditionalCost(workOrders) + (includeLaborCost ?
                getLaborCostAndTime(workOrders).getFirst() : 0);
    }

    public Collection<WorkOrder> findByCreatedBy(Long id) {
        return workOrderRepository.findByCreatedBy(id);
    }

    public Collection<WorkOrder> findByDueDateBetweenAndCompany(Date date1, Date date2, Long id) {
        return workOrderRepository.findByDueDateBetweenAndCompany_Id(date1, date2, id);
    }

    public void importWorkOrder(WorkOrder workOrder, WorkOrderImportDTO dto, Company company) {
        checkUsageBasedLimit(company);
        Helper.populateWorkOrderBaseFromImportDTO(workOrder, dto, company, locationService, teamService, userService,
                assetService, workOrderCategoryService);
        workOrder.setCompany(company);
        workOrder.setDueDate(Helper.getDateFromExcelDate(dto.getDueDate()));
        workOrder.setCustomId(getWorkOrderNumber(company));
        workOrder.setRequiredSignature(Helper.getBooleanFromString(dto.getRequiredSignature()));

        Optional<User> optionalCompletedBy = userService.findByEmailAndCompany(dto.getCompletedByEmail(),
                company.getId());
        optionalCompletedBy.ifPresent(workOrder::setCompletedBy);
        workOrder.setCompletedOn(dto.getCompletedOn() == null ? null : Helper.addSeconds(new Date(), 60 * 10));
        workOrder.setArchived(Helper.getBooleanFromString(dto.getArchived()));
        workOrder.setStatus(Status.getStatusFromString(dto.getStatus()));
        workOrder.setFeedback(dto.getFeedback());
        List<Customer> customers = new ArrayList<>();
        dto.getCustomersNames().forEach(name -> {
            Optional<Customer> optionalCustomer = customerService.findByNameIgnoreCaseAndCompany(name, company.getId());
            optionalCustomer.ifPresent(customers::add);
        });
        workOrder.setCustomers(customers);
    }

    public Collection<WorkOrder> findByCreatedByAndCreatedAtBetween(Long id, Date date1, Date date2) {
        return workOrderRepository.findByCreatedByAndCreatedAtBetween(id, date1, date2);
    }

    public Collection<WorkOrder> findByCompletedByAndCreatedAtBetween(Long id, Date date1, Date date2) {
        return workOrderRepository.findByCompletedBy_IdAndCreatedAtBetween(id, date1, date2);
    }

    public SearchCriteria getSearchCriteria(User user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (!user.getSuperAccountRelations().isEmpty()) {
                List<Long> childCompanyIds = user.getSuperAccountRelations().stream()
                        .map(rel -> rel.getChildUser().getCompany().getId())
                        .distinct()
                        .toList();
                searchCriteria.getFilterFields().add(FilterField.builder()
                        .field("company")
                        .operation("inm")
                        .joinType(JoinType.LEFT)
                        .value("")
                        .values(new ArrayList<>(childCompanyIds))
                        .build());
            } else {
                searchCriteria.filterCompany(user);
            }
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)) {
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS);
                if (!canViewOthers) {
                    searchCriteria.getFilterFields().add(FilterField.builder()
                            .field("createdBy")
                            .value(user.getId())
                            .operation("eq")
                            .values(new ArrayList<>())
                            .alternatives(Arrays.asList(
                                    FilterField.builder()
                                            .field("assignedTo")
                                            .operation("inm")
                                            .joinType(JoinType.LEFT)
                                            .value("")
                                            .values(Collections.singletonList(user.getId())).build(),
                                    FilterField.builder()
                                            .field("primaryUser")
                                            .operation("eq")
                                            .value(user.getId())
                                            .values(Collections.singletonList(user.getId())).build(),
                                    FilterField.builder()
                                            .field("team")
                                            .operation("in")
                                            .value("")
                                            .values(teamService.findByUser(user.getId()).stream().map(Team::getId).collect(Collectors.toList())).build()
                            )).build());
                } else if (searchCriteria.getFilterFields().stream().anyMatch(filterField -> filterField.getField().equals("assignedToUser"))) {
                    searchCriteria.getFilterFields().add(
                            FilterField.builder()
                                    .field("assignedTo")
                                    .operation("inm")
                                    .joinType(JoinType.LEFT)
                                    .value("")
                                    .values(Collections.singletonList(user.getId()))
                                    .alternatives(
                                            Arrays.asList(FilterField.builder()
                                                            .field("primaryUser")
                                                            .operation("eq")
                                                            .value(user.getId())
                                                            .values(Collections.singletonList(user.getId())).build(),
                                                    FilterField.builder()
                                                            .field("team")
                                                            .operation("in")
                                                            .value("")
                                                            .values(teamService.findByUser(user.getId()).stream().map(Team::getId).collect(Collectors.toList())).build()

                                            )).build());
                }

            } else if (user.getRole().getCode().equals(RoleCode.REQUESTER)) {
                searchCriteria.getFilterFields().add(FilterField.builder()
                        .field("parentRequest.createdBy")
                        .value(user.getId())
                        .operation("eq")
                        .values(new ArrayList<>()).build());
            }
            searchCriteria.getFilterFields().
                    removeIf(filterField -> filterField.getField().equals("assignedToUser"));
//            else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN); //Work order is viewed by everyone
        }
        return searchCriteria;
    }

    public Integer countUrgent(User user) {
        SpecificationBuilder<WorkOrder> builder = new SpecificationBuilder<>();
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.getFilterFields().addAll(Arrays.asList(FilterField.builder()
                        .field("dueDate")
                        .value(Helper.addSeconds(new Date(), 2 * 24 * 3600))
                        .operation("le").build(),
                FilterField.builder().field("status")
                        .value(Status.COMPLETE)
                        .operation("ne")
                        .build()));
        searchCriteria = getSearchCriteria(user, searchCriteria);
        searchCriteria.getFilterFields().forEach(builder::with);
        return Math.toIntExact(workOrderRepository.count(builder.build()));
    }

    public Collection<WorkOrder> findByCompanyAndCreatedAtBetween(Long id, Date start, Date end) {
        return workOrderRepository.findByCompany_IdAndCreatedAtBetween(id, start, end);
    }

    public List<Object[]> findTopNAssetsByIncompleteWO(Long companyId, Date start, Date end, int limit) {
        return workOrderRepository.findTopNAssetsByIncompleteWO(companyId, start, end, limit);
    }

    public List<Object[]> findTopNAssetsTimeCost(Long companyId, Date start, Date end, int limit) {
        return workOrderRepository.findTopNAssetsTimeCost(companyId, start, end, limit);
    }

    public List<Object[]> findWOCostsByDateRange(Long companyId, Date start, Date end) {
        return workOrderRepository.findWOCostsByDateRange(companyId, start, end);
    }

    public List<Object[]> findTopNAssetsRepairTime(Long companyId, Date start, Date end, int limit) {
        return workOrderRepository.findTopNAssetsRepairTime(companyId, start, end, limit);
    }

    public List<Object[]> findTotalWOCosts(Long companyId, Date start, Date end) {
        return workOrderRepository.findTotalWOCosts(companyId, start, end);
    }

    public Collection<WorkOrder> findByAssignedToUserAndCreatedAtBetween(Long id, Date start, Date end) {
        return workOrderRepository.findByAssignedToUserAndCreatedAtBetween(id, start, end);
    }

    @Autowired
    public void setWebhookDispatchService(WebhookDispatchService webhookDispatchService) {
        this.webhookDispatchService = webhookDispatchService;
    }

    private Collection<WOField> detectPatchDTOChangedFields(WorkOrder original, WorkOrderPatchDTO patch) {
        Collection<WOField> changedFields = new ArrayList<>();

        if (!Objects.equals(
                patch.getAsset() != null ? patch.getAsset().getId() : null,
                original.getAsset() != null ? original.getAsset().getId() : null)) {
            changedFields.add(WOField.ASSET);
        }
        if (!collectionsMatch(patch.getAssignedTo(), original.getAssignedTo(), User::getId)) {
            changedFields.add(WOField.ASSIGNEES);
        }
        if (!Objects.equals(
                patch.getCategory() != null ? patch.getCategory().getId() : null,
                original.getCategory() != null ? original.getCategory().getId() : null)) {
            changedFields.add(WOField.CATEGORY);
        }
        if (!Objects.equals(patch.getDescription(), original.getDescription())) {
            changedFields.add(WOField.DESCRIPTION);
        }
        if (!Objects.equals(patch.getDueDate(), original.getDueDate())) {
            changedFields.add(WOField.DUE_DATE);
        }
        if (!Objects.equals(patch.getEstimatedDuration(), original.getEstimatedDuration())) {
            changedFields.add(WOField.ESTIMATED_DURATION);
        }
        if (!Objects.equals(
                patch.getLocation() != null ? patch.getLocation().getId() : null,
                original.getLocation() != null ? original.getLocation().getId() : null)) {
            changedFields.add(WOField.LOCATION);
        }
        if (!Objects.equals(patch.getPriority(), original.getPriority())) {
            changedFields.add(WOField.PRIORITY);
        }
        if (!Objects.equals(patch.getTitle(), original.getTitle())) {
            changedFields.add(WOField.TITLE);
        }
        if (!Objects.equals(
                patch.getTeam() != null ? patch.getTeam().getId() : null,
                original.getTeam() != null ? original.getTeam().getId() : null)) {
            changedFields.add(WOField.TEAM);
        }
        if (!collectionsMatch(patch.getCustomers(), original.getCustomers(), Customer::getId)) {
            changedFields.add(WOField.CUSTOMERS);
        }

        return changedFields;
    }

    private Collection<WOField> detectChangedFieldsFromEntity(WorkOrder original, WorkOrder updated) {
        Collection<WOField> changedFields = new ArrayList<>();

        if (!Objects.equals(
                original.getAsset() != null ? original.getAsset().getId() : null,
                updated.getAsset() != null ? updated.getAsset().getId() : null)) {
            changedFields.add(WOField.ASSET);
        }
        if (!collectionsMatch(original.getAssignedTo(), updated.getAssignedTo(), User::getId)) {
            changedFields.add(WOField.ASSIGNEES);
        }
        if (!Objects.equals(
                original.getCategory() != null ? original.getCategory().getId() : null,
                updated.getCategory() != null ? updated.getCategory().getId() : null)) {
            changedFields.add(WOField.CATEGORY);
        }
        if (!Objects.equals(original.getDescription(), updated.getDescription())) {
            changedFields.add(WOField.DESCRIPTION);
        }
        if (!Objects.equals(original.getDueDate(), updated.getDueDate())) {
            changedFields.add(WOField.DUE_DATE);
        }
        if (original.getEstimatedDuration() != updated.getEstimatedDuration()) {
            changedFields.add(WOField.ESTIMATED_DURATION);
        }
        if (!Objects.equals(
                original.getLocation() != null ? original.getLocation().getId() : null,
                updated.getLocation() != null ? updated.getLocation().getId() : null)) {
            changedFields.add(WOField.LOCATION);
        }
        if (!Objects.equals(original.getPriority(), updated.getPriority())) {
            changedFields.add(WOField.PRIORITY);
        }
        if (!Objects.equals(original.getTitle(), updated.getTitle())) {
            changedFields.add(WOField.TITLE);
        }
        if (!Objects.equals(
                original.getTeam() != null ? original.getTeam().getId() : null,
                updated.getTeam() != null ? updated.getTeam().getId() : null)) {
            changedFields.add(WOField.TEAM);
        }
        if (!Objects.equals(original.getStatus(), updated.getStatus())) {
            changedFields.add(WOField.STATUS);
        }
        if (!collectionsMatch(original.getCustomers(), updated.getCustomers(), Customer::getId)) {
            changedFields.add(WOField.CUSTOMERS);
        }

        return changedFields;
    }

    private <T> boolean collectionsMatch(Collection<T> a, Collection<T> b, Function<T, Long> idExtractor) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        return a.stream().allMatch(aItem ->
                b.stream().anyMatch(bItem ->
                        idExtractor.apply(bItem).equals(idExtractor.apply(aItem))));
    }

    public Page<WorkOrder> findByCompany(Long id, Pageable pageable) {
        return workOrderRepository.findByCompany_Id(id, pageable);
    }

    public Page<WorkOrder> findByCompanyWithTimeAndCost(Long id, Pageable pageable) {
        return workOrderRepository.findByCompany_IdWithTimeAndCost(id, pageable);
    }

    @Transactional
    public WorkOrder changeStatus(WorkOrderChangeStatusDTO dto, Long id, User user, String platform) {
        Optional<WorkOrder> optionalWorkOrder = findById(id);
        WorkOrder savedWorkOrder = optionalWorkOrder.get();
        em.detach(savedWorkOrder); // detach FIRST
        WorkOrder originalWorkOrder = savedWorkOrder;
        WorkOrder mutableWO = findById(id).get(); // fresh managed copy

        if (mutableWO.getFirstTimeToReact() == null && !dto.getStatus().equals(Status.ON_HOLD))
            mutableWO.setFirstTimeToReact(new Date());
        Status savedWorkOrderStatusBefore = mutableWO.getStatus();

        if (dto.getStatus() == null) throw new CustomException("Status can't be null", HttpStatus.NOT_ACCEPTABLE);
        if (dto.getSignature() != null && !licenseService.hasEntitlement(LicenseEntitlement.SIGNATURE_CAPTURE))
            throw new CustomException("You need a license to add signature to work order",
                    HttpStatus.FORBIDDEN);
        mutableWO.setSignature(dto.getSignature());
        mutableWO.setStatus(dto.getStatus());
        mutableWO.setFeedback(dto.getFeedback());

        if (dto.getStatus() != Status.COMPLETE) {
            mutableWO.setCompletedOn(null);
            mutableWO.setCompletedBy(null);
        }
        if (mutableWO.canBeEditedBy(user) && (dto.getSignature() == null ||
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE))) {
            if (!dto.getStatus().equals(Status.IN_PROGRESS)) {
                if (dto.getStatus().equals(Status.COMPLETE)) {
                    mutableWO.setCompletedBy(user);
                    mutableWO.setCompletedOn(new Date());
                    if (mutableWO.getAsset() != null) {
                        Asset asset = mutableWO.getAsset();
                        Collection<WorkOrder> workOrdersOfSameAsset = findByAsset(asset.getId());
                        if (workOrdersOfSameAsset.stream().noneMatch(workOrder1 -> !workOrder1.getId().equals(id) && !workOrder1.getStatus().equals(Status.COMPLETE))) {
                            assetService.stopDownTime(asset.getId(), Helper.getLocale(user));
                        }
                    }
                    if (mutableWO.getParentPreventiveMaintenance() != null)
                        scheduleService.scheduleNextWorkOrderJobAfterCompletion(mutableWO.getParentPreventiveMaintenance().getSchedule().getId(), mutableWO.getCompletedOn());
                }
                Collection<Labor> labors = laborService.findByWorkOrder(id);
                Collection<Labor> primaryTimes = labors.stream().filter(Labor::isLogged).collect(Collectors.toList());
                primaryTimes.forEach(laborService::stop);
            }
            WorkOrder patchedWorkOrder = saveAndFlushWithWebhook(mutableWO, user.getCompany(),
                    originalWorkOrder);

            if (patchedWorkOrder.getStatus().equals(Status.COMPLETE) && !savedWorkOrderStatusBefore.equals(Status.COMPLETE)) {
                List<User> admins =
                        userService.findWorkersByCompany(user.getCompany().getId()).stream().filter(ownUser -> ownUser.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) && ownUser.isEnabled() && ownUser.getUserSettings().shouldEmailUpdatesForWorkOrders()).collect(Collectors.toList());
                notificationService.createMultiple(admins.stream().map(admin -> new Notification(messageSource.getMessage("complete_work_order_content", new String[]{patchedWorkOrder.getTitle(), user.getFullName()}, Helper.getLocale(admin)), admin,
                                NotificationType.WORK_ORDER, id)).collect(Collectors.toList()), true,
                        messageSource.getMessage("complete_work_order", null, Helper.getLocale(user)));
                Collection<Workflow> workflows =
                        workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CLOSED,
                                user.getCompany().getId());
                workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, patchedWorkOrder));

                if ("ios".equalsIgnoreCase(platform) || "android".equalsIgnoreCase(platform)) {
                    reviewEligibilityService.incrementWorkOrder(reviewEligibilityService.getOrCreate(user));
                }
            }
            if (user.getCompany().getCompanySettings().getGeneralPreferences().isWoUpdateForRequesters()
                    && savedWorkOrderStatusBefore != patchedWorkOrder.getStatus()
                    && patchedWorkOrder.getParentRequest() != null) {
                Long requesterId = patchedWorkOrder.getParentRequest().getCreatedBy();
                String requesterEmail = null;
                User requester = null;
                if (requesterId == null) {
                    String contact = patchedWorkOrder.getParentRequest().getContact();
                    if (contact != null && Helper.isValidEmailAddress(contact)) {
                        requesterEmail = contact;
                    }
                } else {
                    requester = userService.findById(requesterId).get();
                    requesterEmail = requester.getEmail();
                }
                Locale locale = Helper.getLocale(user);
                String message = messageSource.getMessage("notification_wo_request",
                        new Object[]{patchedWorkOrder.getTitle(),
                                messageSource.getMessage(patchedWorkOrder.getStatus().toString(), null, locale)},
                        locale);
                if (requester != null) {
                    notificationService.create(new Notification(message, requester,
                            NotificationType.WORK_ORDER, id));
                }
                if ((requester != null && requester.getUserSettings().shouldEmailUpdatesForRequests() && requester.isEnabled()) || requesterEmail != null) {
                    Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                        put("workOrderLink", frontendUrl + "/app/work-orders/" + id);
                        put("message", message);
                    }};
                    mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(new String[]{requesterEmail},
                            messageSource.getMessage("request_update", null, locale), mailVariables, "requester" +
                                    "-update.html", Helper.getLocale(user), null);
                }
            }
            return patchedWorkOrder;
        } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
    }

    public byte[] generatePdfBytes(WorkOrder savedWorkOrder, User user, ReportConfig config) {
        StorageService storageService = storageServiceFactory.getStorageService();
        Context thymeleafContext = new Context();
        thymeleafContext.setLocale(Helper.getLocale(user));
        Optional<User> creator = savedWorkOrder.getCreatedBy() == null ? Optional.empty() :
                userService.findById(savedWorkOrder.getCreatedBy());
        List<Task> tasks = taskService.findByWorkOrder(savedWorkOrder.getId());
        Map<Long, String[]> tasksImagesUrls = tasks.stream()
                .collect(Collectors.toMap(
                        Task::getId,
                        task -> task.getImages().stream()
                                .map(image -> storageService.generateSignedUrl(image, 5))
                                .toArray(String[]::new)
                ));
        Collection<PartQuantity> partQuantities = config.isCost() ?
                partQuantityService.findByWorkOrder(savedWorkOrder.getId()) :
                Collections.emptyList();
        Collection<Labor> labors = config.isCost() ? laborService.findByWorkOrder(savedWorkOrder.getId()) :
                Collections.emptyList();
        Collection<Relation> relations = config.isRelations() ?
                relationService.findByWorkOrder(savedWorkOrder.getId()) :
                Collections.emptyList();
        Collection<AdditionalCost> additionalCosts = config.isCost() ?
                additionalCostService.findByWorkOrder(savedWorkOrder.getId()) : Collections.emptyList();
        Collection<WorkOrderHistory> workOrderHistories = config.isWorkOrderHistory() ?
                workOrderHistoryService.findByWorkOrder(savedWorkOrder.getId()) : Collections.emptyList();
        List<Comment> comments = config.isComments() ? commentService.findByCriteria(
                new CommentCriteria() {{
                    setWorkOrderId(savedWorkOrder.getId());
                }}, user) : Collections.emptyList();
        Map<Long, String[]> commentFilesUrls = comments.stream()
                .collect(Collectors.toMap(
                        Comment::getId,
                        comment -> comment.getFiles().stream()
                                .map(file -> storageService.generateSignedUrl(file, 5))
                                .toArray(String[]::new)
                ));
        String[] workOrderFilesUrls = config.isFiles() ? savedWorkOrder.getFiles().stream()
                .map(file -> storageService.generateSignedUrl(file, 5))
                .toArray(String[]::new) : new String[0];
        Map<String, Object> variables = new HashMap<String, Object>() {{
            put("companyName", user.getCompany().getName());
            put("companyPhone", user.getCompany().getPhone());
            put("companyLogo", user.getCompany().getLogo() == null ? null :
                    storageService.generateSignedUrl(user.getCompany().getLogo(), 5));
            put("currency",
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCurrency().getCode());
            put("utils", utils);
            put("dateFormat", user.getCompany().getCompanySettings().getGeneralPreferences().getDateFormat());
            put("timeZone", user.getCompany().getCompanySettings().getGeneralPreferences().getTimeZone());
            put("assignedTo",
                    Helper.enumerate(savedWorkOrder.getAssignedTo().stream().map(User::getFullName).collect(Collectors.toList())));
            put("customers",
                    Helper.enumerate(savedWorkOrder.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())));
            put("workOrder", savedWorkOrder);
            put("primaryUserName", savedWorkOrder.getPrimaryUser() == null ? null :
                    savedWorkOrder.getPrimaryUser().getFullName());
            put("createdBy", creator.<Object>map(User::getFullName).orElse(null));
            put("tasks", tasks);
            put("labors", labors);
            put("relations", relations);
            put("additionalCosts", additionalCosts);
            put("workOrderHistories", workOrderHistories);
            put("partQuantities", partQuantities);
            put("environment", environment);
            put("tasksImagesUrls", tasksImagesUrls);
            put("messageSource", messageSource);
            put("locale", Helper.getLocale(user));
            String companyColor = user.getCompany().getCompanySettings().getGeneralPreferences().getColor();
            put("backgroundColor", companyColor != null && !companyColor.isBlank() ? companyColor :
                    brandingService.getMailBackgroundColor()
            );
            put("reportConfig", config);
            put("comments", comments);
            put("commentFilesUrls", commentFilesUrls);
            put("workOrderFilesUrls", workOrderFilesUrls);
            put("workOrderImageUrl", savedWorkOrder.getImage() == null ? null :
                    storageService.generateSignedUrl(savedWorkOrder.getImage(), 5));
        }};
        thymeleafContext.setVariables(variables);

        String reportHtml = thymeleafTemplateEngine.process("work-order-report.html", thymeleafContext);

        ConverterProperties converterProperties = new ConverterProperties()
                .setTagWorkerFactory(new ITagWorkerFactory() {
                    private final DefaultTagWorkerFactory defaultFactory = new DefaultTagWorkerFactory();

                    @Override
                    public ITagWorker getTagWorker(IElementNode tag, ProcessorContext context) {
                        try {
                            return defaultFactory.getTagWorker(tag, context);
                        } catch (Exception e) {
                            log.warn("Failed to create tag worker for <{}>: {}", tag.name(), e.getMessage());
                            return null;
                        }
                    }
                });
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(reportHtml, target, converterProperties);
        return target.toByteArray();
    }

    public String generateReport(Long id, User user, ReportConfig config) {
        Optional<WorkOrder> optionalWorkOrder = findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || user.getId().equals(savedWorkOrder.getCreatedBy()) || savedWorkOrder.isAssignedTo(user))) {
                byte[] bytes = generatePdfBytes(savedWorkOrder, user, config);
                MultipartFile file = new MultipartFileImpl(bytes, "Work Order Report.pdf");
                return storageServiceFactory.getStorageService().uploadAndSign(file,
                        "reports/" + user.getCompany().getId());
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }


    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<WorkOrder> optionalWorkOrder = findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (
                    user.getId().equals(savedWorkOrder.getCreatedBy()) ||
                            user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
                Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                    put("workOrdersLink", frontendUrl + "/app/work-orders");
                    put("workOrderTitle", savedWorkOrder.getTitle());
                    put("deleter", user.getFullName());
                }};
                String title = messageSource.getMessage("deleted_wo", null, Helper.getLocale(user));

                List<User> usersToMail =
                        userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole()
                                        .getViewPermissions().contains(PermissionEntity.SETTINGS))
                                .filter(user1 -> user1.isEnabled() && user1.getUserSettings().isEmailNotified()).toList();

                mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(User::getEmail)
                                .toArray(String[]::new), title, mailVariables, "deleted-work-order.html",
                        Helper.getLocale(user), null);

                delete(savedWorkOrder, user.getCompany());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public WorkOrder createWithIntercom(WorkOrderPostDTO workOrderReq, User user) {
        WorkOrder createdWorkOrder = create(workOrderReq, user.getCompany());

        if (!user.getCompany().isFirstWorkOrderCreated()) {
            user.getCompany().setFirstWorkOrderCreated(true);
            companyService.update(user.getCompany());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("work_order_id", createdWorkOrder.getId());
            metadata.put("work_order_title", createdWorkOrder.getTitle());
            intercomService.createCompanyActivationEvent(
                    "first-work-order-created",
                    user.getCompany().getId(),
                    user.getEmail(),
                    metadata
            );
        }

        return createdWorkOrder;
    }

    public List<WorkOrder> getWorkOrdersByPart(Long partId) {
        Collection<PartQuantity> partQuantities = partQuantityService.findByPart(partId).stream()
                .filter(partQuantity -> partQuantity.getWorkOrder() != null).toList();
        Collection<WorkOrder> workOrders =
                partQuantities.stream().map(PartQuantity::getWorkOrder).toList();
        return workOrders.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparingLong(WorkOrder::getId))),
                ArrayList::new));
    }

    public void sendReport(Long id, WorkOrderSendReportDTO request, User user) {
        Optional<WorkOrder> optionalWorkOrder = findById(id);
        if (optionalWorkOrder.isEmpty()) {
            throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        }
        WorkOrder savedWorkOrder = optionalWorkOrder.get();

        if (!user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) ||
                (!user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                        !user.getId().equals(savedWorkOrder.getCreatedBy()) &&
                        !savedWorkOrder.isAssignedTo(user))) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }

        List<Customer> customersInDb =
                customerService.findByCompanyAndIdIn(user.getCompany().getId(),
                        request.getCustomers().stream().map(Customer::getId).toList());

        List<String> customerEmails = customersInDb.stream()
                .map(Customer::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toList();

        if (customerEmails.isEmpty()) {
            throw new CustomException("No contractors with email addresses found", HttpStatus.BAD_REQUEST);
        }

        ReportConfig config = request.getConfig() != null ? request.getConfig() : new ReportConfig();
        byte[] pdfBytes = generatePdfBytes(savedWorkOrder, user, config);

        Set<String> allRecipients = new HashSet<>(customerEmails);
        allRecipients.add(user.getEmail());

        Locale locale = Helper.getLocale(user);
        String subject = messageSource.getMessage("workOrderReportSubject",
                null, locale);

        String customMessage = request.getMessage() != null ? request.getMessage() : "";
        String messageBody = messageSource.getMessage("workOrderReportBody",
                new Object[]{user.getFullName(), customMessage, savedWorkOrder.getTitle()},
                locale);

        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("messageBody", messageBody);

        List<EmailAttachmentDTO> attachments = Collections.singletonList(
                EmailAttachmentDTO.builder()
                        .attachmentName(emailMessageSource.getMessage("workOrderReport", null, locale) + ".pdf")
                        .attachmentData(pdfBytes)
                        .attachmentType("application/pdf")
                        .build()
        );

        mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(
                allRecipients.toArray(new String[0]),
                subject,
                mailVariables,
                "work-order-report-email.html",
                locale,
                attachments
        );
    }

    public Collection<CalendarEvent<WorkOrderBaseMiniDTO>> getEvents(@Valid DateRange dateRange, Long companyId,
                                                                     User user) {
        if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            return TenantAspectUtils.executeWithDisabledCompanyCheck(() -> {
                List<Long> companyIds = user.getSuperAccountRelations().isEmpty()
                        ? Collections.singletonList(user.getCompany().getId())
                        : user.getSuperAccountRelations().stream()
                        .map(rel -> rel.getChildUser().getCompany().getId())
                        .distinct()
                        .toList();
                if (companyId != null) {
                    if (!companyIds.contains(companyId))
                        throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
                    companyIds = Collections.singletonList(companyId);
                }

                List<CalendarEvent<WorkOrderBaseMiniDTO>> result = new ArrayList<>();
                for (Long compId : companyIds) {
                    result.addAll(preventiveMaintenanceService.getEvents(dateRange.getEnd(), compId).stream()
                            .filter(calendarEvent -> calendarEvent.getDate().after(new Date()))
                            .filter(calendarEvent -> canViewWorkOrderBase(user, calendarEvent.getEvent()))
                            .map(calendarEvent -> new CalendarEvent<>(calendarEvent.getType(),
                                    preventiveMaintenanceMapper.toBaseMiniDto(calendarEvent.getEvent()),
                                    calendarEvent.getDate()))
                            .toList());
                    result.addAll(findByDueDateBetweenAndCompany(dateRange.getStart(),
                            dateRange.getEnd(),
                            compId).stream().filter(workOrder -> canViewWorkOrderBase(user, workOrder)).map(workOrderMapper::toBaseMiniDto).map(workOrderMiniDTO -> new CalendarEvent<>("WORK_ORDER",
                            workOrderMiniDTO, workOrderMiniDTO.getDueDate())).toList());
                }
                return result;
            });
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private boolean canViewWorkOrderBase(User user, WorkOrderBase workOrderBase) {
        boolean canViewOthers =
                user.getRole().getViewOtherPermissions().contains(workOrderBase instanceof PreventiveMaintenance ?
                        PermissionEntity.PREVENTIVE_MAINTENANCES : PermissionEntity.WORK_ORDERS);
        return canViewOthers || (workOrderBase.getCreatedBy() != null && workOrderBase.getCreatedBy().equals(user.getId())) || workOrderBase.isAssignedTo(user);

    }

    public List<File> addFiles(Long workOrderId, List<File> files, User user) {
        Optional<WorkOrder> optionalWorkOrder = findById(workOrderId);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (!savedWorkOrder.canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            savedWorkOrder.getFiles().addAll(files);
            save(savedWorkOrder);
            return savedWorkOrder.getFiles();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public List<File> removeFile(Long workOrderId, Long fileId, User user) {
        Optional<WorkOrder> optionalWorkOrder = findById(workOrderId);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (!savedWorkOrder.canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            savedWorkOrder.getFiles().removeIf(file -> file.getId().equals(fileId));
            save(savedWorkOrder);
            return savedWorkOrder.getFiles();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }
}