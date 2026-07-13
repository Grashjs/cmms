package com.grash.controller;

import com.grash.dto.AdditionalCostPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.AdditionalCost;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.AdditionalCostService;
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
@RequestMapping("/additional-costs")
@Api(tags = "additionalCost")
@RequiredArgsConstructor
public class AdditionalCostController {

    private final AdditionalCostService additionalCostService;
    private final UserService userService;
    private final WorkOrderService workOrderService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AdditionalCost not found")})
    public AdditionalCost getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);
        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            return savedAdditionalCost;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AdditionalCost not found")})
    public Collection<AdditionalCost> getByWorkOrder(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return additionalCostService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public AdditionalCost create(@ApiParam("AdditionalCost") @Valid @RequestBody AdditionalCost additionalCostReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ADDITIONAL_COST)) {
            WorkOrder workOrder = workOrderService.findById(additionalCostReq.getWorkOrder().getId()).get();
            if (workOrder.getFirstTimeToReact() == null) {
                workOrder.setFirstTimeToReact(new Date());
                workOrderService.save(workOrder);
            }
            return additionalCostService.create(additionalCostReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "AdditionalCost not found")})
    public AdditionalCost patch(@ApiParam("AdditionalCost") @Valid @RequestBody AdditionalCostPatchDTO additionalCost, @ApiParam("id") @PathVariable("id") Long id,
                                HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);

        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            return additionalCostService.update(id, additionalCost);
        } else throw new CustomException("AdditionalCost not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "AdditionalCost not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);
        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            additionalCostService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("AdditionalCost not found", HttpStatus.NOT_FOUND);
    }

}
