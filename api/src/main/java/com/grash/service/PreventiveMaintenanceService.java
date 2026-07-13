package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.CalendarEvent;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.repository.PreventiveMaintenanceRepository;
import lombok.RequiredArgsConstructor;
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
public class PreventiveMaintenanceService {
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final TeamService teamService;
    private final UserService userService;
    private final AssetService assetService;
    private final CompanyService companyService;
    private final LocationService locationService;
    private final EntityManager em;
    private final CustomSequenceService customSequenceService;

    private final PreventiveMaintenanceMapper preventiveMaintenanceMapper;

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
            PreventiveMaintenance updatedPM =
                    preventiveMaintenanceRepository.saveAndFlush(preventiveMaintenanceMapper.updatePreventiveMaintenance(savedPreventiveMaintenance, preventiveMaintenance));
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

    public List<CalendarEvent> getEvents(Date end, Long companyId) {
        List<PreventiveMaintenance> preventiveMaintenances =
                preventiveMaintenanceRepository.findByCreatedAtBeforeAndCompany_Id(end, companyId);
        List<CalendarEvent> result = new ArrayList<>();
        for (PreventiveMaintenance preventiveMaintenance : preventiveMaintenances) {
            Schedule schedule = preventiveMaintenance.getSchedule();
            if (schedule.isDisabled()) continue;
            List<Date> dates = new ArrayList<>();

            // Create a Calendar instance and set the start date
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(schedule.getStartsOn());

            // Add the start date to the list
            dates.add(schedule.getStartsOn());

            Date max = schedule.getEndsOn() == null ? end : schedule.getEndsOn().before(end) ? schedule.getEndsOn() :
                    end;
            // Loop until the calendar date is after the end date
            while (calendar.getTime().before(max)) {
                // Add the frequency days to the current date
                calendar.add(Calendar.DAY_OF_MONTH, schedule.getFrequency());

                // Add the new date to the list if it's before or equal to the end date
                if (!calendar.getTime().after(max)) {
                    dates.add(calendar.getTime());
                }
            }

            result.addAll(dates.stream().map(date -> new CalendarEvent("PREVENTIVE_MAINTENANCE",
                    preventiveMaintenanceMapper.toBaseMiniDto(preventiveMaintenance), date)).collect(Collectors.toList()));
        }
        return result;
    }
}
