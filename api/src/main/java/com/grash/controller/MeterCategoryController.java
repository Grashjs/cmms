package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.MeterCategory;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.MeterCategoryService;
import com.grash.service.UserService;
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
@RequestMapping("/meter-categories")
@Tag(name = "meterCategory")
@RequiredArgsConstructor
public class MeterCategoryController {

    private final MeterCategoryService meterCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")

    public Collection<MeterCategory> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return meterCategoryService.findByCompany(user.getCompany().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return meterCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public MeterCategory getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
            Optional<MeterCategory> optionalMeterCategory = meterCategoryService.findById(id);
            if (optionalMeterCategory.isPresent()) {
                MeterCategory savedMeterCategory = optionalMeterCategory.get();
                return savedMeterCategory;
            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);

    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    MeterCategory create(@Valid @RequestBody MeterCategory meterCategoryReq,
                         HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return meterCategoryService.create(meterCategoryReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public MeterCategory patch(@Valid @RequestBody CategoryPatchDTO meterCategory,
                               @PathVariable("id") Long id,
                               HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<MeterCategory> optionalMeterCategory = meterCategoryService.findById(id);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {

            if (optionalMeterCategory.isPresent()) {
                MeterCategory savedMeterCategory = optionalMeterCategory.get();
                return meterCategoryService.update(id, meterCategory);
            } else throw new CustomException("MeterCategory not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<MeterCategory> optionalMeterCategory = meterCategoryService.findById(id);
        if (optionalMeterCategory.isPresent()) {
            MeterCategory savedMeterCategory = optionalMeterCategory.get();
            if (savedMeterCategory.getCreatedBy().equals(user.getId()) || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.CATEGORIES)) {
                meterCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("MeterCategory not found", HttpStatus.NOT_FOUND);
    }

}


