package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PreventiveMaintenancePatchDTO;
import com.grash.dto.PreventiveMaintenancePostDTO;
import com.grash.dto.PreventiveMaintenanceShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.PreventiveMaintenanceMapper;
import com.grash.model.OwnUser;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PreventiveMaintenanceService;
import com.grash.service.ScheduleService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/preventive-maintenances")
@Api(tags = "preventiveMaintenance")
@RequiredArgsConstructor
public class PreventiveMaintenanceController {

    private final PreventiveMaintenanceService preventiveMaintenanceService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final PreventiveMaintenanceMapper preventiveMaintenanceMapper;
    private final EntityManager em;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<PreventiveMaintenanceShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                                     HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(preventiveMaintenanceService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "PreventiveMaintenance not found")})
    public PreventiveMaintenanceShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceService.findById(id);
        if (optionalPreventiveMaintenance.isPresent()) {
            PreventiveMaintenance savedPreventiveMaintenance = optionalPreventiveMaintenance.get();
            return preventiveMaintenanceMapper.toShowDto(savedPreventiveMaintenance);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public PreventiveMaintenanceShowDTO create(@ApiParam("PreventiveMaintenance") @Valid @RequestBody PreventiveMaintenancePostDTO preventiveMaintenancePost, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        PreventiveMaintenance preventiveMaintenance = preventiveMaintenanceMapper.toModel(preventiveMaintenancePost);
        preventiveMaintenance = preventiveMaintenanceService.create(preventiveMaintenance, user);

        Schedule schedule = preventiveMaintenance.getSchedule();
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
        em.refresh(preventiveMaintenance);
        scheduleService.scheduleWorkOrder(savedSchedule);
        return preventiveMaintenanceMapper.toShowDto(preventiveMaintenance);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PreventiveMaintenance not found")})
    public PreventiveMaintenanceShowDTO patch(@ApiParam("PreventiveMaintenance") @Valid @RequestBody PreventiveMaintenancePatchDTO preventiveMaintenance, @ApiParam("id") @PathVariable("id") Long id,
                                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceService.findById(id);

        if (optionalPreventiveMaintenance.isPresent()) {
            PreventiveMaintenance savedPreventiveMaintenance = optionalPreventiveMaintenance.get();
            PreventiveMaintenance patchedPreventiveMaintenance = preventiveMaintenanceService.update(id,
                    preventiveMaintenance);
            return preventiveMaintenanceMapper.toShowDto(patchedPreventiveMaintenance);
        } else throw new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PreventiveMaintenance not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<PreventiveMaintenance> optionalPreventiveMaintenance = preventiveMaintenanceService.findById(id);
        if (optionalPreventiveMaintenance.isPresent()) {
            scheduleService.stopScheduleTimers(optionalPreventiveMaintenance.get().getSchedule().getId());
            preventiveMaintenanceService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("PreventiveMaintenance not found", HttpStatus.NOT_FOUND);
    }

}
