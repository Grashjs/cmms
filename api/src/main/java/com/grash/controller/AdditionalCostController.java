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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/additional-costs")
@Tag(name = "additionalCost")
@RequiredArgsConstructor
public class AdditionalCostController {

    private final AdditionalCostService additionalCostService;
    private final UserService userService;
    private final WorkOrderService workOrderService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public AdditionalCost getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);
        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            checkAccessToAdditionalCost(savedAdditionalCost, user);
            return savedAdditionalCost;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    public Collection<AdditionalCost> getByWorkOrder(@PathVariable("id") Long id,
                                                     HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder workOrder = optionalWorkOrder.get();
            checkAccessToWorkOrder(workOrder, user);
            return additionalCostService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AdditionalCost create(@Valid @RequestBody AdditionalCost additionalCostReq,
                                 HttpServletRequest req) {
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
    public AdditionalCost patch(@Valid @RequestBody AdditionalCostPatchDTO additionalCost, @PathVariable("id") Long id,
                                HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);

        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            checkAccessToAdditionalCost(savedAdditionalCost, user);
            return additionalCostService.update(id, additionalCost);
        } else throw new CustomException("AdditionalCost not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);
        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            checkAccessToAdditionalCost(savedAdditionalCost, user);
            additionalCostService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("AdditionalCost not found", HttpStatus.NOT_FOUND);
    }

    private void checkAccessToAdditionalCost(AdditionalCost additionalCost, OwnUser user) {
        if (!additionalCost.getWorkOrder().getCompany().getId().equals(user.getCompany().getId())) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    private void checkAccessToWorkOrder(WorkOrder workOrder, OwnUser user) {
        if (!workOrder.getCompany().getId().equals(user.getCompany().getId())) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

}


