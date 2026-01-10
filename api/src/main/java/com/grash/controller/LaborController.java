package com.grash.controller;

import com.grash.dto.LaborPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Labor;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.Status;
import com.grash.model.enums.TimeStatus;
import com.grash.service.LaborService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/labors")
@Tag(name = "labor")
@RequiredArgsConstructor
public class LaborController {

    private final LaborService laborService;
    private final UserService userService;
    private final WorkOrderService workOrderService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Labor getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Labor> optionalLabor = laborService.findById(id);
        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            return savedLabor;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);

    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    public Collection<Labor> getByWorkOrder(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return laborService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    public Labor controlTimer(@PathVariable("id") Long id, HttpServletRequest req,
                              @RequestParam(defaultValue = "true") boolean start) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent() && optionalWorkOrder.get().canBeEditedBy(user)) {
            Optional<Labor> optionalLabor =
                    laborService.findByWorkOrder(id).stream().filter(labor -> labor.isLogged() && labor.getAssignedTo().getId().equals(user.getId())).findFirst();
            if (start) {
                WorkOrder workOrder = optionalWorkOrder.get();
                if (workOrder.getFirstTimeToReact() == null) workOrder.setFirstTimeToReact(new Date());

                if (!workOrder.getStatus().equals(Status.IN_PROGRESS)) {
                    workOrder.setStatus(Status.IN_PROGRESS);
                    workOrderService.save(workOrder);
                }
                if (optionalLabor.isPresent()) {
                    Labor labor = optionalLabor.get();
                    if (labor.getStatus().equals(TimeStatus.RUNNING)) {
                        return labor;
                    } else {
                        labor.setStartedAt(new Date());
                        labor.setStatus(TimeStatus.RUNNING);
                        return laborService.save(labor);
                    }
                } else {
                    Labor labor = new Labor(user, user.getRate(), new Date(), optionalWorkOrder.get(), true,
                            TimeStatus.RUNNING);
                    return laborService.create(labor);
                }
            } else {
                if (optionalLabor.isPresent()) {
                    Labor labor = optionalLabor.get();
                    if (labor.getStatus().equals(TimeStatus.STOPPED)) {
                        return labor;
                    } else {
                        return laborService.stop(labor);
                    }
                } else throw new CustomException("No timer to stop", HttpStatus.NOT_FOUND);
            }
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Labor patch(@Valid @RequestBody LaborPatchDTO labor,
                       @PathVariable("id") Long id,
                       HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Labor> optionalLabor = laborService.findById(id);

        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            return laborService.update(id, labor);
        } else throw new CustomException("Labor not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Labor> optionalLabor = laborService.findById(id);
        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            laborService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Labor not found", HttpStatus.NOT_FOUND);
    }
}


