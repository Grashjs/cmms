package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.ExportRequestDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/export")
@Tag(name = "Export", description = "Operations for exporting data")
@RequiredArgsConstructor
@Slf4j
public class ExportController {
    private final UserService userService;
    private final AsyncExportService asyncExportService;

    /**
     * Exports the rows a filter selects, with the columns the caller chose — the export half of
     * the saved-view feature. The GET variants below stay: they dump a whole entity and are what
     * the "export everything" menu entries use.
     * <p>
     * Access differs from the GET variant on purpose. That one requires the view-other
     * permission because it ignores filters and returns every row in the company. This one runs
     * the criteria through {@code WorkOrderService.getSearchCriteria}, which narrows to the
     * user's own records when they lack view-other, so the plain view permission is enough.
     * <p>
     * The view permission is checked here and not left to the criteria scoping, because that
     * method only <em>narrows</em> — a user with no work-order view permission at all gets no
     * narrowing from it (which is also why {@code POST /work-orders/search} is more permissive
     * than it looks). The export additionally runs on an {@code @Async} thread with no
     * SecurityContext, so {@code CompanyAudit.afterLoad} does not fire as a second line of
     * defence. This check is the only gate; do not remove it.
     */
    @PostMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportWorkOrdersFiltered(HttpServletRequest req,
                                                                    @Parameter(description = "Unique identifier for " +
                                                                            "tracking the export job")
                                                                    @RequestParam String uuid,
                                                                    @Parameter(description = "Row filter and column " +
                                                                            "selection")
                                                                    @RequestBody(required = false) ExportRequestDTO exportRequest) {
        User user = userService.whoami(req);
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        ExportRequestDTO request = exportRequest == null ? new ExportRequestDTO() : exportRequest;
        SearchCriteria criteria = request.getCriteria() == null ? new SearchCriteria() : request.getCriteria();
        asyncExportService.exportWorkOrdersFiltered(user, uuid, criteria, request.getColumns());
        return ResponseEntity.ok().body(new SuccessResponse(true, uuid));
    }

    /**
     * Exports the assets a filter selects. See {@link #exportWorkOrdersFiltered}.
     */
    @PostMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportAssetsFiltered(HttpServletRequest req,
                                                                @Parameter(description = "Unique identifier for " +
                                                                        "tracking the export job")
                                                                @RequestParam String uuid,
                                                                @Parameter(description = "Row filter and column " +
                                                                        "selection")
                                                                @RequestBody(required = false) ExportRequestDTO exportRequest) {
        User user = userService.whoami(req);
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        ExportRequestDTO request = exportRequest == null ? new ExportRequestDTO() : exportRequest;
        SearchCriteria criteria = request.getCriteria() == null ? new SearchCriteria() : request.getCriteria();
        asyncExportService.exportAssetsFiltered(user, uuid, criteria, request.getColumns());
        return ResponseEntity.ok().body(new SuccessResponse(true, uuid));
    }

    @GetMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportWorkOrders(HttpServletRequest req, @Parameter(description = "Unique " +
            "identifier for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            asyncExportService.exportWorkOrders(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportAssets(HttpServletRequest req, @Parameter(description = "Unique " +
            "identifier for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS)) {
            asyncExportService.exportAssets(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportLocations(HttpServletRequest req, @Parameter(description = "Unique " +
            "identifier for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.LOCATIONS)) {
            asyncExportService.exportLocations(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportParts(HttpServletRequest req, @Parameter(description = "Unique " +
            "identifier for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
            asyncExportService.exportParts(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/part-transactions")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportPartTransactions(HttpServletRequest req,
                                                                   @Parameter(description = "Unique identifier " +
                                                                           "for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
            asyncExportService.exportPartTransactions(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportMeters(HttpServletRequest req, @Parameter(description = "Unique " +
            "identifier for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.METERS)) {
            asyncExportService.exportMeters(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/preventive-maintenances")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportPreventiveMaintenances(HttpServletRequest req,
                                                                        @Parameter(description = "Unique identifier " +
                                                                                "for tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)) {
            asyncExportService.exportPreventiveMaintenances(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/costs-times")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportCostsAndTimes(HttpServletRequest req,
                                                               @Parameter(description = "Unique identifier for " +
                                                                       "tracking the export job") @RequestParam String uuid) {
        User user = userService.whoami(req);
        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            asyncExportService.exportCostsAndTimes(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}

