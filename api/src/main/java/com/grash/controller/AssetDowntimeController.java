package com.grash.controller;

import com.grash.dto.AssetDowntimePatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.AssetDowntime;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.AssetDowntimeService;
import com.grash.service.AssetService;
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
@RequestMapping("/asset-downtimes")
@Api(tags = "assetDowntime")
@RequiredArgsConstructor
public class AssetDowntimeController {

    private final AssetDowntimeService assetDowntimeService;
    private final UserService userService;
    private final AssetService assetService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetDowntime not found")})
    public AssetDowntime getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS)) {
            Optional<AssetDowntime> optionalAssetDowntime = assetDowntimeService.findById(id);
            if (optionalAssetDowntime.isPresent()) {
                return assetDowntimeService.findById(id).get();
            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public AssetDowntime create(@ApiParam("AssetDowntime") @Valid @RequestBody AssetDowntime assetDowntimeReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(assetDowntimeReq.getAsset().getId());
        if (!optionalAsset.isPresent()) {
            throw new CustomException("Asset Not found", HttpStatus.BAD_REQUEST);
        }
        if (optionalAsset.get().getRealCreatedAt().after(assetDowntimeReq.getStartsOn())) {
            throw new CustomException("The downtime can't occur before the asset in service date", HttpStatus.NOT_ACCEPTABLE);
        }
        if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.ASSETS) || optionalAsset.get().getCreatedBy().equals(user.getId())) {
            return assetDowntimeService.create(assetDowntimeReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/asset/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Labor not found")})
    public Collection<AssetDowntime> getByAsset(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            return assetDowntimeService.findByAsset(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "AssetDowntime not found")})
    public AssetDowntime patch(@ApiParam("AssetDowntime") @Valid @RequestBody AssetDowntimePatchDTO assetDowntime,
                               @ApiParam("id") @PathVariable("id") Long id,
                               HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<AssetDowntime> optionalAssetDowntime = assetDowntimeService.findById(id);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.CATEGORIES)) {
            if (optionalAssetDowntime.isPresent()) {
                if (canPatchAsset(optionalAssetDowntime.get().getAsset(), user)) {
                    return assetDowntimeService.update(id, assetDowntime);
                } else {
                    throw new CustomException("Can't patch assetDowntime of someone else", HttpStatus.NOT_ACCEPTABLE);
                }
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
            @ApiResponse(code = 404, message = "AssetDowntime not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<AssetDowntime> optionalAssetDowntime = assetDowntimeService.findById(id);
        if (optionalAssetDowntime.isPresent()) {
            if (canPatchAsset(optionalAssetDowntime.get().getAsset(), user)) {
                assetDowntimeService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("AssetDowntime not found", HttpStatus.NOT_FOUND);
    }

    private boolean canPatchAsset(Asset asset, OwnUser user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.ASSETS) || asset.getCreatedBy().equals(user.getId());
    }
}
