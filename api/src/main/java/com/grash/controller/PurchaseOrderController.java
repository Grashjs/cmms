package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PurchaseOrderPatchDTO;
import com.grash.dto.PurchaseOrderShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.PartQuantityMapper;
import com.grash.mapper.PurchaseOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.*;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-orders")
@Tag(name = "purchaseOrder")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final UserService userService;
    private final PartQuantityService partQuantityService;
    private final MessageSource messageSource;
    private final PartQuantityMapper partQuantityMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PartService partService;
    private final NotificationService notificationService;
    private final EmailService2 emailService2;
    private final WorkflowService workflowService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<PurchaseOrderShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                             HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PURCHASE_ORDERS)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers =
                        user.getRole().getViewOtherPermissions().contains(PermissionEntity.PURCHASE_ORDERS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(purchaseOrderService.findBySearchCriteria(searchCriteria).map(this::setPartQuantities));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public PurchaseOrderShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderService.findById(id);
        if (optionalPurchaseOrder.isPresent()) {
            PurchaseOrder savedPurchaseOrder = optionalPurchaseOrder.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PURCHASE_ORDERS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PURCHASE_ORDERS) || savedPurchaseOrder.getCreatedBy().equals(user.getId()))) {
                return setPartQuantities(purchaseOrderMapper.toShowDto(savedPurchaseOrder));
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public PurchaseOrderShowDTO patch(@Valid @RequestBody PurchaseOrderPatchDTO purchaseOrder, Long id,
                                      HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderService.findById(id);

        if (optionalPurchaseOrder.isPresent()) {
            PurchaseOrder savedPurchaseOrder = optionalPurchaseOrder.get();
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.PURCHASE_ORDERS) || savedPurchaseOrder.getCreatedBy().equals(user.getId())) {
                PurchaseOrder patchedPurchaseOrder = purchaseOrderService.update(id, purchaseOrder);
                Collection<Workflow> workflows =
                        workflowService.findByMainConditionAndCompany(WFMainCondition.PURCHASE_ORDER_UPDATED,
                                user.getCompany().getId());
                workflows.forEach(workflow -> workflowService.runPurchaseOrder(workflow, patchedPurchaseOrder));
                return setPartQuantities(purchaseOrderMapper.toShowDto(patchedPurchaseOrder));
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PurchaseOrder not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public PurchaseOrderShowDTO respond(boolean approved, @PathVariable("id") Long id,
                                        HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderService.findById(id);

        if (optionalPurchaseOrder.isPresent()) {
            PurchaseOrder savedPurchaseOrder = optionalPurchaseOrder.get();
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.PURCHASE_ORDERS)) {
                if (!savedPurchaseOrder.getStatus().equals(ApprovalStatus.APPROVED)) {
                    if (approved) {
                        Collection<PartQuantity> partQuantities =
                                partQuantityService.findByPurchaseOrder(savedPurchaseOrder.getId());
                        partQuantities.forEach(partQuantity -> {
                            Part part = partQuantity.getPart();
                            part.setQuantity(part.getQuantity() + partQuantity.getQuantity());
                            partService.save(part);
                        });
                    }
                    savedPurchaseOrder.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
                    return setPartQuantities(purchaseOrderMapper.toShowDto(purchaseOrderService.save(savedPurchaseOrder)));
                } else
                    throw new CustomException("The purchase order has already been approved",
                            HttpStatus.NOT_ACCEPTABLE);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PurchaseOrder not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderService.findById(id);
        if (optionalPurchaseOrder.isPresent()) {
            PurchaseOrder savedPurchaseOrder = optionalPurchaseOrder.get();
            if (savedPurchaseOrder.getCreatedBy().equals(user.getId()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.PURCHASE_ORDERS)) {
                purchaseOrderService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PurchaseOrder not found", HttpStatus.NOT_FOUND);
    }

    private PurchaseOrderShowDTO setPartQuantities(PurchaseOrderShowDTO purchaseOrderShowDTO) {
        Collection<PartQuantity> partQuantities = partQuantityService.findByPurchaseOrder(purchaseOrderShowDTO.getId());
        purchaseOrderShowDTO.setPartQuantities(partQuantities.stream().map(partQuantityMapper::toShowDto).collect(Collectors.toList()));
        return purchaseOrderShowDTO;
    }
}


