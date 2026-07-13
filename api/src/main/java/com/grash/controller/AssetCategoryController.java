package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.AssetCategory;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.AssetCategoryService;
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
@RequestMapping("/asset-categories")
@Api(tags = "assetCategory")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<AssetCategory> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
                return assetCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return assetCategoryService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public AssetCategory getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.CATEGORIES)) {
            Optional<AssetCategory> optionalAssetCategory = assetCategoryService.findById(id);
            if (optionalAssetCategory.isPresent()) {
                return assetCategoryService.findById(id).get();
            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public AssetCategory create(@ApiParam("AssetCategory") @Valid @RequestBody AssetCategory assetCategoryReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            return assetCategoryService.create(assetCategoryReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public AssetCategory patch(@ApiParam("AssetCategory") @Valid @RequestBody CategoryPatchDTO assetCategory,
                               @ApiParam("id") @PathVariable("id") Long id,
                               HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<AssetCategory> optionalAssetCategory = assetCategoryService.findById(id);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            if (optionalAssetCategory.isPresent()) {
                return assetCategoryService.update(id, assetCategory);
            } else {
                throw new CustomException("Category not found", HttpStatus.NOT_FOUND);
            }
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<AssetCategory> optionalAssetCategory = assetCategoryService.findById(id);
        if (optionalAssetCategory.isPresent()) {
            if (optionalAssetCategory.get().getCreatedBy().equals(user.getId()) || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.CATEGORIES)) {
                assetCategoryService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("AssetCategory not found", HttpStatus.NOT_FOUND);
    }

}
