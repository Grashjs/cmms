package com.grash.controller;

import com.grash.dto.FieldConfigurationPatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.FieldConfiguration;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.FieldConfigurationService;
import com.grash.service.LicenseService;
import com.grash.service.UserService;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/field-configurations")
@Tag(name = "fieldConfiguration")
@RequiredArgsConstructor
public class FieldConfigurationController {

    private final FieldConfigurationService fieldConfigurationService;
    private final UserService userService;
    private final LicenseService licenseService;

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public FieldConfiguration patch(@Valid @RequestBody FieldConfigurationPatchDTO fieldConfiguration, @PathVariable(
                                            "id") Long id,
                                    HttpServletRequest req) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.FIELD_CONFIGURATION))
            throw new CustomException("You need a license to edit field configurations", HttpStatus.FORBIDDEN);
        OwnUser user = userService.whoami(req);
        Optional<FieldConfiguration> optionalFieldConfiguration = fieldConfigurationService.findById(id);

        if (optionalFieldConfiguration.isPresent()) {
            FieldConfiguration savedFieldConfiguration = optionalFieldConfiguration.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                    && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.REQUEST_CONFIGURATION)) {
                return fieldConfigurationService.update(id, fieldConfiguration);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("FieldConfiguration not found", HttpStatus.NOT_FOUND);
    }


}


