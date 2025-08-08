package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetMiniDTO;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.SuccessResponse;
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
import com.grash.service.AssetService;
import com.grash.service.LocationService;
import com.grash.service.PartService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.el.ELManager;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assets")
@Api(tags = "asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetMapper assetMapper;
    private final UserService userService;
    private final LocationService locationService;
    private final PartService partService;
    private final MessageSource messageSource;
    private final EntityManager em;

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

    @GetMapping("/nfc/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Asset not found")})
    public AssetShowDTO getByNfcId(@ApiParam("id") @PathVariable("id") String nfcId,
                                   @ApiIgnore @CurrentUser OwnUser user) {
        Optional<Asset> optionalAsset = assetService.findByNfcIdAndCompany(nfcId, user.getCompany().getId());
        return getAsset(optionalAsset, user);
    }

    @GetMapping("/barcode/{data}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Asset not found")})
    public AssetShowDTO getByBarcode(@ApiParam("data") @PathVariable("data") String data,
                                     @ApiIgnore @CurrentUser OwnUser user) {
        Optional<Asset> optionalAsset = assetService.findByBarcodeAndCompany(data, user.getCompany().getId());
        return getAsset(optionalAsset, user);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Asset not found")})
    public AssetShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Asset not found")})
    public Collection<AssetShowDTO> getByLocation(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            return assetService.findByLocation(id).stream().map(asset -> assetMapper.toShowDto(asset, assetService)).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }


    @GetMapping("/part/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Assets for this part not found")})
    public Collection<AssetShowDTO> getByPart(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            return optionalPart.get().getAssets().stream().map(asset -> assetMapper.toShowDto(asset, assetService)).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/children/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Asset not found")})
    public List<AssetShowDTO> getChildrenById(@ApiParam("id") @PathVariable("id") Long id,
                                              Pageable pageable,
                                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (id.equals(0L) && user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            return assetService.findByCompany(user.getCompany().getId(), pageable.getSort()).stream().filter(asset -> asset.getParentAsset() == null).map(asset -> assetMapper.toShowDto(asset, assetService)).collect(Collectors.toList());
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public AssetShowDTO create(@ApiParam("Asset") @Valid @RequestBody Asset assetReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.ASSETS)) {
            if (assetReq.getBarCode() != null) {
                Optional<Asset> optionalAssetWithSameBarCode =
                        assetService.findByBarcodeAndCompany(assetReq.getBarCode(), user.getCompany().getId());
                if (optionalAssetWithSameBarCode.isPresent()) {
                    throw new CustomException("Asset with same barCode exists", HttpStatus.NOT_ACCEPTABLE);
                }
            }
            if (assetReq.getNfcId() != null) {
                Optional<Asset> optionalAssetWithSameNfcId = assetService.findByNfcIdAndCompany(assetReq.getNfcId(),
                        user.getCompany().getId());
                if (optionalAssetWithSameNfcId.isPresent()) {
                    throw new CustomException("Asset with same nfc code exists", HttpStatus.NOT_ACCEPTABLE);
                }
            }
            Asset createdAsset = assetService.create(assetReq, user);
            String message = messageSource.getMessage("notification_asset_assigned",
                    new Object[]{createdAsset.getName()}, Helper.getLocale(user));
            assetService.notify(createdAsset, messageSource.getMessage("new_assignment", null,
                    Helper.getLocale(user)), message);
            return assetMapper.toShowDto(createdAsset, assetService);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Asset not found")})
    public AssetShowDTO patch(@ApiParam("Asset") @Valid @RequestBody AssetPatchDTO asset,
                              @ApiParam("id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
    })
    public Collection<AssetMiniDTO> getMini(@RequestParam(required = false) Long locationId, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        List<Asset> assets = new ArrayList<>();
        if (locationId == null) {
            assets = assetService.findByCompany(user.getCompany().getId());
        } else {
            assets = assetService.findByLocation(locationId);
        }
        return assets.stream().map(assetMapper::toMiniDto).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Asset not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            Asset savedAsset = optionalAsset.get();
            if (savedAsset.getCreatedBy().equals(user.getId()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.ASSETS)) {
                assetService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Asset not found", HttpStatus.NOT_FOUND);
    }

}
