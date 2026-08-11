package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AssetMiniDTO;
import com.grash.dto.AssetPatchDTO;
import com.grash.dto.AssetPostDTO;
import com.grash.dto.AssetShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.mapper.AssetMapper;
import com.grash.model.Asset;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.*;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assets")
@Tag(name = "Assets", description = "Operations on assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetMapper assetMapper;
    private final UserService userService;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<AssetShowDTO>> search(@Parameter(description = "Search criteria for filtering assets") @RequestBody SearchCriteria searchCriteria,
                                                     HttpServletRequest req) {
        User user = userService.whoami(req);
        return ResponseEntity.ok(assetService.findBySearchCriteria(assetService.getSearchCriteria(user, searchCriteria)));
    }

    @GetMapping("/nfc")
    @PreAuthorize("permitAll()")
    public AssetMiniDTO getByNfcId(@RequestParam @Parameter(description = "NFC identifier of the asset") String nfcId,
                                   @Parameter(hidden = true) @CurrentUser User user) {
        return assetMapper.toMiniDto(assetService.getByNfcIdAndCompany(nfcId, user));
    }

    @GetMapping("/barcode")
    @PreAuthorize("permitAll()")
    public AssetMiniDTO getByBarcode(@RequestParam @Parameter(description = "Barcode of the asset") String data,
                                     @Parameter(hidden = true) @CurrentUser User user) {
        return assetMapper.toMiniDto(assetService.getByBarcodeAndCompany(data, user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public AssetShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        return assetMapper.toShowDto(assetService.checkAccessToAssetId(id, user), assetService);
    }

    @GetMapping("/location/{id}")
    @PreAuthorize("permitAll()")
    public Collection<AssetShowDTO> getByLocation(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Collection<Asset> assets = assetService.findByLocationAndUser(id, user);
        Set<Long> parentIdsWithChildren = assetService.getParentIdsWithChildren(
                assets.stream().map(Asset::getId).collect(Collectors.toList()));
        return assets.stream().map(asset -> assetMapper.toShowDto(asset, parentIdsWithChildren)).collect(Collectors.toList());
    }

    @GetMapping("/part/{id}")
    @PreAuthorize("permitAll()")
    public Collection<AssetShowDTO> getByPart(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Collection<Asset> assets = assetService.findByPartAndUser(id, user);
        Set<Long> parentIdsWithChildren = assetService.getParentIdsWithChildren(
                assets.stream().map(Asset::getId).collect(Collectors.toList()));
        return assets.stream().map(asset -> assetMapper.toShowDto(asset, parentIdsWithChildren)).collect(Collectors.toList());
    }

    @GetMapping("/children/{id}")
    @PreAuthorize("permitAll()")
    @Deprecated
    public List<AssetShowDTO> getChildrenById(@PathVariable("id") Long id,
                                              HttpServletRequest req) {
        User user = userService.whoami(req);
        List<Asset> assets = assetService.findChildren(id, user, Pageable.unpaged());
        Set<Long> parentIdsWithChildren = assetService.getParentIdsWithChildren(
                assets.stream().map(Asset::getId).collect(Collectors.toList()));
        return assets.stream().map(asset -> assetMapper.toShowDto(asset, parentIdsWithChildren)).collect(Collectors.toList());
    }

    @GetMapping("/children/{id}/paginated")
    @PreAuthorize("permitAll()")
    public Page<AssetShowDTO> getChildrenByIdPaginated(@PathVariable("id") Long id,
                                                       Pageable pageable,
                                                       HttpServletRequest req) {
        User user = userService.whoami(req);
        Page<Asset> assetPage = assetService.findChildrenPaginated(id, user, pageable);
        Set<Long> parentIdsWithChildren = assetService.getParentIdsWithChildren(
                assetPage.getContent().stream().map(Asset::getId).collect(Collectors.toList()));
        return assetPage.map(asset -> assetMapper.toShowDto(asset, parentIdsWithChildren));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AssetShowDTO create(@Parameter(description = "Asset data to create") @Valid @RequestBody AssetPostDTO assetReq,
                               HttpServletRequest req) {
        User user = userService.whoami(req);
        return assetMapper.toShowDto(assetService.createByUser(assetReq, user), assetService);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AssetShowDTO patch(@Parameter(description = "Asset fields to update") @Valid @RequestBody AssetPatchDTO asset,
                              @PathVariable("id") Long id,
                              HttpServletRequest req) {
        User user = userService.whoami(req);
        return assetMapper.toShowDto(assetService.patch(id, asset, user), assetService);
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<AssetMiniDTO> getMini(@RequestParam(required = false) @Parameter(description = "Filter assets " +
            "by location ID. If not provided, returns all assets") Long locationId, HttpServletRequest req) {
        User user = userService.whoami(req);
        return assetService.findMini(locationId, user).stream().map(assetMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/public/mini/{portalUUID}")
    public Collection<AssetMiniDTO> getMiniPublic(@PathVariable String portalUUID,
                                                  @RequestParam(required = false) @Parameter(description = "Filter " +
                                                          "assets by location ID. If not provided, returns all assets" +
                                                          " for the portal") Long locationId,
                                                  HttpServletRequest req) {
        return assetService.findMiniPublic(portalUUID, locationId, Helper.extractClientIp(req)).stream().map(assetMapper::toMiniDto).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        assetService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}
