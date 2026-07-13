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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-orders")
@Api(tags = "purchaseOrder")
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "PurchaseOrder not found")})
    public PurchaseOrderShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public PurchaseOrderShowDTO create(@ApiParam("PurchaseOrder") @Valid @RequestBody PurchaseOrder purchaseOrderReq,
                                       HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PURCHASE_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.PURCHASE_ORDER)) {
            PurchaseOrder savedPurchaseOrder = purchaseOrderService.create(purchaseOrderReq);
            Collection<Workflow> workflows =
                    workflowService.findByMainConditionAndCompany(WFMainCondition.PURCHASE_ORDER_CREATED,
                            user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runPurchaseOrder(workflow, savedPurchaseOrder));
            PurchaseOrderShowDTO result = setPartQuantities(purchaseOrderMapper.toShowDto(savedPurchaseOrder));
            double cost =
                    result.getPartQuantities().stream().mapToDouble(partQuantityShowDTO -> partQuantityShowDTO.getQuantity() * partQuantityShowDTO.getPart().getCost()).sum();
            String title = messageSource.getMessage("new_po", null, Helper.getLocale(user));
            String message = messageSource.getMessage("notification_new_po_request", new Object[]{result.getName(),
                            cost,
                            user.getCompany().getCompanySettings().getGeneralPreferences().getCurrency().getCode()},
                    Helper.getLocale(user));
            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("purchaseOrderLink", frontendUrl + "/app/purchase-orders/" + result.getId());
                put("message", message);
            }};
            Collection<OwnUser> usersToNotify = userService.findByCompany(user.getCompany().getId()).stream()
                    .filter(user1 -> user1.isEnabled() && user1.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) ||
                            user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN)).collect(Collectors.toList());
            notificationService.createMultiple(usersToNotify.stream().map(user1 -> new Notification(message, user1,
                    NotificationType.PURCHASE_ORDER, result.getId())).collect(Collectors.toList()), true, title);
            Collection<OwnUser> usersToMail =
                    usersToNotify.stream().filter(user1 -> user1.getUserSettings().shouldEmailUpdatesForPurchaseOrders()).collect(Collectors.toList());
            emailService2.sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail).toArray(String[]::new), messageSource.getMessage("new_po", null, Helper.getLocale(user)), mailVariables, "new-purchase-order.html", Helper.getLocale(user));
            return result;
        } else throw new

                CustomException("Access denied", HttpStatus.FORBIDDEN);

    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PurchaseOrder not found")})
    public PurchaseOrderShowDTO patch(@ApiParam("PurchaseOrder") @Valid @RequestBody PurchaseOrderPatchDTO purchaseOrder, @ApiParam("id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PurchaseOrder not found")})
    public PurchaseOrderShowDTO respond(@ApiParam("approved") @RequestParam("approved") boolean approved, @ApiParam(
                                                "id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PurchaseOrder not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
