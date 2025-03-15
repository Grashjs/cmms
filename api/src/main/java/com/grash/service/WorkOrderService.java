package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.WorkOrderPatchDTO;
import com.grash.dto.imports.WorkOrderImportDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.abstracts.Cost;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.*;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.repository.WorkOrderHistoryRepository;
import com.grash.repository.WorkOrderRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
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
    private final EmailService2 emailService2;
    private final WorkOrderCategoryService workOrderCategoryService;
    private WorkflowService workflowService;
    private final MessageSource messageSource;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    public void setDeps(@Lazy WorkflowService workflowService
    ) {
        this.workflowService = workflowService;
    }

    @Transactional
    public WorkOrder create(WorkOrder workOrder) {
        if (workOrder instanceof WorkOrderPostDTO) {
            WorkOrderPostDTO workOrderPostDTO = (WorkOrderPostDTO) workOrder;
            workOrder = workOrderMapper.fromPostDto(workOrderPostDTO);
            if (workOrderPostDTO.getAsset() != null && workOrderPostDTO.getAssetStatus() != null) {
                Asset asset = assetService.findById(workOrderPostDTO.getAsset().getId()).get();
                asset.setStatus(workOrderPostDTO.getAssetStatus());
                assetService.save(asset);
            }
        }
        WorkOrder savedWorkOrder = workOrderRepository.saveAndFlush(workOrder);
        em.refresh(savedWorkOrder);
        Company company = savedWorkOrder.getCompany();
        notify(savedWorkOrder, Helper.getLocale(company));
        Collection<Workflow> workflows =
                workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CREATED, company.getId());
        workflows.forEach(workflow -> workflowService.runWorkOrder(workflow, savedWorkOrder));

        return savedWorkOrder;
    }

    @Autowired
    public void setDeps(@Lazy LaborService laborService,
                        @Lazy AdditionalCostService additionalCostService,
                        @Lazy PartQuantityService partQuantityService) {
        this.laborService = laborService;
        this.additionalCostService = additionalCostService;
        this.partQuantityService = partQuantityService;
    }

    @Transactional
    public WorkOrder update(Long id, WorkOrderPatchDTO workOrder, OwnUser user) {
        if (workOrderRepository.existsById(id)) {
            WorkOrder savedWorkOrder = workOrderRepository.findById(id).get();
            if (savedWorkOrder.getFirstTimeToReact() == null) savedWorkOrder.setFirstTimeToReact(new Date());
            WorkOrder updatedWorkOrder =
                    workOrderRepository.saveAndFlush(workOrderMapper.updateWorkOrder(savedWorkOrder, workOrder));
            em.refresh(updatedWorkOrder);
            return updatedWorkOrder;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<WorkOrder> getAll() {
        return workOrderRepository.findAll();
    }

    public void delete(Long id) {
        workOrderRepository.deleteById(id);
    }

    public Optional<WorkOrder> findById(Long id) {
        return workOrderRepository.findById(id);
    }

    public Optional<WorkOrder> findByIdAndCompany(Long id, Long companyId) {
        return workOrderRepository.findByIdAndCompany_Id(id, companyId);
    }

    public Collection<WorkOrder> findByCompany(Long id) {
        return workOrderRepository.findByCompany_Id(id);
    }

    public void notify(WorkOrder workOrder, Locale locale) {
        String title = messageSource.getMessage("new_wo", null, locale);
        String message = messageSource.getMessage("notification_wo_assigned", new Object[]{workOrder.getTitle()},
                locale);
        Collection<OwnUser> users = workOrder.getUsers();
        notificationService.createMultiple(users.stream().map(user -> new Notification(message, user,
                NotificationType.WORK_ORDER, workOrder.getId())).collect(Collectors.toList()), true, title);

        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
            put("workOrderLink", frontendUrl + "/app/work-orders/" + workOrder.getId());
            put("featuresLink", frontendUrl + "/#key-features");
            put("workOrderTitle", workOrder.getTitle());
        }};
        Collection<OwnUser> usersToMail =
                users.stream().filter(user -> user.getUserSettings().isEmailUpdatesForWorkOrders()).collect(Collectors.toList());
        if (!usersToMail.isEmpty()) {
            emailService2.sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail).toArray(String[]::new), messageSource.getMessage("new_wo", null, locale), mailVariables, "new-work-order.html", Helper.getLocale(users.stream().findFirst().get()));
        }
    }

    public void patchNotify(WorkOrder oldWorkOrder, WorkOrder newWorkOrder, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_wo_assigned", new Object[]{newWorkOrder.getTitle()},
                Helper.getLocale(newWorkOrder.getCompany()));
        List<OwnUser> usersToNotify = oldWorkOrder.getNewUsersToNotify(newWorkOrder.getUsers());
        notificationService.createMultiple(usersToNotify.stream().map(user ->
                new Notification(message, user, NotificationType.WORK_ORDER, newWorkOrder.getId())).collect(Collectors.toList()), true, title);

        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
            put("workOrderLink", frontendUrl + "/app/work-orders/" + newWorkOrder.getId());
            put("featuresLink", frontendUrl + "/#key-features");
            put("workOrderTitle", newWorkOrder.getTitle());
        }};
        Collection<OwnUser> usersToMail =
                usersToNotify.stream().filter(user -> user.getUserSettings().isEmailUpdatesForWorkOrders()).collect(Collectors.toList());
        if (!usersToMail.isEmpty()) {
            emailService2.sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail).toArray(String[]::new), messageSource.getMessage("new_wo", null, locale), mailVariables, "new-work-order.html", Helper.getLocale(usersToMail.stream().findFirst().get()));
        }
    }

    public Collection<WorkOrder> findByAsset(Long id) {
        return workOrderRepository.findByAsset_Id(id);
    }

    public Collection<WorkOrder> findByAssetAndCreatedAtBetween(Long id, Date start, Date end) {
        return workOrderRepository.findByAsset_IdAndCreatedAtBetween(id, start, end);
    }

    public Page<WorkOrder> findLastByPM(Long id, int count) {
        return workOrderRepository.findByParentPreventiveMaintenance_Id(id, PageRequest.of(0, count));
    }

    public Collection<WorkOrder> findByLocation(Long id) {
        return workOrderRepository.findByLocation_Id(id);
    }

    public Page<WorkOrder> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<WorkOrder> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), "id");
        return workOrderRepository.findAll(builder.build(), page);
    }

    public WorkOrder save(WorkOrder workOrder) {
        return workOrderRepository.save(workOrder);
    }

    public WorkOrder getWorkOrderFromWorkOrderBase(WorkOrderBase workOrderBase) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle(workOrderBase.getTitle());
        workOrder.setDescription(workOrderBase.getDescription());
        workOrder.setPriority(workOrderBase.getPriority());
        workOrder.setImage(workOrder.getImage());
        workOrder.setCompany(workOrderBase.getCompany());
        workOrder.getFiles().addAll(workOrderBase.getFiles());
        workOrder.setAsset(workOrderBase.getAsset());
        workOrder.setLocation(workOrderBase.getLocation());
        workOrder.setPrimaryUser(workOrderBase.getPrimaryUser());
        workOrder.setTeam(workOrderBase.getTeam());
        workOrder.getAssignedTo().addAll(workOrderBase.getAssignedTo());
        workOrder.setEstimatedDuration(workOrder.getEstimatedDuration());
        return workOrder;
    }

    public Collection<WorkOrder> findByPrimaryUser(Long id) {
        return workOrderRepository.findByPrimaryUser_Id(id);
    }

    public Collection<WorkOrder> findByAssignedToUser(Long id) {
        return workOrderRepository.findByAssignedToUser(id);
    }

    public Collection<WorkOrder> findByCompletedBy(Long id) {
        return workOrderRepository.findByCompletedBy_Id(id);
    }

    public Collection<WorkOrder> findByPriorityAndCompany(Priority priority, Long companyId) {
        return workOrderRepository.findByPriorityAndCompany_Id(priority, companyId);
    }

    public Collection<WorkOrder> findByPriorityAndCompanyAndCreatedAtBetween(Priority priority, Long companyId,
                                                                             Date start, Date end) {
        return workOrderRepository.findByPriorityAndCompany_IdAndCreatedAtBetween(priority, companyId, start, end);
    }

    public Collection<WorkOrder> findByCategory(Long id) {
        return workOrderRepository.findByCategory_Id(id);
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

    public long getAdditionalCost(Collection<WorkOrder> workOrders) {
        Collection<Long> costs = workOrders.stream().map(workOrder -> {
                    Collection<AdditionalCost> additionalCosts =
                            additionalCostService.findByWorkOrder(workOrder.getId());
                    return additionalCosts.stream().mapToLong(Cost::getCost).sum();
                }
        ).collect(Collectors.toList());
        return costs.stream().mapToLong(value -> value).sum();
    }

    public long getPartCost(Collection<WorkOrder> workOrders) {
        Collection<Long> costs = workOrders.stream().map(workOrder -> {
                    Collection<PartQuantity> partQuantities = partQuantityService.findByWorkOrder(workOrder.getId());
                    return partQuantities.stream().mapToLong(partQuantity -> partQuantity.getPart().getCost() * partQuantity.getQuantity()).sum();
                }
        ).collect(Collectors.toList());
        return costs.stream().mapToLong(value -> value).sum();
    }

    public long getAllCost(Collection<WorkOrder> workOrders, boolean includeLaborCost) {
        return getPartCost(workOrders) + getAdditionalCost(workOrders) + (includeLaborCost ?
                getLaborCostAndTime(workOrders).getFirst() : 0);
    }

    public Collection<WorkOrder> findByCreatedBy(Long id) {
        return workOrderRepository.findByCreatedBy(id);
    }

    public boolean isWorkOrderInCompany(WorkOrder workOrder, long companyId, boolean optional) {
        if (optional) {
            Optional<WorkOrder> optionalWorkOrder = workOrder == null ? Optional.empty() : findById(workOrder.getId());
            return workOrder == null || (optionalWorkOrder.isPresent() && optionalWorkOrder.get().getCompany().getId().equals(companyId));
        } else {
            Optional<WorkOrder> optionalWorkOrder = findById(workOrder.getId());
            return optionalWorkOrder.isPresent() && optionalWorkOrder.get().getCompany().getId().equals(companyId);
        }
    }

    public Collection<WorkOrder> findByDueDateBetweenAndCompany(Date date1, Date date2, Long id) {
        return workOrderRepository.findByDueDateBetweenAndCompany_Id(date1, date2, id);
    }

    public void importWorkOrder(WorkOrder workOrder, WorkOrderImportDTO dto, Company company) {
        Long companySettingsId = company.getCompanySettings().getId();
        Long companyId = company.getId();
        workOrder.setDueDate(Helper.getDateFromExcelDate(dto.getDueDate()));
        workOrder.setPriority(Priority.getPriorityFromString(dto.getPriority()));
        workOrder.setEstimatedDuration(dto.getEstimatedDuration());
        workOrder.setDescription(dto.getDescription());
        workOrder.setTitle(dto.getTitle());
        workOrder.setRequiredSignature(Helper.getBooleanFromString(dto.getRequiredSignature()));
        Optional<WorkOrderCategory> optionalWorkOrderCategory =
                workOrderCategoryService.findByNameIgnoreCaseAndCompanySettings(dto.getCategory(), companySettingsId);
        optionalWorkOrderCategory.ifPresent(workOrder::setCategory);
        Optional<Location> optionalLocation = locationService.findByNameIgnoreCaseAndCompany(dto.getLocationName(),
                companyId);
        optionalLocation.ifPresent(workOrder::setLocation);
        Optional<Team> optionalTeam = teamService.findByNameIgnoreCaseAndCompany(dto.getTeamName(), companyId);
        optionalTeam.ifPresent(workOrder::setTeam);
        Optional<OwnUser> optionalPrimaryUser = userService.findByEmailAndCompany(dto.getPrimaryUserEmail(), companyId);
        optionalPrimaryUser.ifPresent(workOrder::setPrimaryUser);
        List<OwnUser> assignedTo = new ArrayList<>();
        dto.getAssignedToEmails().forEach(email -> {
            Optional<OwnUser> optionalUser1 = userService.findByEmailAndCompany(email, companyId);
            optionalUser1.ifPresent(assignedTo::add);
        });
        workOrder.setAssignedTo(assignedTo);
        Optional<Asset> optionalAsset = assetService.findByNameIgnoreCaseAndCompany(dto.getAssetName(), companyId);
        optionalAsset.ifPresent(workOrder::setAsset);
        Optional<OwnUser> optionalCompletedBy = userService.findByEmailAndCompany(dto.getCompletedByEmail(), companyId);
        optionalCompletedBy.ifPresent(workOrder::setCompletedBy);
        workOrder.setCompletedOn(dto.getCompletedOn() == null ? null : Helper.addSeconds(new Date(), 60 * 10));
        workOrder.setArchived(Helper.getBooleanFromString(dto.getArchived()));
        workOrder.setStatus(Status.getStatusFromString(dto.getStatus()));
        workOrder.setFeedback(dto.getFeedback());
        List<Customer> customers = new ArrayList<>();
        dto.getCustomersNames().forEach(name -> {
            Optional<Customer> optionalCustomer = customerService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalCustomer.ifPresent(customers::add);
        });
        workOrder.setCustomers(customers);
        workOrderRepository.save(workOrder);
    }

    public Collection<WorkOrder> findByCreatedByAndCreatedAtBetween(Long id, Date date1, Date date2) {
        return workOrderRepository.findByCreatedByAndCreatedAtBetween(id, date1, date2);
    }

    public Collection<WorkOrder> findByCompletedByAndCreatedAtBetween(Long id, Date date1, Date date2) {
        return workOrderRepository.findByCompletedBy_IdAndCreatedAtBetween(id, date1, date2);
    }

    public SearchCriteria getSearchCriteria(OwnUser user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            searchCriteria.filterCompany(user);
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
                    searchCriteria.getFilterFields().
                            removeIf(filterField -> filterField.getField().equals("assignedToUser"));
                }

            } else if (user.getRole().getCode().equals(RoleCode.REQUESTER)) {
                searchCriteria.getFilterFields().add(FilterField.builder()
                        .field("parentRequest.createdBy")
                        .value(user.getId())
                        .operation("eq")
                        .values(new ArrayList<>()).build());
            }
//            else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN); //Work order is viewed by everyone
        }
        return searchCriteria;
    }

    public Integer countUrgent(OwnUser user) {
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

    public Collection<WorkOrder> findByAssignedToUserAndCreatedAtBetween(Long id, Date start, Date end) {
        return workOrderRepository.findByAssignedToUserAndCreatedAtBetween(id, start, end);
    }
}
