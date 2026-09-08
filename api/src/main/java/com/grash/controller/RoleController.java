package com.grash.controller;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.security.CurrentUser;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.service.RoleService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Operations on roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<Role> getAll(@Parameter(hidden = true) @CurrentUser User user) {
        return roleService.getAll(user);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Role getById(@PathVariable("id") Long id, @Parameter(hidden = true) @CurrentUser User user) {
        return roleService.getById(id, user);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    Role create(@Parameter(description = "Role data to create") @Valid @RequestBody Role roleReq,
                @Parameter(hidden = true) @CurrentUser User user) {
        return roleService.create(roleReq, user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Role patch(@Parameter(description = "Role fields to update") @Valid @RequestBody RolePatchDTO role,
                      @PathVariable("id") Long id,
                      @Parameter(hidden = true) @CurrentUser User user) {
        return roleService.patch(id, role, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        roleService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }
}