package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.CompanySettings;
import com.grash.model.User;
import com.grash.service.CompanySettingsService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/company-settings")
@Tag(name = "Company Settings", description = "Operations on company settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public CompanySettings getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<CompanySettings> companySettingsOptional = companySettingsService.findById(id);
        if (companySettingsOptional.isPresent()) {
            CompanySettings savedCompanySettings = companySettingsOptional.get();
            if (!savedCompanySettings.getCompany().getId().equals(user.getCompany().getId()))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return savedCompanySettings;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }
}

