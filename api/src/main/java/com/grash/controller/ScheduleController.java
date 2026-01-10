package com.grash.controller;

import com.grash.dto.SchedulePatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Schedule;
import com.grash.model.enums.RoleType;
import com.grash.service.ScheduleService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/schedules")
@Tag(name = "schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;

//    @GetMapping("")
//    @PreAuthorize("permitAll()")
//    
//    public Collection<Schedule> getAll(HttpServletRequest req) {
//        OwnUser user = userService.whoami(req);
//        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
//            return scheduleService.findByCompany(user.getCompany().getId());
//        } else return scheduleService.getAll();
//    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Schedule getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Schedule> optionalSchedule = scheduleService.findById(id);
        if (optionalSchedule.isPresent()) {
            Schedule savedSchedule = optionalSchedule.get();
            if (!savedSchedule.getPreventiveMaintenance().getCompany().getId().equals(user.getCompany().getId()))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return savedSchedule;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Schedule patch(@Valid @RequestBody SchedulePatchDTO schedule,
                          @PathVariable("id") Long id,
                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Schedule> optionalSchedule = scheduleService.findById(id);

        if (optionalSchedule.isPresent()) {
            Schedule savedSchedule = optionalSchedule.get();
            if (!savedSchedule.getPreventiveMaintenance().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            Schedule updatedSchedule = scheduleService.update(id, schedule);
            scheduleService.reScheduleWorkOrder(updatedSchedule);
            return updatedSchedule;
        } else throw new CustomException("Schedule not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Schedule> optionalSchedule = scheduleService.findById(id);
        if (optionalSchedule.isPresent()) {
            Schedule savedSchedule = optionalSchedule.get();
            if (!savedSchedule.getPreventiveMaintenance().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            scheduleService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Schedule not found", HttpStatus.NOT_FOUND);
    }

}


