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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/labors")
@Api(tags = "labor")
@RequiredArgsConstructor
public class LaborController {

    private final LaborService laborService;
    private final UserService userService;
    private final WorkOrderService workOrderService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Labor not found")})
    public Labor getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Labor> optionalLabor = laborService.findById(id);
        if (optionalLabor.isPresent()) {
            Labor savedLabor = optionalLabor.get();
            return savedLabor;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);

    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Labor not found")})
    public Collection<Labor> getByWorkOrder(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return laborService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Labor not found")})
    public Labor controlTimer(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req, @RequestParam(defaultValue = "true") boolean start) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent() && optionalWorkOrder.get().canBeEditedBy(user)) {
            Optional<Labor> optionalLabor = laborService.findByWorkOrder(id).stream().filter(labor -> labor.isLogged() && labor.getAssignedTo().getId().equals(user.getId())).findFirst();
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
                    Labor labor = new Labor(user, user.getRate(), new Date(), optionalWorkOrder.get(), true, TimeStatus.RUNNING);
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Labor create(@ApiParam("Labor") @Valid @RequestBody Labor laborReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ADDITIONAL_TIME)) {
            WorkOrder workOrder = workOrderService.findById(laborReq.getWorkOrder().getId()).get();
            if (workOrder.getFirstTimeToReact() == null) {
                workOrder.setFirstTimeToReact(new Date());
                workOrderService.save(workOrder);
            }
            return laborService.create(laborReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Labor not found")})
    public Labor patch(@ApiParam("Labor") @Valid @RequestBody LaborPatchDTO labor, @ApiParam("id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Labor not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
