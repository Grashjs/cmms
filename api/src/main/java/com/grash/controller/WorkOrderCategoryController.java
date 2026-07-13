package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.UserService;
import com.grash.service.WorkOrderCategoryService;
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
@RequestMapping("/work-order-categories")
@Api(tags = "workOrderCategory")
@RequiredArgsConstructor
public class WorkOrderCategoryController {

    private final WorkOrderCategoryService workOrderCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderCategory not found")})
    public Collection<WorkOrderCategory> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return workOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return workOrderCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderCategory not found")})
    public WorkOrderCategory getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderCategory> optionalWorkOrderCategory = workOrderCategoryService.findById(id);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
            if (optionalWorkOrderCategory.isPresent()) {
                WorkOrderCategory savedWorkOrderCategory = optionalWorkOrderCategory.get();
                return savedWorkOrderCategory;
            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);

    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public WorkOrderCategory create(@ApiParam("WorkOrderCategory") @Valid @RequestBody WorkOrderCategory workOrderCategory, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return workOrderCategoryService.create(workOrderCategory);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrderCategory not found")})
    public WorkOrderCategory patch(@ApiParam("WorkOrderCategory") @Valid @RequestBody CategoryPatchDTO categoryPatchDTO, @ApiParam("id") @PathVariable("id") Long id,
                                   HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderCategory> optionalWorkOrderCategory = workOrderCategoryService.findById(id);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {

            if (optionalWorkOrderCategory.isPresent()) {
                WorkOrderCategory savedWorkOrderCategory = optionalWorkOrderCategory.get();
                return workOrderCategoryService.update(id, categoryPatchDTO);
            } else throw new CustomException("WorkOrderCategory not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrderCategory not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<WorkOrderCategory> optionalWorkOrderCategory = workOrderCategoryService.findById(id);
        if (optionalWorkOrderCategory.isPresent()) {
            WorkOrderCategory savedWorkOrderCategory = optionalWorkOrderCategory.get();
            if (savedWorkOrderCategory.getCreatedBy().equals(user.getId()) || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.CATEGORIES)) {
                workOrderCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrderCategory not found", HttpStatus.NOT_FOUND);
    }

}
