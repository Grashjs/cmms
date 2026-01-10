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
@RequestMapping("/asset-downtimes")
@Tag(name = "assetDowntime")
@RequiredArgsConstructor
public class AssetDowntimeController {

    private final AssetDowntimeService assetDowntimeService;
    private final UserService userService;
    private final AssetService assetService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public AssetDowntime getById(@PathVariable("id") Long id, HttpServletRequest req) {
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
    public Collection<AssetDowntime> getByAsset(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            return assetDowntimeService.findByAsset(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AssetDowntime patch(@Valid @RequestBody AssetDowntimePatchDTO assetDowntime,
                               @PathVariable("id") Long id,
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
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
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


