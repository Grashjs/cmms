package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.CalendarEvent;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.dto.imports.PreventiveMaintenanceImportDTO;
import com.grash.model.*;
import com.grash.model.enums.Priority;
import com.grash.model.enums.RecurrenceType;
import com.grash.utils.Helper;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.repository.PreventiveMaintenanceRepository;
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
    private final WorkOrderCategoryService workOrderCategoryService;
    private final LocationService locationService;
    private final TeamService teamService;
    private final UserService userService;
    private final AssetService assetService;
    private final ChecklistService checklistService;

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
        return preventiveMaintenanceRepository.findById(id)
                .filter(pm -> pm.getCompany().getId().equals(companyId));
    }

    @Transactional
    public void importPreventiveMaintenance(PreventiveMaintenance pm, PreventiveMaintenanceImportDTO dto, Company company) {
        Long companySettingsId = company.getCompanySettings().getId();
        Long companyId = company.getId();
        
        // Set basic fields
        pm.setName(dto.getName());
        pm.setTitle(dto.getTitle());
        pm.setDescription(dto.getDescription());
        pm.setCompany(company);
        
        // Set priority
        if (dto.getPriority() != null && !dto.getPriority().isEmpty()) {
            try {
                pm.setPriority(Priority.valueOf(dto.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                pm.setPriority(Priority.NONE);
            }
        } else {
            pm.setPriority(Priority.NONE);
        }
        
        // Set estimated duration (convert hours to minutes)
        if (dto.getEstimatedDuration() != null) {
            pm.setEstimatedDuration(dto.getEstimatedDuration() * 60);
        }
        
        // Set required signature
        if (dto.getRequiredSignature() != null) {
            pm.setRequiredSignature(dto.getRequiredSignature().equalsIgnoreCase("Yes") || 
                                   dto.getRequiredSignature().equalsIgnoreCase("True"));
        }
        
        // Lookup and set category
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            Optional<WorkOrderCategory> optionalCategory = 
                workOrderCategoryService.findByNameIgnoreCaseAndCompanySettings(dto.getCategory(), companySettingsId);
            optionalCategory.ifPresent(pm::setCategory);
        }
        
        // Lookup and set location
        if (dto.getLocationName() != null && !dto.getLocationName().isEmpty()) {
            Optional<Location> optionalLocation = 
                locationService.findByNameIgnoreCaseAndCompany(dto.getLocationName(), companyId).stream().findFirst();
            optionalLocation.ifPresent(pm::setLocation);
        }
        
        // Lookup and set team
        if (dto.getTeamName() != null && !dto.getTeamName().isEmpty()) {
            Optional<Team> optionalTeam = 
                teamService.findByNameIgnoreCaseAndCompany(dto.getTeamName(), companyId);
            optionalTeam.ifPresent(pm::setTeam);
        }
        
        // Lookup and set primary user
        if (dto.getPrimaryUserEmail() != null && !dto.getPrimaryUserEmail().isEmpty()) {
            Optional<OwnUser> optionalUser = 
                userService.findByEmailAndCompany(dto.getPrimaryUserEmail(), companyId);
            optionalUser.ifPresent(pm::setPrimaryUser);
        }
        
        // Lookup and set assigned users
        if (dto.getAssignedToEmails() != null && !dto.getAssignedToEmails().isEmpty()) {
            List<OwnUser> assignedUsers = new ArrayList<>();
            for (String email : dto.getAssignedToEmails()) {
                if (email != null && !email.trim().isEmpty()) {
                    Optional<OwnUser> optionalUser = userService.findByEmailAndCompany(email.trim(), companyId);
                    optionalUser.ifPresent(assignedUsers::add);
                }
            }
            pm.setAssignedTo(assignedUsers);
        }
        
        // Lookup and set asset
        if (dto.getAssetName() != null && !dto.getAssetName().isEmpty()) {
            Optional<Asset> optionalAsset = 
                assetService.findByNameIgnoreCaseAndCompany(dto.getAssetName(), companyId).stream().findFirst();
            optionalAsset.ifPresent(pm::setAsset);
        }
        
        // Set expected start date
        if (dto.getExpectedStartDate() != null) {
            pm.setEstimatedStartDate(Helper.getDateFromExcelDate(dto.getExpectedStartDate()));
        }
        
        // Generate custom ID if creating new PM
        if (pm.getId() == null) {
            Long nextSequence = customSequenceService.getNextPreventiveMaintenanceSequence(company);
            pm.setCustomId("PM" + String.format("%06d", nextSequence));
        }
        
        // Save PM first to get ID
        PreventiveMaintenance savedPM = preventiveMaintenanceRepository.saveAndFlush(pm);
        
        // Create or update schedule
        Schedule schedule = savedPM.getSchedule();
        if (schedule == null) {
            schedule = new Schedule(savedPM);
        }
        
        // Set schedule fields
        if (dto.getStartsOn() != null) {
            schedule.setStartsOn(Helper.getDateFromExcelDate(dto.getStartsOn()));
        }
        
        if (dto.getFrequency() != null) {
            schedule.setFrequency(dto.getFrequency());
        }
        
        // Set recurrence type
        if (dto.getRecurrenceType() != null && !dto.getRecurrenceType().isEmpty()) {
            try {
                schedule.setRecurrenceType(RecurrenceType.valueOf(dto.getRecurrenceType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                schedule.setRecurrenceType(RecurrenceType.MONTHLY);
            }
        }
        
        // Set recurrence based on
        if (dto.getRecurrenceBasedOn() != null && !dto.getRecurrenceBasedOn().isEmpty()) {
            try {
                schedule.setRecurrenceBasedOn(RecurrenceBasedOn.valueOf(dto.getRecurrenceBasedOn().toUpperCase()));
            } catch (IllegalArgumentException e) {
                schedule.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
            }
        } else {
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
        }
        
        // Set due date delay
        if (dto.getDueDateDelay() != null) {
            schedule.setDueDateDelay(dto.getDueDateDelay());
        }
        
        // Set ends on
        if (dto.getEndsOn() != null) {
            schedule.setEndsOn(Helper.getDateFromExcelDate(dto.getEndsOn()));
        }
        
        // Set days of week for weekly recurrence
        if (dto.getDaysOfWeek() != null && !dto.getDaysOfWeek().isEmpty()) {
            schedule.setDaysOfWeek(dto.getDaysOfWeek());
        }
        
        // Enable schedule
        schedule.setDisabled(false);
        
        // Save PM with schedule
        savedPM.setSchedule(schedule);
        preventiveMaintenanceRepository.saveAndFlush(savedPM);
        
        // Copy tasks from checklist if specified
        if (dto.getChecklistName() != null && !dto.getChecklistName().isEmpty()) {
            Collection<Checklist> checklists = checklistService.findByCompanySettings(companySettingsId);
            Optional<Checklist> optionalChecklist = checklists.stream()
                .filter(c -> c.getName().equalsIgnoreCase(dto.getChecklistName()))
                .findFirst();
            if (optionalChecklist.isPresent()) {
                Checklist checklist = optionalChecklist.get();
                // Copy tasks from checklist to PM
                for (TaskBase taskBase : checklist.getTaskBases()) {
                    Task task = new Task(taskBase, null, savedPM, null);
                    task.setCompany(company);
                    em.persist(task);
                }
            }
        }
        
        em.refresh(savedPM);
    }
}
