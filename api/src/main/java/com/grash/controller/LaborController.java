package com.grash.controller;

import com.grash.dto.LaborPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Labor;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.Status;
import com.grash.model.enums.TimeStatus;
import com.grash.service.LaborService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/labors")
@Tag(name = "Labor", description = "Operations on labor tracking")
@RequiredArgsConstructor
public class LaborController {

    private final LaborService laborService;
    private final UserService userService;
    private final WorkOrderService workOrderService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Labor getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Labor> optionalLabor = laborService.findById(id);
        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            if (savedLabor.getWorkOrder().canBeViewedBy(user)) {
                return savedLabor;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);

    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    public Collection<Labor> getByWorkOrder(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            if (!optionalWorkOrder.get().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return laborService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    public Labor controlTimer(@PathVariable("id") Long id, HttpServletRequest req,
                              @RequestParam(defaultValue = "true") boolean start,
                              @RequestParam(required = false) Long laborId,
                              @RequestParam(required = false) java.util.UUID timerRunId,
                              @RequestParam(required = false) Long version) {
        return laborService.controlTimer(id, start, laborId, timerRunId, version, userService.whoami(req));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    Labor create(@Parameter(description = "Labor data to create") @Valid @RequestBody Labor laborReq,
                 HttpServletRequest req) {
        User user = userService.whoami(req);
        return laborService.createConnected(laborReq, user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Labor patch(@Parameter(description = "Labor fields to update") @Valid @RequestBody LaborPatchDTO labor,
                       @PathVariable("id") Long id,
                       HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Labor> optionalLabor = laborService.findById(id);

        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            if (!savedLabor.getWorkOrder().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return laborService.update(id, labor, user);
        } else throw new CustomException("Labor not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, @RequestParam(required = false) Long version, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<Labor> optionalLabor = laborService.findById(id);
        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            if (!savedLabor.getWorkOrder().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            laborService.delete(id, version, user);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Labor not found", HttpStatus.NOT_FOUND);
    }
}


