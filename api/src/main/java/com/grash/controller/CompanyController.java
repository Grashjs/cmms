package com.grash.controller;

import com.grash.dto.CompanyPatchDTO;
import com.grash.dto.CompanyShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CompanyMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.CacheService;
import com.grash.service.CompanyService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/companies")
@Tag(name = "company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    private final UserService userService;
    private final CompanyMapper companyMapper;
    private final CacheService cacheService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public CompanyShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Company> companyOptional = companyService.findById(id);
        if (companyOptional.isPresent()) {
            return companyMapper.toShowDto(companyService.findById(id).get());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public CompanyShowDTO patch(@Valid @RequestBody CompanyPatchDTO company,
                                @PathVariable("id") Long id,
                                HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Company> optionalCompany = companyService.findById(id);

        if (optionalCompany.isPresent()) {
            Company savedCompany = optionalCompany.get();
            if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            Company newCompany = companyService.update(id, company);
            user.setCompany(newCompany);
            cacheService.putUserInCache(user);
            return companyMapper.toShowDto(newCompany);
        } else throw new CustomException("Company not found", HttpStatus.NOT_FOUND);
    }

}


