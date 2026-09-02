package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.mapper.LocationMapper;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.LocationService;
import com.grash.utils.TenantAspectUtils;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations")
@Tag(name = "Locations", description = "Operations on locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final LocationMapper locationMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<LocationShowDTO>> search(@Parameter(description = "Search criteria for filtering " +
                                                                 "locations") @RequestBody SearchCriteria searchCriteria,
                                                        @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(TenantAspectUtils.executeWithDisabledCompanyCheck(() ->
                locationService.findBySearchCriteria(locationService.getSearchCriteria(user, searchCriteria))
                        .map(location -> locationMapper.toShowDto(location, locationService))
        ));
    }

    //TODO remove. Waiting for mobile app to switch to paginated version.
    @GetMapping("/children/{id}")
    @PreAuthorize("permitAll()")
    @Deprecated
    public Collection<LocationShowDTO> getChildrenById(@Parameter(description = "Location ID") @PathVariable("id") Long id,
                                                       @Parameter(hidden = true) @CurrentUser User user) {
        return locationService.getChildren(id, user).stream()
                .map(location -> locationMapper.toShowDto(location, locationService))
                .collect(Collectors.toList());
    }

    @GetMapping("/children/{id}/paginated")
    @PreAuthorize("permitAll()")
    public Page<LocationShowDTO> getChildrenByIdPaginated(@PathVariable("id") Long id,
                                                          Pageable pageable,
                                                          @Parameter(hidden = true) @CurrentUser User user) {
        return locationService.getChildrenPaginated(id, pageable, user)
                .map(location -> locationMapper.toShowDto(location, locationService));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<LocationMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user) {
        return locationService.findByCompany(user.getCompany().getId()).stream()
                .map(locationMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/public/mini/{portalUUID}")
    public Collection<LocationMiniDTO> getMiniPublic(@Parameter(description = "Portal UUID") @PathVariable String portalUUID, HttpServletRequest req) {
        return locationService.getMiniPublic(portalUUID, req).stream()
                .map(locationMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public LocationShowDTO getById(@Parameter(description = "Location ID") @PathVariable("id") Long id,
                                   @Parameter(hidden = true) @CurrentUser User user) {
        return locationMapper.toShowDto(locationService.getById(id, user), locationService);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    LocationShowDTO create(@Parameter(description = "Location data to create") @Valid @RequestBody LocationPostDTO locationReq,
                           @Parameter(hidden = true) @CurrentUser User user) {
        return locationMapper.toShowDto(locationService.create(locationReq, user), locationService);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public LocationShowDTO patch(@Parameter(description = "Location fields to update") @Valid @RequestBody LocationPatchDTO location,
                                 @Parameter(description = "Location ID") @PathVariable("id") Long id,
                                 @Parameter(hidden = true) @CurrentUser User user) {
        return locationMapper.toShowDto(locationService.patch(id, location, user), locationService);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@Parameter(description = "Location ID") @PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        locationService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}
