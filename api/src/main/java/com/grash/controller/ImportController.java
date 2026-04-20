package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.imports.*;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.enums.ImportEntity;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.AsyncImportService;
import com.grash.service.CompanyService;
import com.grash.service.IntercomService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/import")
@Tag(name = "Import", description = "Operations for importing data")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImportController {

    private final UserService userService;
    private final AsyncImportService asyncImportService;
    private final IntercomService intercomService;
    private final CompanyService companyService;

    @PostMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> importWorkOrders(@Parameter(description = "List of work orders to import") @Valid @RequestBody List<WorkOrderImportDTO> toImport,
                                                            HttpServletRequest req,
                                                            @Parameter(description = "Unique identifier for tracking " +
                                                                    "the import job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            asyncImportService.importWorkOrders(user, toImport, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> importAssets(@Parameter(description = "List of assets to import") @Valid @RequestBody List<AssetImportDTO> toImport,
                                                        HttpServletRequest req,
                                                        @Parameter(description = "Unique identifier for tracking the " +
                                                                "import job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.ASSETS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            asyncImportService.importAssets(user, toImport, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> importLocations(@Parameter(description = "List of locations to import") @Valid @RequestBody List<LocationImportDTO> toImport,
                                                           HttpServletRequest req,
                                                           @Parameter(description = "Unique identifier for tracking " +
                                                                   "the import job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.LOCATIONS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            asyncImportService.importLocations(user, toImport, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> importMeters(@Parameter(description = "List of meters to import") @Valid @RequestBody List<MeterImportDTO> toImport,
                                                        HttpServletRequest req,
                                                        @Parameter(description = "Unique identifier for tracking the " +
                                                                "import job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.METERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            asyncImportService.importMeters(user, toImport, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> importParts(@Parameter(description = "List of parts to import") @Valid @RequestBody List<PartImportDTO> toImport,
                                                       HttpServletRequest req,
                                                       @Parameter(description = "Unique identifier for tracking the " +
                                                               "import job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            asyncImportService.importParts(user, toImport, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/preventive-maintenances")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> importPreventiveMaintenances(@Parameter(description = "List of preventive " +
                                                                                "maintenances to import") @Valid @RequestBody List<PreventiveMaintenanceImportDTO> toImport,
                                                                        HttpServletRequest req,
                                                                        @Parameter(description = "Unique identifier " +
                                                                                "for tracking the import job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            asyncImportService.importPreventiveMaintenances(user, toImport, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public byte[] importMeters(@Parameter(description = "Language for the import template") @RequestParam Language language, @Parameter(description = "Entity type to import") @RequestParam ImportEntity importEntity,
                               HttpServletRequest req) throws IOException {
        String path =
                "import-templates/" + language.name().toLowerCase() + "/" + importEntity.name().toLowerCase() + ".csv";
        String fallbackPath = "import-templates/en/" + importEntity.name().toLowerCase() + ".csv";

        return readFileWithFallback(path, fallbackPath);
    }

    private byte[] readFileWithFallback(String path, String fallbackPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            resource = new ClassPathResource(fallbackPath);
        }

        return StreamUtils.copyToByteArray(resource.getInputStream());
    }
}


