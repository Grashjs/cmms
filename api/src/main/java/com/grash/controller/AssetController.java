package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetMiniDTO;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetMapper;
import com.grash.model.Asset;
import com.grash.model.Location;
import com.grash.model.OwnUser;
import com.grash.model.Part;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.security.CurrentUser;
import com.grash.service.*;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assets")
@Tag(name = "asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetMapper assetMapper;
    private final UserService userService;
    private final LocationService locationService;
    private final PartService partService;
    private final MessageSource messageSource;
    private final EntityManager em;
    private final LicenseService licenseService;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<AssetShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                     HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(assetService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/nfc")
    @PreAuthorize("permitAll()")
    public AssetShowDTO getByNfcId(@RequestParam String nfcId,
                                   @ApiIgnore @CurrentUser OwnUser user) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.NFC_BARCODE))
            throw new CustomException("You need a license to scan an asset", HttpStatus.FORBIDDEN);
        Optional<Asset> optionalAsset = assetService.findByNfcIdAndCompany(nfcId, user.getCompany().getId());
        return getAsset(optionalAsset, user);
    }

    @GetMapping("/barcode")
    @PreAuthorize("permitAll()")
    public AssetShowDTO getByBarcode(@RequestParam String data,
                                     @ApiIgnore @CurrentUser OwnUser user) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.NFC_BARCODE))
            throw new CustomException("You need a license to scan an asset", HttpStatus.FORBIDDEN);
        Optional<Asset> optionalAsset = assetService.findByBarcodeAndCompany(data, user.getCompany().getId());
        return getAsset(optionalAsset, user);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public AssetShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);
        return getAsset(optionalAsset, user);
    }

    private AssetShowDTO getAsset(Optional<Asset> optionalAsset, OwnUser user) {
        if (optionalAsset.isPresent()) {
            Asset savedAsset = optionalAsset.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS) || savedAsset.getCreatedBy().equals(user.getId()))) {
                return assetMapper.toShowDto(savedAsset, assetService);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/location/{id}")
    @PreAuthorize("permitAll()")
    public Collection<AssetShowDTO> getByLocation(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            return assetService.findByLocation(id).stream().map(asset -> assetMapper.toShowDto(asset, assetService)).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }


    @GetMapping("/part/{id}")
    @PreAuthorize("permitAll()")
    public Collection<AssetShowDTO> getByPart(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            return optionalPart.get().getAssets().stream().map(asset -> assetMapper.toShowDto(asset, assetService)).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/children/{id}")
    @PreAuthorize("permitAll()")
    public List<AssetShowDTO> getChildrenById(@PathVariable("id") Long id,
                                              Pageable pageable,
                                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (id.equals(0L) && user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            return assetService.findByCompanyAndParentAssetNull(user.getCompany().getId(), pageable).stream().map(asset -> assetMapper.toShowDto(asset, assetService)).collect(Collectors.toList());
        }
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            Asset savedAsset = optionalAsset.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS)) {
                return assetService.findAssetChildren(id, pageable.getSort()).stream().map(asset -> assetMapper.toShowDto(asset,
                        assetService)).collect(Collectors.toList());
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);

        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AssetShowDTO patch(@Valid @RequestBody AssetPatchDTO asset,
                              @PathVariable("id") Long id,
                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);

        if (optionalAsset.isPresent()) {
            Asset savedAsset = optionalAsset.get();
            em.detach(savedAsset);
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.ASSETS) || savedAsset.getCreatedBy().equals(user.getId())
            ) {
                if (!asset.getStatus().isReallyDown() && savedAsset.getStatus().isReallyDown()) {
                    assetService.stopDownTime(savedAsset.getId(), Helper.getLocale(user));
                } else if (asset.getStatus().isReallyDown() && !savedAsset.getStatus().isReallyDown()) {
                    assetService.triggerDownTime(savedAsset.getId(), Helper.getLocale(user), asset.getStatus());
                }
                if (asset.getBarCode() != null) {
                    Optional<Asset> optionalAssetWithSameBarCode =
                            assetService.findByBarcodeAndCompany(asset.getBarCode(), user.getCompany().getId());
                    if (optionalAssetWithSameBarCode.isPresent() && !optionalAssetWithSameBarCode.get().getId().equals(id)) {
                        throw new CustomException("Asset with same barcode exists", HttpStatus.NOT_ACCEPTABLE);
                    }
                }
                if (asset.getNfcId() != null) {
                    Optional<Asset> optionalAssetWithSameNfcId = assetService.findByNfcIdAndCompany(asset.getNfcId(),
                            user.getCompany().getId());
                    if (optionalAssetWithSameNfcId.isPresent() && !optionalAssetWithSameNfcId.get().getId().equals(id)) {
                        throw new CustomException("Asset with same nfc code exists", HttpStatus.NOT_ACCEPTABLE);
                    }
                }
                if (asset.getParentAsset() != null && asset.getParentAsset().getId().equals(id))
                    throw new CustomException("Parent asset cannot be the same id", HttpStatus.NOT_ACCEPTABLE);
                Asset patchedAsset = assetService.update(id, asset);
                assetService.patchNotify(savedAsset, patchedAsset, Helper.getLocale(user));
                return assetMapper.toShowDto(patchedAsset, assetService);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Asset not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            Asset savedAsset = optionalAsset.get();
            if (user.getId().equals(savedAsset.getCreatedBy()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.ASSETS)) {
                assetService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Asset not found", HttpStatus.NOT_FOUND);
    }

}



