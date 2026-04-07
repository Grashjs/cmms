package com.grash.service;

import com.grash.dto.imports.*;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
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

    @Async
    public void importWorkOrders(OwnUser user, List<WorkOrderImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSend("/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importWorkOrders(toImport, user.getCompany());
            messagingTemplate.convertAndSend("/imports/" + uuid, response);
            log.info("Import completed for work-orders, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for work-orders, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importAssets(OwnUser user, List<AssetImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.ASSETS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSend("/imports/" + uuid, "error: Access Denied");
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

            messagingTemplate.convertAndSend("/imports/" + uuid, response);
            log.info("Import completed for assets, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for assets, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importLocations(OwnUser user, List<LocationImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.LOCATIONS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSend("/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importLocations(toImport, user.getCompany());
            messagingTemplate.convertAndSend("/imports/" + uuid, response);
            log.info("Import completed for locations, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for locations, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importMeters(OwnUser user, List<MeterImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.METERS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSend("/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importMeters(toImport, user.getCompany());
            messagingTemplate.convertAndSend("/imports/" + uuid, response);
            log.info("Import completed for meters, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for meters, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importParts(OwnUser user, List<PartImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSend("/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importParts(toImport, user.getCompany());
            messagingTemplate.convertAndSend("/imports/" + uuid, response);
            log.info("Import completed for parts, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for parts, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/imports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void importPreventiveMaintenances(OwnUser user, List<PreventiveMaintenanceImportDTO> toImport, String uuid) {
        try {
            if (!user.getRole().getCreatePermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)
                    || !user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.IMPORT_CSV)) {
                messagingTemplate.convertAndSend("/imports/" + uuid, "error: Access Denied");
                return;
            }

            ImportResponse response = importService.importPreventiveMaintenances(toImport, user.getCompany());
            messagingTemplate.convertAndSend("/imports/" + uuid, response);
            log.info("Import completed for preventive-maintenances, uuid: {}, created: {}, updated: {}", uuid, response.getCreated(), response.getUpdated());
        } catch (Exception e) {
            log.error("Import failed for preventive-maintenances, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/imports/" + uuid, "error: " + e.getMessage());
        }
    }
}
