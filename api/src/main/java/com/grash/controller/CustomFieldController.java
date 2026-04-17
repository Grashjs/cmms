package com.grash.controller;

import com.grash.dto.CustomFieldPatchDTO;
import com.grash.dto.CustomFieldPostDTO;
import com.grash.dto.CustomFieldShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomFieldMapper;
import com.grash.model.CustomField;
import com.grash.model.CompanySettings;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.CustomFieldService;
import com.grash.service.CompanySettingsService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/custom-fields")
@Tag(name = "Company Custom Fields", description = "Operations on company custom fields")
@RequiredArgsConstructor
public class CustomFieldController {

    private final CustomFieldService customFieldService;
    private final CompanySettingsService companySettingsService;
    private final UserService userService;
    private final CustomFieldMapper customFieldMapper;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public List<CustomFieldShowDTO> getAllByCompanySettings(
            HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<CompanySettings> optionalCompanySettings =
                companySettingsService.findById(user.getCompany().getCompanySettings().getId());

        if (optionalCompanySettings.isPresent()) {
            CompanySettings companySettings = optionalCompanySettings.get();
            checkAccessToCompanySettings(companySettings, user);
            List<CustomField> fields = customFieldService.getAllByCompanySettings(companySettings);
            return fields.stream()
                    .map(customFieldMapper::toShowDto)
                    .sorted(Comparator.comparing(CustomFieldShowDTO::getOrder))
                    .collect(Collectors.toList());
        } else throw new CustomException("Company Settings not found", HttpStatus.NOT_FOUND);
    }


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public CustomFieldShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<CustomField> optionalField = customFieldService.findById(id);

        if (optionalField.isPresent()) {
            CustomField field = optionalField.get();
            checkAccessToCompanySettings(field.getCompanySettings(), user);
            return customFieldMapper.toShowDto(field);
        } else throw new CustomException("Custom field not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CustomFieldShowDTO create(
            @Parameter(description = "Custom field to create") @Valid @RequestBody CustomFieldPostDTO fieldDto,
            HttpServletRequest req) {
        User user = userService.whoami(req);

        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }

        Optional<CompanySettings> optionalCompanySettings =
                companySettingsService.findById(user.getCompany().getCompanySettings().getId());
        if (optionalCompanySettings.isPresent()) {
            CustomField savedField = customFieldService.create(fieldDto, optionalCompanySettings.get());
            return customFieldMapper.toShowDto(savedField);
        } else throw new CustomException("Company Settings not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CustomFieldShowDTO patch(
            @Parameter(description = "Custom field fields to update") @Valid @RequestBody CustomFieldPatchDTO fieldDto,
            @PathVariable("id") Long id,
            HttpServletRequest req) {
        User user = userService.whoami(req);

        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }

        Optional<CustomField> optionalField = customFieldService.findById(id);
        if (optionalField.isPresent()) {
            CustomField field = optionalField.get();
            checkAccessToCompanySettings(field.getCompanySettings(), user);

            CustomField updatedField = customFieldService.update(id, fieldDto);
            return customFieldMapper.toShowDto(updatedField);
        } else throw new CustomException("Custom field not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }

        Optional<CustomField> optionalField = customFieldService.findById(id);
        if (optionalField.isPresent()) {
            CustomField field = optionalField.get();
            checkAccessToCompanySettings(field.getCompanySettings(), user);

            customFieldService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
        } else throw new CustomException("Custom field not found", HttpStatus.NOT_FOUND);
    }

    private void checkAccessToCompanySettings(CompanySettings companySettings, User user) {
        if (!companySettings.getCompany().getId().equals(user.getCompany().getId())) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    @PatchMapping("/reorder")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse reorder(
            @RequestBody List<Long> reorderList,
            HttpServletRequest req) {
        User user = userService.whoami(req);
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        customFieldService.reorder(reorderList, user.getCompany().getCompanySettings());
        return new SuccessResponse(true, "Reordered successfully");
    }
}





