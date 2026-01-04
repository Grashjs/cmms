package com.grash.controller;

import com.grash.dto.FloorPlanPatchDTO;
import com.grash.dto.FloorPlanShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.dto.license.LicensingState;
import com.grash.exception.CustomException;
import com.grash.mapper.FloorPlanMapper;
import com.grash.model.FloorPlan;
import com.grash.model.Location;
import com.grash.model.OwnUser;
import com.grash.service.FloorPlanService;
import com.grash.service.LicenseService;
import com.grash.service.LocationService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping("/state")
    public LicensingState getValidity(HttpServletRequest req) {
        return licenseService.getLicensingState();
    }
}
