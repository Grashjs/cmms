package com.grash.controller;

import com.grash.dto.imports.*;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.*;
import com.grash.model.enums.ImportEntity;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.*;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.swing.text.html.parser.Entity;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/import")
@Api(tags = "import")
@RequiredArgsConstructor
@Transactional
public class ImportController {

    private final AssetService assetService;
    private final UserService userService;
    private final StorageServiceFactory storageServiceFactory;
    private final LocationService locationService;
    private final PartService partService;
    private final MeterService meterService;
    private final WorkOrderService workOrderService;

    @PostMapping("/work-orders")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importWorkOrders(@Valid @RequestBody List<WorkOrderImportDTO> toImport,
                                           HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        final int[] created = {0};
        final int[] updated = {0};
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            toImport.forEach(workOrderImportDTO -> {
                Long id = workOrderImportDTO.getId();
                WorkOrder workOrder = new WorkOrder();
                if (id == null) {
                    created[0] = created[0] + 1;
                } else {
                    Optional<WorkOrder> optionalWorkOrder = workOrderService.findByIdAndCompany(id,
                            user.getCompany().getId());
                    if (optionalWorkOrder.isPresent()) {
                        workOrder = optionalWorkOrder.get();
                        updated[0] = updated[0] + 1;
                    }
                }
                workOrderService.importWorkOrder(workOrder, workOrderImportDTO, user.getCompany());
            });
            return ImportResponse.builder()
                    .created(created[0])
                    .updated(updated[0])
                    .build();
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importAssets(@Valid @RequestBody List<AssetImportDTO> toImport, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        final int[] created = {0};
        final int[] updated = {0};
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            AssetService.orderAssets(toImport).forEach(assetImportDTO -> {
                Long id = assetImportDTO.getId();
                Asset asset = new Asset();
                if (id == null) {
                    created[0] = created[0] + 1;
                } else {
                    Optional<Asset> optionalAsset = assetService.findByIdAndCompany(id, user.getCompany().getId());
                    if (optionalAsset.isPresent()) {
                        asset = optionalAsset.get();
                        updated[0] = updated[0] + 1;
                    }
                }
                assetService.importAsset(asset, assetImportDTO, user.getCompany());
            });
            return ImportResponse.builder()
                    .created(created[0])
                    .updated(updated[0])
                    .build();
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importLocations(@Valid @RequestBody List<LocationImportDTO> toImport,
                                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        final int[] created = {0};
        final int[] updated = {0};
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            LocationService.orderLocations(toImport).forEach(locationImportDTO -> {
                Long id = locationImportDTO.getId();
                Location location = new Location();
                if (id == null) {
                    created[0] = created[0] + 1;
                } else {
                    Optional<Location> optionalLocation = locationService.findByIdAndCompany(id,
                            user.getCompany().getId());
                    if (optionalLocation.isPresent()) {
                        location = optionalLocation.get();
                        updated[0] = updated[0] + 1;
                    }
                }
                locationService.importLocation(location, locationImportDTO, user.getCompany());
            });
            return ImportResponse.builder()
                    .created(created[0])
                    .updated(updated[0])
                    .build();
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/meters")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importMeters(@Valid @RequestBody List<MeterImportDTO> toImport, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        final int[] created = {0};
        final int[] updated = {0};
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS) && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            toImport.forEach(meterImportDTO -> {
                Long id = meterImportDTO.getId();
                Meter meter = new Meter();
                if (id == null) {
                    created[0] = created[0] + 1;
                } else {
                    Optional<Meter> optionalMeter = meterService.findByIdAndCompany(id, user.getCompany().getId());
                    if (optionalMeter.isPresent()) {
                        meter = optionalMeter.get();
                        updated[0] = updated[0] + 1;
                    }
                }
                meterService.importMeter(meter, meterImportDTO, user.getCompany());
            });
            return ImportResponse.builder()
                    .created(created[0])
                    .updated(updated[0])
                    .build();
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/parts")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ImportResponse importParts(@Valid @RequestBody List<PartImportDTO> toImport, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        final int[] created = {0};
        final int[] updated = {0};
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            toImport.forEach(partImportDTO -> {
                Long id = partImportDTO.getId();
                Part part = new Part();
                if (id == null) {
                    created[0] = created[0] + 1;
                } else {
                    Optional<Part> optionalPart = partService.findByIdAndCompany(id, user.getCompany().getId());
                    if (optionalPart.isPresent()) {
                        part = optionalPart.get();
                        updated[0] = updated[0] + 1;
                    }
                }
                partService.importPart(part, partImportDTO, user.getCompany());
            });
            return ImportResponse.builder()
                    .created(created[0])
                    .updated(updated[0])
                    .build();
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
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
