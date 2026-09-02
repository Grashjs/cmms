package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.CalendarEvent;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenancePostDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.imports.PreventiveMaintenanceImportDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.*;
import com.grash.model.enums.*;

import com.grash.repository.PreventiveMaintenanceRepository;
import com.grash.utils.Helper;
import com.grash.utils.Sanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.grash.utils.Consts.usageBasedFreeLimits;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreventiveMaintenanceService {
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final EntityManager em;
    private final CustomSequenceService customSequenceService;
    private final Scheduler scheduler;
    private final PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    private final LocationService locationService;
    private final TeamService teamService;
    private final UserService userService;
    private final AssetService assetService;
    private final WorkOrderCategoryService workOrderCategoryService;
    private final ScheduleService scheduleService;
    private final LicenseService licenseService;
    private final CustomFieldValueService customFieldValueService;
    private final WorkOrderService workOrderService;
    private final TaskService taskService;


    @Transactional
    public PreventiveMaintenance create(PreventiveMaintenancePostDTO preventiveMaintenancePost, User user) {
        if (!user.getRole().getCreatePermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        PreventiveMaintenance preventiveMaintenance = preventiveMaintenanceMapper.toModel(preventiveMaintenancePost);
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.PREVENTIVE_MAINTENANCE)) {
            throw new CustomException("Preventive maintenance feature is not enabled for this subscription plan.",
                    HttpStatus.FORBIDDEN);
        }
        checkUsageBasedLimit(user.getCompany());
        Company company = user.getCompany();
        Long nextSequence = customSequenceService.getNextPreventiveMaintenanceSequence(company);
        preventiveMaintenance.setCustomId("PM" + String.format("%06d", nextSequence));

        if (!preventiveMaintenancePost.getCustomFields().isEmpty()) {
            setPMCustomFields(preventiveMaintenance, preventiveMaintenancePost.getCustomFields(), company);
        }
        Sanitizer.sanitizePreventiveMaintenance(preventiveMaintenance);
        PreventiveMaintenance savedPM = preventiveMaintenanceRepository.saveAndFlush(preventiveMaintenance);
        em.refresh(savedPM);

        Schedule schedule = savedPM.getSchedule();
        schedule.setDaysOfWeek(preventiveMaintenancePost.getDaysOfWeek());
        schedule.setRecurrenceBasedOn(preventiveMaintenancePost.getRecurrenceBasedOn());
        schedule.setRecurrenceType(preventiveMaintenancePost.getRecurrenceType());
        schedule.setEndsOn(preventiveMaintenancePost.getEndsOn());
        schedule.setStartsOn(preventiveMaintenancePost.getStartsOn() != null ?
                preventiveMaintenancePost.getStartsOn() : new Date());
        schedule.setFrequency(preventiveMaintenancePost.getFrequency());
        schedule.setDueDateDelay(preventiveMaintenancePost.getDueDateDelay());
        Schedule savedSchedule = scheduleService.save(schedule);
        em.refresh(savedSchedule);
        em.refresh(savedPM);
        scheduleService.scheduleWorkOrder(savedSchedule);
        return savedPM;
    }

    @Transactional
    public PreventiveMaintenance update(Long id, PreventiveMaintenancePatchDTO preventiveMaintenance, User user) {
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.PREVENTIVE_MAINTENANCE)) {
            throw new CustomException("Preventive maintenance feature is not enabled for this subscription plan.",
                    HttpStatus.FORBIDDEN);
        }
        if (preventiveMaintenanceRepository.existsById(id)) {
            PreventiveMaintenance savedPreventiveMaintenance = preventiveMaintenanceRepository.findById(id).get();
            if (!preventiveMaintenance.getCustomFields().isEmpty()) {
                setPMCustomFields(savedPreventiveMaintenance, preventiveMaintenance.getCustomFields(),
                        user.getCompany());
            }
            PreventiveMaintenance pmToSave =
                    preventiveMaintenanceMapper.updatePreventiveMaintenance(savedPreventiveMaintenance,
                            preventiveMaintenance);
            Sanitizer.sanitizePreventiveMaintenance(pmToSave);
            pmToSave.getSchedule().setDisabled(false);
            PreventiveMaintenance updatedPM =
                    preventiveMaintenanceRepository.saveAndFlush(pmToSave);
            em.refresh(updatedPM);
            return updatedPM;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public PreventiveMaintenance patch(Long id, PreventiveMaintenancePatchDTO preventiveMaintenance, User user) {
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceRepository.findById(id);
        if (optionalPreventiveMaintenance.isPresent()) {
            PreventiveMaintenance savedPreventiveMaintenance = optionalPreventiveMaintenance.get();
            if (savedPreventiveMaintenance.canBeEditedBy(user)) {
                return update(id, preventiveMaintenance, user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public WorkOrder createWorkOrderFromPreventiveMaintenance(PreventiveMaintenance preventiveMaintenance) {
        WorkOrderPostDTO workOrder = workOrderService.getWorkOrderFromWorkOrderBase(preventiveMaintenance);
        workOrder.getCustomFields().removeIf(customFieldValue -> !workOrder.getCustomFieldValues()
                .stream().filter(customFieldValue1 -> customFieldValue1.getCustomField().getId().equals(customFieldValue.getId()))
                .findFirst().get().getCustomField().isCopyOnRepeat());

        Collection<Task> tasks = taskService.findByPreventiveMaintenance(preventiveMaintenance.getId());
        workOrder.setParentPreventiveMaintenance(preventiveMaintenance);

        Schedule schedule = preventiveMaintenance.getSchedule();
        if (schedule.getDueDateDelay() != null) {
            workOrder.setDueDate(Helper.incrementDays(new Date(), schedule.getDueDateDelay()));
        }

        WorkOrder savedWorkOrder = workOrderService.create(workOrder, preventiveMaintenance.getCompany());

        tasks.forEach(task -> {
            Task copiedTask = new Task(task.getTaskBase(), savedWorkOrder, null, task.getValue());
            copiedTask.setCompany(preventiveMaintenance.getCompany());
            taskService.create(copiedTask);
        });

        return savedWorkOrder;
    }

    @Transactional
    public WorkOrder triggerWorkOrder(Long id, User user) {
        if (!(user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS))) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        PreventiveMaintenance preventiveMaintenance = findById(id)
                .orElseThrow(() -> new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND));
        checkAccessToPreventiveMaintenance(user, preventiveMaintenance);
        return createWorkOrderFromPreventiveMaintenance(preventiveMaintenance);
    }

    public List<WorkOrder> getRecentWorkOrders(Long id, User user) {
        checkAccessToPreventiveMaintenance(user, findByIdAndCompany(id, user.getCompany().getId()).get());
        return workOrderService.findLastByPM(id, 10).stream().collect(Collectors.toList());
    }

    private void checkAccessToPreventiveMaintenance(User user, PreventiveMaintenance preventiveMaintenance) {
        if (!preventiveMaintenance.canBeViewedBy(user)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    private void setPMCustomFields(PreventiveMaintenance preventiveMaintenance,
                                   List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
                                   Company company) {
        customFieldValueService.setCustomFields(
                preventiveMaintenance,
                preventiveMaintenance.getCustomFieldValues(),
                customFieldValuePostDTOS,
                company,
                CustomFieldEntityType.WORK_ORDER,
                cfv -> cfv.setPreventiveMaintenance(preventiveMaintenance)
        );
    }

    public Collection<PreventiveMaintenance> getAll() {
        return preventiveMaintenanceRepository.findAll();
    }

    public void delete(Long id) {
        preventiveMaintenanceRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceRepository.findById(id);
        if (optionalPreventiveMaintenance.isPresent()) {
            PreventiveMaintenance savedPreventiveMaintenance = optionalPreventiveMaintenance.get();
            if (savedPreventiveMaintenance.canBeDeletedBy(user)) {
                scheduleService.stopScheduleJobs(savedPreventiveMaintenance.getSchedule().getId());
                delete(id);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND);
    }

    public Optional<PreventiveMaintenance> findById(Long id) {
        return preventiveMaintenanceRepository.findById(id);
    }

    public PreventiveMaintenance getById(Long id, User user) {
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceRepository.findById(id);
        if (optionalPreventiveMaintenance.isPresent()) {
            PreventiveMaintenance savedPreventiveMaintenance = optionalPreventiveMaintenance.get();
            checkAccessToPreventiveMaintenance(user, savedPreventiveMaintenance);
            return savedPreventiveMaintenance;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<PreventiveMaintenance> findByCompany(Long id) {
        return preventiveMaintenanceRepository.findByCompany_Id(id);
    }

    public Page<PreventiveMaintenance> findByCompanyForExport(Long companyId, Pageable pageable) {
        return preventiveMaintenanceRepository.findByCompanyForExport(companyId, pageable);
    }

    private void checkUsageBasedLimit(Company company) {
        Integer threshold = usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_PM_SCHEDULES);
        if (!licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_PM_SCHEDULES)
                && preventiveMaintenanceRepository.hasMoreThan(company.getId(), threshold.longValue() - 1
        ))
            throw new CustomException("You need a license to add a new PM schedule. Free Limit reached: " + threshold,
                    HttpStatus.FORBIDDEN);

    }

    public Page<PreventiveMaintenanceShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<PreventiveMaintenance> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return preventiveMaintenanceRepository.findAll(builder.build(), page).map(preventiveMaintenanceMapper::toShowDto);
    }

    public Page<PreventiveMaintenance> findBySearchCriteriaWithEntityGraph(SearchCriteria searchCriteria) {
        SpecificationBuilder<PreventiveMaintenance> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        Specification<PreventiveMaintenance> baseSpec = builder.build();
        Specification<PreventiveMaintenance> fetchSpec = (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch(PreventiveMaintenance_.asset, JoinType.LEFT);
                root.fetch(PreventiveMaintenance_.location, JoinType.LEFT);
                root.fetch(PreventiveMaintenance_.category, JoinType.LEFT);
                root.fetch(PreventiveMaintenance_.primaryUser, JoinType.LEFT);
                root.fetch(PreventiveMaintenance_.team, JoinType.LEFT);
                root.fetch(PreventiveMaintenance_.image, JoinType.LEFT);
                root.fetch(PreventiveMaintenance_.schedule, JoinType.LEFT);
            }
            return baseSpec == null ? null : baseSpec.toPredicate(root, query, criteriaBuilder);
        };
        return preventiveMaintenanceRepository.findAll(fetchSpec, page);
    }

    public SearchCriteria getSearchCriteria(User user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)) {
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
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return searchCriteria;
    }

    public List<CalendarEvent<PreventiveMaintenance>> getEvents(Date end, Long companyId) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.PM_CALENDAR))
            return Collections.emptyList();
        List<PreventiveMaintenance> preventiveMaintenances =
                preventiveMaintenanceRepository.findByCreatedAtBeforeAndCompany_Id(end, companyId);
        List<CalendarEvent<PreventiveMaintenance>> result = new ArrayList<>();

        for (PreventiveMaintenance preventiveMaintenance : preventiveMaintenances) {
            Schedule schedule = preventiveMaintenance.getSchedule();
            if (schedule == null || schedule.isDisabled()) continue;

            if (schedule.getRecurrenceBasedOn() != RecurrenceBasedOn.SCHEDULED_DATE) continue;

            try {
                TriggerKey triggerKey = new TriggerKey("wo-trigger-" + schedule.getId(), "wo-group");
                Trigger trigger = scheduler.getTrigger(triggerKey);

                if (trigger == null) {
                    log.warn("No trigger found for schedule {}", schedule.getId());
                    continue;
                }

                // Get all fire times up to the end date
                List<Date> fireTimes = new ArrayList<>();

                // Use TriggerUtils to get computed fire times
                if (trigger instanceof OperableTrigger) {
                    OperableTrigger operableTrigger = (OperableTrigger) trigger;
                    Date currentTime = new Date();

                    // Start from now or startsOn, whichever is earlier
                    Date startTime = schedule.getStartsOn().before(currentTime) ?
                            schedule.getStartsOn() : currentTime;

                    // Compute fire times
                    Date fireTime = operableTrigger.getFireTimeAfter(startTime);
                    while (fireTime != null && (fireTime.before(end) || fireTime.equals(end))) {
                        if (shouldFireOnDate(schedule, fireTime)) {
                            fireTimes.add(fireTime);
                        }
                        fireTime = operableTrigger.getFireTimeAfter(fireTime);

                        // Safety limit to prevent infinite loops
                        if (fireTimes.size() > 1000) {
                            log.warn("Reached safety limit of 1000 events for schedule {}", schedule.getId());
                            break;
                        }
                    }
                }

                // Convert fire times to calendar events
                result.addAll(fireTimes.stream()
                        .map(date -> new CalendarEvent<>("PREVENTIVE_MAINTENANCE", preventiveMaintenance, date))
                        .toList());

            } catch (SchedulerException e) {
                log.error("Error getting trigger fire times for schedule {}", schedule.getId(), e);
            }
        }

        return result;
    }

    private boolean shouldFireOnDate(Schedule schedule, Date fireTime) {
        if (schedule.getRecurrenceType() != RecurrenceType.WEEKLY || schedule.getFrequency() <= 1) {
            return true;
        }

        String tzId = schedule.getPreventiveMaintenance()
                .getCompany().getCompanySettings()
                .getGeneralPreferences().getTimeZone();
        ZoneId zoneId = ZoneId.of(tzId);

        long daysSinceStart = ChronoUnit.DAYS.between(
                schedule.getStartsOn().toInstant().atZone(zoneId).toLocalDate(),
                fireTime.toInstant().atZone(zoneId).toLocalDate()
        );
        long weeksSinceStart = daysSinceStart / 7;

        return weeksSinceStart % schedule.getFrequency() == 0;
    }

    public Optional<PreventiveMaintenance> findByIdAndCompany(Long id, Long companyId) {
        return preventiveMaintenanceRepository.findByIdAndCompany_Id(id, companyId);
    }

    public List<PreventiveMaintenance> saveAll(List<PreventiveMaintenance> preventiveMaintenances) {
        return preventiveMaintenanceRepository.saveAll(preventiveMaintenances);
    }

    public List<PreventiveMaintenance> findByIdsAndCompany(List<Long> ids, Long companyId) {
        return preventiveMaintenanceRepository.findByIdInAndCompany_Id(ids, companyId);
    }

    public void importPreventiveMaintenance(PreventiveMaintenance preventiveMaintenance,
                                            PreventiveMaintenanceImportDTO pmImportDTO, Company company) {
        checkUsageBasedLimit(company);
        Helper.populateWorkOrderBaseFromImportDTO(preventiveMaintenance, pmImportDTO, company, locationService,
                teamService, userService, assetService, workOrderCategoryService);

        preventiveMaintenance.setName(pmImportDTO.getName());
        preventiveMaintenance.setCompany(company);
        Schedule schedule = preventiveMaintenance.getSchedule();
        schedule.setStartsOn(Helper.getDateFromExcelDate(pmImportDTO.getStartsOn()));
        schedule.setFrequency((int) pmImportDTO.getFrequency());
        schedule.setDueDateDelay(pmImportDTO.getDueDateDelay() == null ? null :
                pmImportDTO.getDueDateDelay().intValue());
        schedule.setEndsOn(Helper.getDateFromExcelDate(pmImportDTO.getEndsOn()));
        schedule.setRecurrenceType(RecurrenceType.valueOf(pmImportDTO.getRecurrenceType().toUpperCase()));
        schedule.setRecurrenceBasedOn(RecurrenceBasedOn.valueOf(pmImportDTO.getRecurrenceBasedOn().trim().replaceAll(
                "\\s+", "_").toUpperCase()));
        schedule.setDaysOfWeek(pmImportDTO.getDaysOfWeek().stream().map(this::getDayOfWeekNumber).collect(Collectors.toList()));

        preventiveMaintenance.setCustomId("PM" + String.format("%06d",
                customSequenceService.getNextPreventiveMaintenanceSequence(company)));
        Sanitizer.sanitizePreventiveMaintenance(preventiveMaintenance);

        PreventiveMaintenance savedPM = preventiveMaintenanceRepository.save(preventiveMaintenance);
        scheduleService.reScheduleWorkOrder(savedPM.getSchedule());
    }

    private int getDayOfWeekNumber(String day) {
        return switch (day.toLowerCase()) {
            case "monday" -> 0;
            case "tuesday" -> 1;
            case "wednesday" -> 2;
            case "thursday" -> 3;
            case "friday" -> 4;
            case "saturday" -> 5;
            case "sunday" -> 6;
            default -> throw new IllegalArgumentException("Invalid day of week: " + day);
        };
    }
}

