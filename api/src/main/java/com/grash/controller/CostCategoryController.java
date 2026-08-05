package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.CostCategory;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.CostCategoryService;
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
@RequestMapping("/cost-categories")
@Tag(name = "Cost Categories", description = "Operations on cost categories")
@RequiredArgsConstructor
public class CostCategoryController {

    private final CostCategoryService costCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<CostCategory> getAll(HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return costCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return costCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public CostCategory getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<CostCategory> optionalCostCategory = costCategoryService.findById(id);
        if (optionalCostCategory.isPresent()) {
            if (!optionalCostCategory.get().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return costCategoryService.findById(id).get();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CostCategory create(@Parameter(description = "Cost category to create") @Valid @RequestBody CostCategory costCategoryReq,
                               HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return costCategoryService.create(costCategoryReq, user);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CostCategory patch(@Parameter(description = "Cost category fields to update") @Valid @RequestBody CategoryPatchDTO costCategory,
                              @PathVariable("id") Long id,
                              HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<CostCategory> optionalCostCategory = costCategoryService.findById(id);
        if (optionalCostCategory.isPresent()) {
            if (!optionalCostCategory.get().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return costCategoryService.update(id, costCategory);
        } else {
            throw new CustomException("Category not found", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<CostCategory> optionalCostCategory = costCategoryService.findById(id);
        if (optionalCostCategory.isPresent()) {
            if (optionalCostCategory.get().canBeDeletedBy(user)) {
                costCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("CostCategory not found", HttpStatus.NOT_FOUND);
    }

}


