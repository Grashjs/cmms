package com.grash.controller;

import com.grash.dto.license.LicensingState;
import com.grash.service.LicenseService;
import io.swagger.v3.oas.annotations.Hidden;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
@Hidden
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping("/state")
    public LicensingState getValidity(HttpServletRequest req) {
        return licenseService.getLicensingState();
    }
}


