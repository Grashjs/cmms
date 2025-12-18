package com.grash.controller;

import com.grash.dto.imports.*;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.OwnUser;
import com.grash.model.enums.ImportEntity;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.ImportService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/import")
@Api(tags = "import")
@RequiredArgsConstructor
@Transactional
public class ImportController {

    private final UserService userService;
    private final ImportService importService;

    @PostMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importWorkOrders(@Valid @RequestBody List<WorkOrderImportDTO> toImport,
                                           HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            return importService.importWorkOrders(toImport, user.getCompany());
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importAssets(@Valid @RequestBody List<AssetImportDTO> toImport, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.ASSETS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            return importService.importAssets(toImport, user.getCompany());
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importLocations(@Valid @RequestBody List<LocationImportDTO> toImport,
                                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.LOCATIONS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            return importService.importLocations(toImport, user.getCompany());
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importMeters(@Valid @RequestBody List<MeterImportDTO> toImport, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.METERS) && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            return importService.importMeters(toImport, user.getCompany());
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importParts(@Valid @RequestBody List<PartImportDTO> toImport, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            return importService.importParts(toImport, user.getCompany());
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/preventive-maintenances")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importPreventiveMaintenances(@Valid @RequestBody List<PreventiveMaintenanceImportDTO> toImport,
                                                       HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            return importService.importPreventiveMaintenances(toImport, user.getCompany());
        } else {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public byte[] importMeters(@RequestParam Language language, @RequestParam ImportEntity importEntity,
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
