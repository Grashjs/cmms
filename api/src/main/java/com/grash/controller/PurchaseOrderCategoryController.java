package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.PurchaseOrderCategory;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PurchaseOrderCategoryService;
import com.grash.service.UserService;
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
import java.util.Optional;

@RestController
@RequestMapping("/purchase-order-categories")
@Api(tags = "purchaseOrderCategory")
@RequiredArgsConstructor
public class PurchaseOrderCategoryController {

    private final PurchaseOrderCategoryService PurchaseOrderCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "PurchaseOrderCategoryCategory not found")})
    public Collection<PurchaseOrderCategory> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return PurchaseOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return PurchaseOrderCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "PurchaseOrderCategory not found")})
    public PurchaseOrderCategory getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
            Optional<PurchaseOrderCategory> optionalPurchaseOrderCategory = PurchaseOrderCategoryService.findById(id);
            if (optionalPurchaseOrderCategory.isPresent()) {
                PurchaseOrderCategory savedPurchaseOrderCategory = optionalPurchaseOrderCategory.get();
                return savedPurchaseOrderCategory;
            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);

    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public PurchaseOrderCategory create(@ApiParam("PurchaseOrderCategory") @Valid @RequestBody PurchaseOrderCategory PurchaseOrderCategoryReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return PurchaseOrderCategoryService.create(PurchaseOrderCategoryReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PurchaseOrderCategory not found")})
    public PurchaseOrderCategory patch(@ApiParam("PurchaseOrderCategory") @Valid @RequestBody CategoryPatchDTO categoryPatchDTO, @ApiParam("id") @PathVariable("id") Long id,
                                       HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            Optional<PurchaseOrderCategory> optionalPurchaseOrderCategory = PurchaseOrderCategoryService.findById(id);
            if (optionalPurchaseOrderCategory.isPresent()) {
                PurchaseOrderCategory savedPurchaseOrderCategory = optionalPurchaseOrderCategory.get();
                return PurchaseOrderCategoryService.update(id, categoryPatchDTO);
            } else throw new CustomException("PurchaseOrderCategory not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PurchaseOrderCategory not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<PurchaseOrderCategory> optionalPurchaseOrderCategory = PurchaseOrderCategoryService.findById(id);
        if (optionalPurchaseOrderCategory.isPresent()) {
            PurchaseOrderCategory savedPurchaseOrderCategory = optionalPurchaseOrderCategory.get();
            if (savedPurchaseOrderCategory.getCreatedBy().equals(user.getId()) || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.CATEGORIES)) {
                PurchaseOrderCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PurchaseOrderCategory not found", HttpStatus.NOT_FOUND);
    }

}
