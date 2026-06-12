package com.grash.controller;

import com.grash.dto.shiftConfiguration.ShiftConfigurationPatchDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationPostDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationShowDTO;
import com.grash.dto.shiftConfiguration.UserShiftDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.ShiftConfigurationMapper;
import com.grash.model.ShiftConfiguration;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.security.CurrentUser;
import com.grash.service.ShiftConfigurationService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shift-configurations")
@Tag(name = "ShiftConfiguration")
@RequiredArgsConstructor
public class ShiftConfigurationController {

    private final ShiftConfigurationService shiftConfigurationService;
    private final ShiftConfigurationMapper shiftConfigurationMapper;
    private final UserService userService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<UserShiftDTO> getUsers(
            @Parameter(description = "Comma-separated user IDs (optional, returns all company users if omitted)")
            @RequestParam(required = false) List<Long> userIds,
            HttpServletRequest req) {
        User currentUser = userService.whoami(req);
        if (!currentUser.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) || !currentUser.getRole().getViewPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
        return shiftConfigurationService.getUsersWithShiftConfig(userIds, currentUser.getCompany().getId());
    }

    @PatchMapping("/user/{userId}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ShiftConfigurationShowDTO patchForUser(@PathVariable Long userId,
                                                  @Valid @RequestBody ShiftConfigurationPatchDTO dto,
                                                  @CurrentUser User currentUser,
                                                  HttpServletRequest req) {
        User user = userService.findById(userId).orElseThrow(() -> new CustomException("User not found",
                HttpStatus.NOT_FOUND));
        if (!user.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS) || !user.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        }
        ShiftConfiguration patched = shiftConfigurationService.update(user, dto);
        return shiftConfigurationMapper.toShowDto(patched);
    }

}
