package com.grash.controller;

import com.grash.dto.AdditionalCostPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.AdditionalCost;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.AdditionalCostService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Additional Costs", description = "Operations on additional costs")
@RequiredArgsConstructor
public class AdditionalCostController {

    private final AdditionalCostService additionalCostService;
    private final UserService userService;
    private final WorkOrderService workOrderService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public AdditionalCost getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);
        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            checkAccessToAdditionalCost(savedAdditionalCost, user, true);
            return savedAdditionalCost;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    public Collection<AdditionalCost> getByWorkOrder(@PathVariable("id") Long id,
                                                     HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            WorkOrder workOrder = optionalWorkOrder.get();
            checkAccessToWorkOrder(workOrder, user, true);
            return additionalCostService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AdditionalCost create(@Parameter(description = "Additional cost to create") @Valid @RequestBody AdditionalCost additionalCostReq,
                                 HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ADDITIONAL_COST)) {
            WorkOrder workOrder = workOrderService.findById(additionalCostReq.getWorkOrder().getId()).get();
            checkAccessToWorkOrder(workOrder, user, false);
            if (workOrder.getFirstTimeToReact() == null) {
                workOrder.setFirstTimeToReact(new Date());
                workOrderService.save(workOrder);
            }
            return additionalCostService.create(additionalCostReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AdditionalCost patch(@Parameter(description = "Additional cost fields to update") @Valid @RequestBody AdditionalCostPatchDTO additionalCost, @PathVariable("id") Long id,
                                HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);

        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            checkAccessToAdditionalCost(savedAdditionalCost, user, false);
            return additionalCostService.update(id, additionalCost);
        } else throw new CustomException("AdditionalCost not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<AdditionalCost> optionalAdditionalCost = additionalCostService.findById(id);
        if (optionalAdditionalCost.isPresent()) {
            AdditionalCost savedAdditionalCost = optionalAdditionalCost.get();
            checkAccessToAdditionalCost(savedAdditionalCost, user, false);
            additionalCostService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("AdditionalCost not found", HttpStatus.NOT_FOUND);
    }

    private void checkAccessToAdditionalCost(AdditionalCost additionalCost, User user, boolean view) {
        checkAccessToWorkOrder(additionalCost.getWorkOrder(), user, view);
    }

    private void checkAccessToWorkOrder(WorkOrder workOrder, User user, boolean view) {
        Long workOrderCompanyId = workOrder.getCompany().getId();
        if (!(view ? workOrder.isAccessibleBy(user) : workOrder.canBeEditedBy(user)) || (!workOrderCompanyId.equals(user.getCompany().getId()) && user.getSuperAccountRelations().stream()
                .noneMatch(superAccountRelation -> superAccountRelation.getChildUser().getCompany()
                        .getId().equals(workOrderCompanyId)))) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

}


