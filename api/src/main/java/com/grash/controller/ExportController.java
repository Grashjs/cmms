package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.*;
import com.grash.utils.CsvFileGenerator;
import com.grash.utils.Helper;
import com.grash.utils.MultipartFileImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/export")
@Tag(name = "export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {
    private final UserService userService;
    private final AsyncExportService asyncExportService;

    @GetMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportWorkOrders(HttpServletRequest req, @RequestParam String uuid) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            asyncExportService.exportWorkOrders(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportAssets(HttpServletRequest req, @RequestParam String uuid) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS)) {
            asyncExportService.exportAssets(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportLocations(HttpServletRequest req, @RequestParam String uuid) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.LOCATIONS)) {
            asyncExportService.exportLocations(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportParts(HttpServletRequest req, @RequestParam String uuid) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
            asyncExportService.exportParts(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportMeters(HttpServletRequest req, @RequestParam String uuid) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.METERS)) {
            asyncExportService.exportMeters(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/preventive-maintenances")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportPreventiveMaintenances(HttpServletRequest req, @RequestParam String uuid) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)) {
            asyncExportService.exportPreventiveMaintenances(user, uuid);
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, uuid));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}

