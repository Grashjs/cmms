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
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/export")
@Api(tags = "export")
@RequiredArgsConstructor
@Transactional
public class ExportController {

    private final AssetService assetService;
    private final MeterService meterService;
    private final UserService userService;
    private final LocationService locationService;
    private final PartService partService;
    private final WorkOrderService workOrderService;
    private final CsvFileGenerator csvFileGenerator;
    private final StorageServiceFactory storageServiceFactory;

    @GetMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportWorkOrders(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS)) {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeWorkOrdersToCsv(workOrderService.findByCompany(user.getCompany().getId()),
                    outputStreamWriter, Helper.getLocale(user));
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Work Orders.csv");
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, storageServiceFactory.getStorageService().uploadAndSign(file,
                            user.getCompany().getId() + "/exports" +
                                    "/work-orders")));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportAssets(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS)) {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeAssetsToCsv(assetService.findByCompany(user.getCompany().getId()),
                    outputStreamWriter, Helper.getLocale(user));
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Assets.csv");
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, storageServiceFactory.getStorageService().uploadAndSign(file,
                            user.getCompany().getId() + "/exports" +
                                    "/assets")));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportLocations(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.LOCATIONS)) {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeLocationsToCsv(locationService.findByCompany(user.getCompany().getId()),
                    outputStreamWriter, Helper.getLocale(user));
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Locations.csv");
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, storageServiceFactory.getStorageService().uploadAndSign(file,
                            user.getCompany().getId() + "/exports" +
                                    "/locations")));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportParts(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writePartsToCsv(partService.findByCompany(user.getCompany().getId()), outputStreamWriter
                    , Helper.getLocale(user));
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Parts.csv");
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, storageServiceFactory.getStorageService().uploadAndSign(file,
                            user.getCompany().getId() + "/exports" +
                                    "/parts")));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> exportMeters(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        if (user.getRole().getViewOtherPermissions().contains(PermissionEntity.METERS)) {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeMetersToCsv(meterService.findByCompany(user.getCompany().getId()),
                    outputStreamWriter, Helper.getLocale(user));
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Meters.csv");
            return ResponseEntity.ok()
                    .body(new SuccessResponse(true, storageServiceFactory.getStorageService().uploadAndSign(file,
                            user.getCompany().getId() + "/exports" +
                                    "/meters")));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}
