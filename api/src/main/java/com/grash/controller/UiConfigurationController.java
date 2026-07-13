package com.grash.controller;

import com.grash.dto.UiConfigurationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.UiConfiguration;
import com.grash.model.OwnUser;
import com.grash.model.UiConfiguration;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.UiConfigurationService;
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
@RequestMapping("/ui-configurations")
@Api(tags = "uiConfiguration")
@RequiredArgsConstructor
public class UiConfigurationController {

    private final UiConfigurationService uiConfigurationService;
    private final UserService userService;

    @PatchMapping()
    public UiConfiguration patch(@ApiParam("UiConfiguration") @Valid @RequestBody UiConfigurationPatchDTO uiConfiguration,
                                 HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<UiConfiguration> optionalUiConfiguration =
                uiConfigurationService.findByCompanySettings(user.getCompany().getCompanySettings().getId());

        if (optionalUiConfiguration.isPresent()) {
            UiConfiguration savedUiConfiguration = optionalUiConfiguration.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return uiConfigurationService.update(savedUiConfiguration.getId(), uiConfiguration);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("UiConfiguration not found", HttpStatus.NOT_FOUND);
    }
}
