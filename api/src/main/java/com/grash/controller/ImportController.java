package com.grash.controller;

import com.grash.dto.imports.*;
import com.grash.dto.SuccessResponse;
import com.grash.model.User;
import com.grash.model.enums.ImportEntity;
import com.grash.model.enums.Language;
import com.grash.security.CurrentUser;
import com.grash.service.AsyncImportService;
import com.grash.service.ImportService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/import")
@Tag(name = "Import", description = "Operations for importing data")
@RequiredArgsConstructor
public class ImportController {

    private final AsyncImportService asyncImportService;
    private final ImportService importService;

    @PostMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse importWorkOrders(@Parameter(description = "List of work orders to import") @Valid @RequestBody List<WorkOrderImportDTO> toImport,
                                            @Parameter(description = "Unique identifier for tracking " +
                                                    "the import job") @RequestParam String uuid,
                                            @Parameter(hidden = true) @CurrentUser User user) {
        return asyncImportService.importWorkOrders(user, toImport, uuid);
    }

    @PostMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse importAssets(@Parameter(description = "List of assets to import") @Valid @RequestBody List<AssetImportDTO> toImport,
                                        @Parameter(description = "Unique identifier for tracking the " +
                                                "import job") @RequestParam String uuid,
                                        @Parameter(hidden = true) @CurrentUser User user) {
        return asyncImportService.importAssets(user, toImport, uuid);
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse importLocations(@Parameter(description = "List of locations to import") @Valid @RequestBody List<LocationImportDTO> toImport,
                                           @Parameter(description = "Unique identifier for tracking " +
                                                   "the import job") @RequestParam String uuid,
                                           @Parameter(hidden = true) @CurrentUser User user) {
        return asyncImportService.importLocations(user, toImport, uuid);
    }

    @PostMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse importMeters(@Parameter(description = "List of meters to import") @Valid @RequestBody List<MeterImportDTO> toImport,
                                        @Parameter(description = "Unique identifier for tracking the " +
                                                "import job") @RequestParam String uuid,
                                        @Parameter(hidden = true) @CurrentUser User user) {
        return asyncImportService.importMeters(user, toImport, uuid);
    }

    @PostMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse importParts(@Parameter(description = "List of parts to import") @Valid @RequestBody List<PartImportDTO> toImport,
                                       @Parameter(description = "Unique identifier for tracking the " +
                                               "import job") @RequestParam String uuid,
                                       @Parameter(hidden = true) @CurrentUser User user) {
        return asyncImportService.importParts(user, toImport, uuid);
    }

    @PostMapping("/preventive-maintenances")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse importPreventiveMaintenances(@Parameter(description = "List of preventive " +
            "maintenances to import") @Valid @RequestBody List<PreventiveMaintenanceImportDTO> toImport,
                                                        @Parameter(description = "Unique identifier " +
                                                                "for tracking the import job") @RequestParam String uuid,
                                                        @Parameter(hidden = true) @CurrentUser User user) {
        return asyncImportService.importPreventiveMaintenances(user, toImport, uuid);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public byte[] downloadTemplate(@Parameter(description = "Language for the import template") @RequestParam Language language,
                                   @Parameter(description = "Entity type to import") @RequestParam ImportEntity importEntity) throws IOException {
        return importService.getTemplate(language, importEntity);
    }
}