package com.grash.service;

import com.grash.dto.SuccessResponse;
import com.grash.dto.imports.*;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncImportService {

    private final ImportService importService;
    private final CompanyService companyService;
    private final IntercomService intercomService;
    private final SimpMessageSendingOperations messagingTemplate;

    private void checkImportPermission(User user, PermissionEntity permission) {
        if (!user.getRole().getCreatePermissions().contains(permission)
                || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    public SuccessResponse importWorkOrders(User user, List<WorkOrderImportDTO> toImport, String uuid) {
        checkImportPermission(user, PermissionEntity.WORK_ORDERS);
        importWorkOrdersAsync(user, toImport, uuid);
        return new SuccessResponse(true, uuid);
    }

    public SuccessResponse importAssets(User user, List<AssetImportDTO> toImport, String uuid) {
        checkImportPermission(user, PermissionEntity.ASSETS);
        importAssetsAsync(user, toImport, uuid);
        return new SuccessResponse(true, uuid);
    }

    public SuccessResponse importLocations(User user, List<LocationImportDTO> toImport, String uuid) {
        checkImportPermission(user, PermissionEntity.LOCATIONS);
        importLocationsAsync(user, toImport, uuid);
        return new SuccessResponse(true, uuid);
    }

    public SuccessResponse importMeters(User user, List<MeterImportDTO> toImport, String uuid) {
        checkImportPermission(user, PermissionEntity.METERS);
        importMetersAsync(user, toImport, uuid);
        return new SuccessResponse(true, uuid);
    }

    public SuccessResponse importParts(User user, List<PartImportDTO> toImport, String uuid) {
        checkImportPermission(user, PermissionEntity.PARTS_AND_MULTIPARTS);
        importPartsAsync(user, toImport, uuid);
        return new SuccessResponse(true, uuid);
    }

    public SuccessResponse importPreventiveMaintenances(User user, List<PreventiveMaintenanceImportDTO> toImport, String uuid) {
        checkImportPermission(user, PermissionEntity.PREVENTIVE_MAINTENANCES);
        importPreventiveMaintenancesAsync(user, toImport, uuid);
        return new SuccessResponse(true, uuid);
    }

    @Async
    public void importWorkOrdersAsync(User user, List<WorkOrderImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importWorkOrders(toImport, user.getCompany());
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, response);
            log.info("Import completed for work-orders, uuid: {}, created: {}, updated: {}", uuid,
                    response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for work-orders, uuid: {}", uuid, e);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importAssetsAsync(User user, List<AssetImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.ASSETS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importAssets(toImport, user.getCompany());

            // Fire Intercom event for first asset import
            if (!user.getCompany().isImportedAssets() && response.getCreated() > 0) {
                user.getCompany().setImportedAssets(true);
                companyService.update(user.getCompany());
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("imported_count", response.getCreated());
                intercomService.createCompanyActivationEvent(
                        "first-assets-imported",
                        user.getCompany().getId(),
                        user.getEmail(),
                        metadata
                );
            }

            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, response);
            log.info("Import completed for assets, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(),
                    response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for assets, uuid: {}", uuid, e);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importLocationsAsync(User user, List<LocationImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.LOCATIONS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importLocations(toImport, user.getCompany());
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, response);
            log.info("Import completed for locations, uuid: {}, created: {}, updated: {}", uuid,
                    response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for locations, uuid: {}", uuid, e);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importMetersAsync(User user, List<MeterImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.METERS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importMeters(toImport, user.getCompany());
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, response);
            log.info("Import completed for meters, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(),
                    response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for meters, uuid: {}", uuid, e);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importPartsAsync(User user, List<PartImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importParts(toImport, user.getCompany());
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, response);
            log.info("Import completed for parts, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(),
                    response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for parts, uuid: {}", uuid, e);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importPreventiveMaintenancesAsync(User user, List<PreventiveMaintenanceImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importPreventiveMaintenances(toImport, user.getCompany());
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, response);
            log.info("Import completed for preventive-maintenances, uuid: {}, created: {}, updated: {}", uuid,
                    response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for preventive-maintenances, uuid: {}", uuid, e);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/imports/" + uuid, "error: " + e.getMessage());
        }
    }
}
