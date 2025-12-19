package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.CalendarEvent;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.dto.imports.PreventiveMaintenanceImportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.*;
import com.grash.model.enums.Priority;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import com.grash.repository.PreventiveMaintenanceRepository;
import com.grash.utils.Helper;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

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


    @Transactional
    public PreventiveMaintenance create(PreventiveMaintenance preventiveMaintenance, OwnUser user) {
        // Generate custom ID
        Company company = user.getCompany();
        Long nextSequence = customSequenceService.getNextPreventiveMaintenanceSequence(company);
        preventiveMaintenance.setCustomId("PM" + String.format("%06d", nextSequence));

        PreventiveMaintenance savedPM = preventiveMaintenanceRepository.saveAndFlush(preventiveMaintenance);
        em.refresh(savedPM);
        return savedPM;
    }

    @Transactional
    public PreventiveMaintenance update(Long id, PreventiveMaintenancePatchDTO preventiveMaintenance) {
        if (preventiveMaintenanceRepository.existsById(id)) {
            PreventiveMaintenance savedPreventiveMaintenance = preventiveMaintenanceRepository.findById(id).get();
            PreventiveMaintenance pmToSave =
                    preventiveMaintenanceMapper.updatePreventiveMaintenance(savedPreventiveMaintenance,
                            preventiveMaintenance);
            pmToSave.getSchedule().setDisabled(false);
            PreventiveMaintenance updatedPM =
                    preventiveMaintenanceRepository.saveAndFlush(pmToSave);
            em.refresh(updatedPM);
            return updatedPM;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<PreventiveMaintenance> getAll() {
        return preventiveMaintenanceRepository.findAll();
    }

    public void delete(Long id) {
        preventiveMaintenanceRepository.deleteById(id);
    }

    public Optional<PreventiveMaintenance> findById(Long id) {
        return preventiveMaintenanceRepository.findById(id);
    }

    public Collection<PreventiveMaintenance> findByCompany(Long id) {
        return preventiveMaintenanceRepository.findByCompany_Id(id);
    }

    public Page<PreventiveMaintenanceShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<PreventiveMaintenance> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return preventiveMaintenanceRepository.findAll(builder.build(), page).map(preventiveMaintenanceMapper::toShowDto);
    }

    public boolean isPreventiveMaintenanceInCompany(PreventiveMaintenance preventiveMaintenance, long companyId,
                                                    boolean optional) {
        if (optional) {
            Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenance == null ?
                    Optional.empty() : findById(preventiveMaintenance.getId());
            return preventiveMaintenance == null || (optionalPreventiveMaintenance.isPresent() && optionalPreventiveMaintenance.get().getCompany().getId().equals(companyId));
        } else {
            Optional<PreventiveMaintenance> optionalPreventiveMaintenance = findById(preventiveMaintenance.getId());
            return optionalPreventiveMaintenance.isPresent() && optionalPreventiveMaintenance.get().getCompany().getId().equals(companyId);
        }
    }

    public List<CalendarEvent<PreventiveMaintenance>> getEvents(Date end, Long companyId) {
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
                        fireTimes.add(fireTime);
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
                        .collect(Collectors.toList()));

            } catch (SchedulerException e) {
                log.error("Error getting trigger fire times for schedule {}", schedule.getId(), e);
            }
        }

        return result;
    }

    public Optional<PreventiveMaintenance> findByIdAndCompany(Long id, Long companyId) {
        return preventiveMaintenanceRepository.findByIdAndCompany_Id(id, companyId);
    }

    public void importPreventiveMaintenance(PreventiveMaintenance preventiveMaintenance,
                                            PreventiveMaintenanceImportDTO pmImportDTO, Company company) {

        Helper.populateWorkOrderBaseFromImportDTO(preventiveMaintenance, pmImportDTO, company, locationService,
                teamService, userService, assetService, workOrderCategoryService);

        preventiveMaintenance.setName(pmImportDTO.getName());

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

        PreventiveMaintenance savedPM = preventiveMaintenanceRepository.save(preventiveMaintenance);
        scheduleService.reScheduleWorkOrder(savedPM.getSchedule());
    }

    private int getDayOfWeekNumber(String day) {
        switch (day.toLowerCase()) {
            case "monday":
                return 0;
            case "tuesday":
                return 1;
            case "wednesday":
                return 2;
            case "thursday":
                return 3;
            case "friday":
                return 4;
            case "saturday":
                return 5;
            case "sunday":
                return 6;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + day);
        }
    }
}
