package com.grash.controller;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.RoleService;
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
@RequestMapping("/roles")
@Api(tags = "role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "RoleCategory not found")})
    public Collection<Role> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return roleService.findByCompany(user.getCompany().getId());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else return roleService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Role not found")})
    public Role getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Role> optionalRole = roleService.findById(id);
        if (optionalRole.isPresent()) {
            Role savedRole = optionalRole.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return savedRole;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Role create(@ApiParam("Role") @Valid @RequestBody Role roleReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        roleReq.setPaid(true);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ROLE)) {
            return roleService.create(roleReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Role not found")})
    public Role patch(@ApiParam("Role") @Valid @RequestBody RolePatchDTO role, @ApiParam("id") @PathVariable("id") Long id,
                      HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Role> optionalRole = roleService.findById(id);

        if (optionalRole.isPresent()) {
            Role savedRole = optionalRole.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return roleService.update(id, role);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Role not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Role not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Role> optionalRole = roleService.findById(id);
        if (optionalRole.isPresent()) {
            Role savedRole = optionalRole.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                roleService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Role not found", HttpStatus.NOT_FOUND);
    }

}
