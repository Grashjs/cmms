package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.UserSettings;
import com.grash.service.UserService;
import com.grash.service.UserSettingsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/user-settings")
@Api(tags = "userSettings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final UserService userService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public UserSettings getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<UserSettings> optionalUserSettings = userSettingsService.findById(id);
        if (optionalUserSettings.isPresent()) {
            UserSettings userSettings = optionalUserSettings.get();
            return userSettings;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public UserSettings patch(@Valid @RequestBody UserSettings userSettings,
                              @PathVariable("id") Long id,
                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<UserSettings> optionalUserSettings = userSettingsService.findById(id);

        if (optionalUserSettings.isPresent()) {
            UserSettings savedUserSettings = optionalUserSettings.get();
            if (savedUserSettings.getId().equals(user.getUserSettings().getId())) {
                return userSettingsService.update(userSettings);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("Can't get someone else's userSettings", HttpStatus.NOT_ACCEPTABLE);
        }

    }

}


