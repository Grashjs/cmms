package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationMiniDTO;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.LocationMapper;
import com.grash.model.Location;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.LocationService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations")
@Api(tags = "location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final LocationMapper locationMapper;
    private final UserService userService;
    private final EntityManager em;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "LocationCategory not found")})
    public List<LocationShowDTO> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.LOCATIONS)) {
                return locationService.findByCompany(user.getCompany().getId()).stream().filter(location -> {
                    boolean canViewOthers =
                            user.getRole().getViewOtherPermissions().contains(PermissionEntity.LOCATIONS);
                    return canViewOthers || location.getCreatedBy().equals(user.getId());
                }).map(location -> locationMapper.toShowDto(location, locationService)).collect(Collectors.toList());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else
            return locationService.getAll().stream().map(location -> locationMapper.toShowDto(location,
                    locationService)).collect(Collectors.toList());
    }

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<LocationShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                        HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.LOCATIONS)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(locationService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/children/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Location not found")})
    public Collection<LocationShowDTO> getChildrenById(@ApiParam("id") @PathVariable("id") Long id,
                                                       Pageable pageable,
                                                       HttpServletRequest req) {
        //only sort is used
        OwnUser user = userService.whoami(req);
        if (id.equals(0L) && user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            return locationService.findByCompany(user.getCompany().getId(), pageable.getSort()).stream().filter(location -> location.getParentLocation() == null).map(location -> locationMapper.toShowDto(location, locationService)).collect(Collectors.toList());
        }
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            Location savedLocation = optionalLocation.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.LOCATIONS)) {
                return locationService.findLocationChildren(id, pageable.getSort()).stream().map(location -> locationMapper.toShowDto(location, locationService)).collect(Collectors.toList());
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);

        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
    })
    public Collection<LocationMiniDTO> getMini(HttpServletRequest req) {
        OwnUser location = userService.whoami(req);
        return locationService.findByCompany(location.getCompany().getId()).stream().map(locationMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Location not found")})
    public LocationShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            Location savedLocation = optionalLocation.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.LOCATIONS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.LOCATIONS) || savedLocation.getCreatedBy().equals(user.getId()))) {
                return locationMapper.toShowDto(savedLocation, locationService);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public LocationShowDTO create(@ApiParam("Location") @Valid @RequestBody Location locationReq,
                                  HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.LOCATIONS)) {
            Location savedLocation = locationService.create(locationReq, user.getCompany());
            locationService.notify(savedLocation, Helper.getLocale(user));
            return locationMapper.toShowDto(savedLocation, locationService);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Location not found")})
    public LocationShowDTO patch(@ApiParam("Location") @Valid @RequestBody LocationPatchDTO location,
                                 @ApiParam("id") @PathVariable("id") Long id,
                                 HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            Location savedLocation = optionalLocation.get();
            em.detach(savedLocation);
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.LOCATIONS) || savedLocation.getCreatedBy().equals(user.getId())) {
                if (location.getParentLocation() != null && location.getParentLocation().getId().equals(id))
                    throw new CustomException("Parent location cannot be the same id", HttpStatus.NOT_ACCEPTABLE);

                Location patchedLocation = locationService.update(id, location);
                locationService.patchNotify(savedLocation, patchedLocation, Helper.getLocale(user));
                return locationMapper.toShowDto(patchedLocation, locationService);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Location not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Location not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            Location savedLocation = optionalLocation.get();
            if (savedLocation.getCreatedBy().equals(user.getId()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.LOCATIONS)) {
                locationService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Location not found", HttpStatus.NOT_FOUND);
    }

}
