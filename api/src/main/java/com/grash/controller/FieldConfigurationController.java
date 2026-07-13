package com.grash.controller;

import com.grash.dto.FieldConfigurationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.FieldConfiguration;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.FieldConfigurationService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/field-configurations")
@Api(tags = "fieldConfiguration")
@RequiredArgsConstructor
public class FieldConfigurationController {

    private final FieldConfigurationService fieldConfigurationService;
    private final UserService userService;

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "FieldConfiguration not found")})
    public FieldConfiguration patch(@ApiParam("FieldConfiguration") @Valid @RequestBody FieldConfigurationPatchDTO fieldConfiguration, @ApiParam("id") @PathVariable("id") Long id,
                                    HttpServletRequest req) {
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
