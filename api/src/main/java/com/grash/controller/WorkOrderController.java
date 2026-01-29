package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPatchDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.factory.MailServiceFactory;
import com.grash.model.*;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.*;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.*;
import com.grash.utils.Helper;
import com.grash.utils.MultipartFileImpl;
import com.itextpdf.html2pdf.HtmlConverter;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.transaction.Transactional;

import jakarta.validation.Valid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@RestController
@RequestMapping("/work-orders")
@Tag(name = "workOrder")
@RequiredArgsConstructor
@Transactional
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderMapper workOrderMapper;
    private final UserService userService;
    private final MessageSource messageSource;
    private final AssetService assetService;
    private final LocationService locationService;
    private final LaborService laborService;
    private final PartService partService;
    private final FileService fileService;
    private final PartQuantityService partQuantityService;
    private final NotificationService notificationService;
    private final MailServiceFactory mailServiceFactory;
    private final TeamService teamService;
    private final TaskService taskService;
    private final RelationService relationService;
    private final AdditionalCostService additionalCostService;
    private final WorkOrderHistoryService workOrderHistoryService;
    private final SpringTemplateEngine thymeleafTemplateEngine;
    private final StorageServiceFactory storageServiceFactory;
    private final WorkflowService workflowService;
    private final Environment environment;
    private final PreventiveMaintenanceService preventiveMaintenanceService;
    private final EntityManager em;
    private final PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    private final BrandingService brandingService;
    private final ScheduleService scheduleService;
    private final LicenseService licenseService;


    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<WorkOrderShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                         HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return ResponseEntity.ok(workOrderService.findBySearchCriteria(workOrderService.getSearchCriteria(user,
                searchCriteria)).map(workOrderMapper::toShowDto));
    }

    @PostMapping("/search/mini")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<WorkOrderBaseMiniDTO>> searchMini(@RequestBody SearchCriteria searchCriteria,
                                                                 HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return ResponseEntity.ok(workOrderService.findBySearchCriteria(workOrderService.getSearchCriteria(user,
                        searchCriteria))
                .map(workOrderMapper::toBaseMiniDto));
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<CalendarEvent<WorkOrderBaseMiniDTO>> getEvents(@Valid @RequestBody DateRange
                                                                             dateRange, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            List<CalendarEvent<WorkOrderBaseMiniDTO>> result = new ArrayList<>();
            result.addAll(preventiveMaintenanceService.getEvents(dateRange.getEnd(), user.getCompany().getId()).stream()
                    .filter(calendarEvent -> calendarEvent.getDate().after(new Date()))
                    .filter(calendarEvent -> canViewWorkOrderBase(user, calendarEvent.getEvent()))
                    .map(calendarEvent -> new CalendarEvent<>(calendarEvent.getType(),
                            preventiveMaintenanceMapper.toBaseMiniDto(calendarEvent.getEvent()),
                            calendarEvent.getDate()))
                    .collect(Collectors.toList()));
            result.addAll(workOrderService.findByDueDateBetweenAndCompany(dateRange.getStart(), dateRange.getEnd(),
                    user.getCompany().getId()).stream().filter(workOrder -> canViewWorkOrderBase(user, workOrder)).map(workOrderMapper::toBaseMiniDto).map(workOrderMiniDTO -> new CalendarEvent<>("WORK_ORDER",
                    workOrderMiniDTO, workOrderMiniDTO.getDueDate())).collect(Collectors.toList()));
            return result;
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private boolean canViewWorkOrderBase(OwnUser user, WorkOrderBase workOrderBase) {
        boolean canViewOthers =
                user.getRole().getViewOtherPermissions().contains(workOrderBase instanceof PreventiveMaintenance ?
                        PermissionEntity.PREVENTIVE_MAINTENANCES : PermissionEntity.WORK_ORDERS);
        return canViewOthers || (workOrderBase.getCreatedBy() != null && workOrderBase.getCreatedBy().equals(user.getId())) || workOrderBase.isAssignedTo(user);

    }

    @GetMapping("/asset/{id}")
    @PreAuthorize("permitAll()")
    public Collection<WorkOrderShowDTO> getByAsset(@PathVariable("id") Long id,
                                                   HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            return workOrderService.findByAsset(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/location/{id}")
    @PreAuthorize("permitAll()")
    public Collection<WorkOrderShowDTO> getByLocation(@PathVariable("id") Long id,
                                                      HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            return workOrderService.findByLocation(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public WorkOrderShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if ((user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || (savedWorkOrder.getCreatedBy() != null && savedWorkOrder.getCreatedBy().equals(user.getId())) || savedWorkOrder.isAssignedTo(user)))
                    || savedWorkOrder.getParentRequest() != null && savedWorkOrder.getParentRequest().getCreatedBy().equals(user.getId())
            ) {
                return workOrderMapper.toShowDto(savedWorkOrder);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    WorkOrderShowDTO create(@Valid @RequestBody WorkOrderPostDTO
                                    workOrderReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && (workOrderReq.getSignature() == null ||
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE))) {
            if (user.getCompany().getCompanySettings().getGeneralPreferences().isAutoAssignWorkOrders()) {
                OwnUser primaryUser = workOrderReq.getPrimaryUser();
                workOrderReq.setPrimaryUser(primaryUser == null ? user : primaryUser);
            }
            WorkOrder createdWorkOrder = workOrderService.create(workOrderReq, user.getCompany());

            return workOrderMapper.toShowDto(createdWorkOrder);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/part/{id}")
    @PreAuthorize("permitAll()")

    public Collection<WorkOrderShowDTO> getByPart(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            Collection<PartQuantity> partQuantities = partQuantityService.findByPart(id).stream()
                    .filter(partQuantity -> partQuantity.getWorkOrder() != null).collect(Collectors.toList());
            Collection<WorkOrder> workOrders =
                    partQuantities.stream().map(PartQuantity::getWorkOrder).collect(Collectors.toList());
            Collection<WorkOrder> uniqueWorkOrders =
                    workOrders.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(WorkOrder::getId))),
                            ArrayList::new));
            return uniqueWorkOrders.stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public WorkOrderShowDTO patch(@Valid @RequestBody WorkOrderPatchDTO
                                          workOrder, @PathVariable("id") Long id,
                                  HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (savedWorkOrder.canBeEditedBy(user)) {
                em.detach(savedWorkOrder);
                WorkOrder patchedWorkOrder = workOrderService.update(id, workOrder, user);

                if (patchedWorkOrder.isArchived() && !savedWorkOrder.isArchived()) {
                    Collection<Workflow> workflows =
                            workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_ARCHIVED,
                                    user.getCompany().getId());
                    workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, patchedWorkOrder));
                }

                boolean shouldNotify =
                        !user.getCompany().getCompanySettings().getGeneralPreferences().isDisableClosedWorkOrdersNotif() || !patchedWorkOrder.getStatus().equals(Status.COMPLETE);
                if (shouldNotify)
                    workOrderService.patchNotify(savedWorkOrder, patchedWorkOrder, Helper.getLocale(user));
                return workOrderMapper.toShowDto(patchedWorkOrder);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/change-status")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public WorkOrderShowDTO changeStatus(@Valid @RequestBody WorkOrderChangeStatusDTO
                                                 workOrder, @PathVariable("id") Long id,
                                         HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        WorkOrder savedWorkOrder = optionalWorkOrder.get();
        if (savedWorkOrder.getFirstTimeToReact() == null && !workOrder.getStatus().equals(Status.ON_HOLD))
            savedWorkOrder.setFirstTimeToReact(new Date());
        Status savedWorkOrderStatusBefore = savedWorkOrder.getStatus();

        if (workOrder.getStatus() == null) throw new CustomException("Status can't be null", HttpStatus.NOT_ACCEPTABLE);
        if (workOrder.getSignature() != null && !licenseService.hasEntitlement(LicenseEntitlement.SIGNATURE_CAPTURE))
            throw new CustomException("You need a license to add signature to work order",
                    HttpStatus.FORBIDDEN);
        savedWorkOrder.setSignature(workOrder.getSignature());
        savedWorkOrder.setStatus(workOrder.getStatus());
        savedWorkOrder.setFeedback(workOrder.getFeedback());

        if (workOrder.getStatus() != Status.COMPLETE) {
            savedWorkOrder.setCompletedOn(null);
            savedWorkOrder.setCompletedBy(null);
        }
        if (savedWorkOrder.canBeEditedBy(user) && (workOrder.getSignature() == null ||
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE))) {
            if (!workOrder.getStatus().equals(Status.IN_PROGRESS)) {
                if (workOrder.getStatus().equals(Status.COMPLETE)) {
                    savedWorkOrder.setCompletedBy(user);
                    savedWorkOrder.setCompletedOn(new Date());
                    if (savedWorkOrder.getAsset() != null) {
                        Asset asset = savedWorkOrder.getAsset();
                        Collection<WorkOrder> workOrdersOfSameAsset = workOrderService.findByAsset(asset.getId());
                        if (workOrdersOfSameAsset.stream().noneMatch(workOrder1 -> !workOrder1.getId().equals(id) && !workOrder1.getStatus().equals(Status.COMPLETE))) {
                            assetService.stopDownTime(asset.getId(), Helper.getLocale(user));
                        }
                    }
                    if (savedWorkOrder.getParentPreventiveMaintenance() != null)
                        scheduleService.scheduleNextWorkOrderJobAfterCompletion(savedWorkOrder.getParentPreventiveMaintenance().getSchedule().getId(), savedWorkOrder.getCompletedOn());
                }
                Collection<Labor> labors = laborService.findByWorkOrder(id);
                Collection<Labor> primaryTimes = labors.stream().filter(Labor::isLogged).collect(Collectors.toList());
                primaryTimes.forEach(laborService::stop);
            }
            WorkOrder patchedWorkOrder = workOrderService.saveAndFlush(savedWorkOrder);

            if (patchedWorkOrder.getStatus().equals(Status.COMPLETE) && !savedWorkOrderStatusBefore.equals(Status.COMPLETE)) {
                List<OwnUser> admins =
                        userService.findWorkersByCompany(user.getCompany().getId()).stream().filter(ownUser -> ownUser.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) && ownUser.isEnabled() && ownUser.getUserSettings().shouldEmailUpdatesForWorkOrders()).collect(Collectors.toList());
                notificationService.createMultiple(admins.stream().map(admin -> new Notification(messageSource.getMessage("complete_work_order_content", new String[]{patchedWorkOrder.getTitle(), user.getFullName()}, Helper.getLocale(admin)), admin,
                                NotificationType.WORK_ORDER, id)).collect(Collectors.toList()), true,
                        messageSource.getMessage("complete_work_order", null, Helper.getLocale(user)));
                Collection<Workflow> workflows =
                        workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CLOSED,
                                user.getCompany().getId());
                workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, patchedWorkOrder));
            }
            if (user.getCompany().getCompanySettings().getGeneralPreferences().isWoUpdateForRequesters()
                    && savedWorkOrderStatusBefore != patchedWorkOrder.getStatus()
                    && patchedWorkOrder.getParentRequest() != null) {
                Long requesterId = patchedWorkOrder.getParentRequest().getCreatedBy();
                OwnUser requester = userService.findById(requesterId).get();
                Locale locale = Helper.getLocale(user);
                String message = messageSource.getMessage("notification_wo_request",
                        new Object[]{patchedWorkOrder.getTitle(),
                                messageSource.getMessage(patchedWorkOrder.getStatus().toString(), null, locale)},
                        locale);
                notificationService.create(new Notification(message, userService.findById(requesterId).get(),
                        NotificationType.WORK_ORDER, id));
                if (requester.getUserSettings().shouldEmailUpdatesForRequests() && requester.isEnabled()) {
                    Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                        put("workOrderLink", frontendUrl + "/app/work-orders/" + id);
                        put("message", message);
                    }};
                    mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(new String[]{requester.getEmail()},
                            messageSource.getMessage("request_update", null, locale), mailVariables, "requester" +
                                    "-update.html", Helper.getLocale(user), null);
                }
            }
            return workOrderMapper.toShowDto(patchedWorkOrder);
        } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (
                    user.getId().equals(savedWorkOrder.getCreatedBy()) ||
                            user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
                Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                    put("featuresLink", frontendUrl + "/#key-features");
                    put("workOrdersLink", frontendUrl + "/app/work-orders");
                    put("workOrderTitle", savedWorkOrder.getTitle());
                    put("deleter", user.getFullName());
                }};
                String title = messageSource.getMessage("deleted_wo", null, Helper.getLocale(user));

                List<OwnUser> usersToMail =
                        userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole()
                                        .getViewPermissions().contains(PermissionEntity.SETTINGS))
                                .filter(user1 -> user1.isEnabled() && user1.getUserSettings().isEmailNotified()).collect(Collectors.toList());

                mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail)
                                .toArray(String[]::new), title, mailVariables, "deleted-work-order.html",
                        Helper.getLocale(user), null);

                workOrderService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "/report/{id}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<?> getPDF(@PathVariable("id") Long id, HttpServletRequest req,
                                    HttpServletResponse response) throws IOException {
        OwnUser user = userService.whoami(req);
        StorageService storageService = storageServiceFactory.getStorageService();
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || user.getId().equals(savedWorkOrder.getCreatedBy()) || savedWorkOrder.isAssignedTo(user))) {
                Context thymeleafContext = new Context();
                thymeleafContext.setLocale(Helper.getLocale(user));
                Optional<OwnUser> creator = savedWorkOrder.getCreatedBy() == null ? Optional.empty() :
                        userService.findById(savedWorkOrder.getCreatedBy());
                List<Task> tasks = taskService.findByWorkOrder(id);
                Map<Long, String[]> tasksImagesUrls = tasks.stream()
                        .collect(Collectors.toMap(
                                Task::getId,
                                task -> task.getImages().stream()
                                        .map(image -> storageService.generateSignedUrl(image, 5))
                                        .toArray(String[]::new)
                        ));
                Collection<PartQuantity> partQuantities = partQuantityService.findByWorkOrder(id);
                Collection<Labor> labors = laborService.findByWorkOrder(id);
                Collection<Relation> relations = relationService.findByWorkOrder(id);
                Collection<AdditionalCost> additionalCosts = additionalCostService.findByWorkOrder(id);
                Collection<WorkOrderHistory> workOrderHistories = workOrderHistoryService.findByWorkOrder(id);
                Map<String, Object> variables = new HashMap<String, Object>() {{
                    put("companyName", user.getCompany().getName());
                    put("companyPhone", user.getCompany().getPhone());
                    put("currency",
                            user.getCompany().getCompanySettings().getGeneralPreferences().getCurrency().getCode());
                    put("assignedTo",
                            Helper.enumerate(savedWorkOrder.getAssignedTo().stream().map(OwnUser::getFullName).collect(Collectors.toList())));
                    put("customers",
                            Helper.enumerate(savedWorkOrder.getCustomers().stream().map(Customer::getName).collect(Collectors.toList())));
                    put("workOrder", savedWorkOrder);
                    put("primaryUserName", savedWorkOrder.getPrimaryUser() == null ? null :
                            savedWorkOrder.getPrimaryUser().getFullName());
                    put("createdBy", creator.<Object>map(OwnUser::getFullName).orElse(null));
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
                    put("backgroundColor", brandingService.getMailBackgroundColor());
                }};
                thymeleafContext.setVariables(variables);

                String reportHtml = thymeleafTemplateEngine.process("work-order-report.html", thymeleafContext);

                /* Setup Source and target I/O streams */
                ByteArrayOutputStream target = new ByteArrayOutputStream();
                /* Call convert method */
                HtmlConverter.convertToPdf(reportHtml, target);
                /* extract output as bytes */
                byte[] bytes = target.toByteArray();
                MultipartFile file = new MultipartFileImpl(bytes, "Work Order Report.pdf");
                return ResponseEntity.ok()
                        .body(new SuccessResponse(true, storageServiceFactory.getStorageService().uploadAndSign(file,
                                "reports/" + user.getCompany().getId())));
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);

    }

    @GetMapping("/urgent")
    @PreAuthorize("permitAll()")
    public SuccessResponse getUrgentCount(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT) && user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
            return new SuccessResponse(true, workOrderService.countUrgent(user).toString());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/files/{id}/add")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<File> addFilesToWorkOrder(@PathVariable("id") Long id, @RequestBody List<File> files,
                                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (!savedWorkOrder.canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            savedWorkOrder.getFiles().addAll(files);
            workOrderService.save(savedWorkOrder);
            return savedWorkOrder.getFiles();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/files/{id}/{fileId}/remove")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<File> removeFileFromWorkOrder(@PathVariable("id") Long id,
                                              @PathVariable("fileId") Long fileId, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (!savedWorkOrder.canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            savedWorkOrder.getFiles().removeIf(file -> file.getId().equals(fileId));
            workOrderService.save(savedWorkOrder);
            return savedWorkOrder.getFiles();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

}



