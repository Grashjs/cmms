package com.grash.controller;

import com.grash.dto.GeneralPreferencesPatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.CompanySettings;
import com.grash.model.GeneralPreferences;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.GeneralPreferencesService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/general-preferences")
@Tag(name = "generalPreferences")
@RequiredArgsConstructor
public class GeneralPreferencesController {


    private final GeneralPreferencesService generalPreferencesService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<GeneralPreferences> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        CompanySettings companySettings = user.getCompany().getCompanySettings();
        return generalPreferencesService.findByCompanySettings(companySettings.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public GeneralPreferences getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<GeneralPreferences> optionalGeneralPreferences = generalPreferencesService.findById(id);
        if (optionalGeneralPreferences.isPresent()) {
            return generalPreferencesService.findById(id).get();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public GeneralPreferences patch(@Valid @RequestBody GeneralPreferencesPatchDTO generalPreferences,
                                    @PathVariable("id") Long id,
                                    HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<GeneralPreferences> optionalGeneralPreferences = generalPreferencesService.findById(id);

        if (optionalGeneralPreferences.isPresent()) {
            GeneralPreferences savedGeneralPreferences = optionalGeneralPreferences.get();
            if (savedGeneralPreferences.getCompanySettings().getId().equals(user.getCompany().getCompanySettings().getId())
                    && user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return generalPreferencesService.update(id, generalPreferences);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("Can't get someone else's generalPreferences", HttpStatus.NOT_ACCEPTABLE);
        }

    }

}


