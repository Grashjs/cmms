package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.PurchaseOrderCategory;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PurchaseOrderCategoryService;
import com.grash.service.UserService;
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
import java.util.Optional;

@RestController
@RequestMapping("/purchase-order-categories")
@Tag(name = "Purchase Order Categories", description = "Operations on purchase order categories")
@RequiredArgsConstructor
public class PurchaseOrderCategoryController {

    private final PurchaseOrderCategoryService purchaseOrderCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<PurchaseOrderCategory> getAll(HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return purchaseOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return purchaseOrderCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public PurchaseOrderCategory getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<PurchaseOrderCategory> optionalPurchaseOrderCategory = purchaseOrderCategoryService.findById(id);
        if (optionalPurchaseOrderCategory.isPresent()) {
            if (!optionalPurchaseOrderCategory.get().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return purchaseOrderCategoryService.findById(id).get();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public PurchaseOrderCategory create(@Parameter(description = "PurchaseOrder category to create") @Valid @RequestBody PurchaseOrderCategory purchaseOrderCategoryReq,
                                        HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return purchaseOrderCategoryService.create(purchaseOrderCategoryReq, user);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public PurchaseOrderCategory patch(@Parameter(description = "PurchaseOrder category fields to update") @Valid @RequestBody CategoryPatchDTO purchaseOrderCategory,
                                       @PathVariable("id") Long id,
                                       HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<PurchaseOrderCategory> optionalPurchaseOrderCategory = purchaseOrderCategoryService.findById(id);
        if (optionalPurchaseOrderCategory.isPresent()) {
            if (!optionalPurchaseOrderCategory.get().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return purchaseOrderCategoryService.update(id, purchaseOrderCategory);
        } else {
            throw new CustomException("Category not found", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<PurchaseOrderCategory> optionalPurchaseOrderCategory = purchaseOrderCategoryService.findById(id);
        if (optionalPurchaseOrderCategory.isPresent()) {
            if (optionalPurchaseOrderCategory.get().canBeDeletedBy(user)) {
                purchaseOrderCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PurchaseOrderCategory not found", HttpStatus.NOT_FOUND);
    }

}


