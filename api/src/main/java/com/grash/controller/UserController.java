package com.grash.controller;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.User;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.security.CurrentUser;
import com.grash.service.CompanyService;
import com.grash.service.IntercomService;
import com.grash.service.RoleService;
import com.grash.service.UserService;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Operations on users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final IntercomService intercomService;
    private final CompanyService companyService;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<UserResponseDTO>> search(@Parameter(description = "Search criteria for filtering " +
                                                                    "users") @RequestBody SearchCriteria searchCriteria,
                                                        @Parameter(hidden = true) @CurrentUser User user,
                                                        @RequestParam(defaultValue = "true") @Parameter(description =
                                                                "show only enabled users") boolean enabledOnly) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        if (enabledOnly) searchCriteria.getFilterFields().add(FilterField.builder()
                .field("enabled").value(true).operation("eq").build());
        return ResponseEntity.ok(userService.findBySearchCriteria(searchCriteria).map(userMapper::toResponseDto));
    }

    @PostMapping("/invite")
    @PreAuthorize("permitAll()")

    public SuccessResponse invite(@Parameter(description = "User invitation data") @RequestBody UserInvitationDTO invitation,
                                  @Parameter(hidden = true) @CurrentUser User user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            int companyUsersCount =
                    (int) userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.isEnabled() && user1.isEnabledInSubscriptionAndPaid()).count();
            Optional<Role> optionalRole = roleService.findById(invitation.getRole().getId());
            if (optionalRole.isPresent() && optionalRole.get().belongsToCompany(user.getCompany())) {
                if (companyUsersCount + invitation.getEmails().size() <= user.getCompany().getSubscription().getUsersCount() || !optionalRole.get().isPaid()) {
                    invitation.getEmails().forEach(email ->
                            userService.invite(email, optionalRole.get(), user, invitation.getDisableSendingEmail())
                    );

                    // Fire Intercom event for first user invitation
                    if (!user.getCompany().isInvitedUsers() && !invitation.getEmails().isEmpty()) {
                        user.getCompany().setInvitedUsers(true);
                        companyService.update(user.getCompany());
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("invited_count", invitation.getEmails().size());
                        intercomService.createCompanyActivationEvent(
                                "first-users-invited",
                                user.getCompany().getId(),
                                user.getEmail(),
                                metadata
                        );
                    }

                    return new SuccessResponse(true, "Users have been invited");
                } else
                    throw new CustomException("Your current subscription doesn't allow you to invite that many users"
                            , HttpStatus.NOT_ACCEPTABLE);

            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public Collection<UserMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user,
                                           @Parameter(description = "Include requesters in the response") @RequestParam(required = false) Boolean withRequesters) {
        return Boolean.TRUE.equals(withRequesters) ?
                userService.findByCompany(user.getCompany().getId()).stream()
                        .filter(User::isEnabled).map(userMapper::toMiniDto).collect(Collectors.toList()) :
                userService.findWorkersByCompany(user.getCompany().getId()).stream()
                        .filter(User::isEnabledInSubscription)
                        .filter(User::isEnabled)
                        .map(userMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/mini/disabled")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public Collection<UserMiniDTO> getMiniDisabled(@Parameter(hidden = true) @CurrentUser User user) {
        return userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> !user1.isEnabledInSubscription()).map(userMapper::toMiniDto).collect(Collectors.toList());
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public UserResponseDTO patch(@Parameter(description = "User fields to update") @Valid @RequestBody UserPatchDTO userReq,
                                 @Parameter(description = "User ID") @PathVariable("id") Long id,
                                 @Parameter(hidden = true) @CurrentUser User requester) {
        Optional<User> optionalUser = userService.findByIdAndCompany(id, requester.getCompany().getId());

        if (optionalUser.isPresent()) {
            User savedUser = optionalUser.get();
            if (requester.getId().equals(savedUser.getId()) ||
                    requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                return userMapper.toResponseDto(userService.update(id, userReq));
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("Can't get someone else's user", HttpStatus.NOT_ACCEPTABLE);
        }

    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public UserResponseDTO getById(@PathVariable("id") Long id, @Parameter(hidden = true) @CurrentUser User user) {
        Optional<User> optionalUser = userService.findByIdAndCompany(id, user.getCompany().getId());
        if (optionalUser.isPresent()) {
            User savedUser = optionalUser.get();
            if (user.getCompany().getId().equals(savedUser.getCompany().getId())) {
                return userMapper.toResponseDto(savedUser);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public UserResponseDTO patchRole(@Parameter(description = "User ID") @PathVariable("id") Long id,
                                     @Parameter(description = "Role ID to assign") @RequestParam("role") Long roleId,
                                     @Parameter(hidden = true) @CurrentUser User requester) {
        Optional<User> optionalUserToPatch = userService.findByIdAndCompany(id, requester.getCompany().getId());
        Optional<Role> optionalRole = roleService.findById(roleId);

        if (optionalUserToPatch.isPresent() && optionalRole.isPresent() && optionalRole.get().belongsToCompany(requester.getCompany())) {
            User userToPatch = optionalUserToPatch.get();
            if (requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                int usersCount =
                        (int) userService.findByCompany(requester.getCompany().getId()).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
                if (usersCount <= requester.getCompany().getSubscription().getUsersCount()) {
                    userToPatch.setRole(optionalRole.get());
                    return userMapper.toResponseDto(userService.save(userToPatch));
                } else
                    throw new CustomException("Company subscription users count doesn't allow this operation",
                            HttpStatus.NOT_ACCEPTABLE);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User or role not found", HttpStatus.NOT_FOUND);
        }

    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public UserResponseDTO disable(@PathVariable("id") Long id,
                                   @Parameter(hidden = true) @CurrentUser User requester) {
        Optional<User> optionalUserToDisable = userService.findByIdAndCompany(id, requester.getCompany().getId());

        if (optionalUserToDisable.isPresent()) {
            User userToDisable = optionalUserToDisable.get();
            if (requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                userToDisable.setEnabled(false);
                userToDisable.setEnabledInSubscription(false);
                return userMapper.toResponseDto(userService.save(userToDisable));
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User or role not found", HttpStatus.NOT_FOUND);
        }

    }

    @PatchMapping("/soft-delete/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public UserResponseDTO softDelete(@PathVariable("id") Long id,
                                      @Parameter(hidden = true) @CurrentUser User requester) {
        Optional<User> optionalUserToSoftDelete = userService.findByIdAndCompany(id, requester.getCompany().getId());

        if (optionalUserToSoftDelete.isPresent()) {
            User userToSoftDelete = optionalUserToSoftDelete.get();
            if (requester.getId().equals(id) || requester.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                userToSoftDelete.setEnabled(false);
                userToSoftDelete.setEnabledInSubscription(false);
                userToSoftDelete.setEmail(userToSoftDelete.getEmail().concat("_".concat(id.toString())));
                return userMapper.toResponseDto(userService.save(userToSoftDelete));
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }
    }
}



