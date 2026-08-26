package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.WorkOrderCategory;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.WorkOrderCategoryService;
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
@RequestMapping("/work-order-categories")
@Tag(name = "Work Order Categories", description = "Operations on work order categories")
@RequiredArgsConstructor
public class WorkOrderCategoryController {

    private final WorkOrderCategoryService workOrderCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<WorkOrderCategory> getAll(HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return workOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return workOrderCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public WorkOrderCategory getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<WorkOrderCategory> optionalWorkOrderCategory = workOrderCategoryService.findById(id);
        if (optionalWorkOrderCategory.isPresent()) {
            if (!optionalWorkOrderCategory.get().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return workOrderCategoryService.findById(id).get();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkOrderCategory create(@Parameter(description = "WorkOrder category to create") @Valid @RequestBody WorkOrderCategory workOrderCategoryReq,
                                    HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return workOrderCategoryService.create(workOrderCategoryReq, user);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkOrderCategory patch(@Parameter(description = "WorkOrder category fields to update") @Valid @RequestBody CategoryPatchDTO workOrderCategory,
                                   @PathVariable("id") Long id,
                                   HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<WorkOrderCategory> optionalWorkOrderCategory = workOrderCategoryService.findById(id);
        if (optionalWorkOrderCategory.isPresent()) {
            if (!optionalWorkOrderCategory.get().canBeEditedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return workOrderCategoryService.update(id, workOrderCategory);
        } else {
            throw new CustomException("Category not found", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<WorkOrderCategory> optionalWorkOrderCategory = workOrderCategoryService.findById(id);
        if (optionalWorkOrderCategory.isPresent()) {
            if (optionalWorkOrderCategory.get().canBeDeletedBy(user)) {
                workOrderCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrderCategory not found", HttpStatus.NOT_FOUND);
    }

}


