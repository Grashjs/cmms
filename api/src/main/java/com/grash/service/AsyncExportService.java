package com.grash.service;

import com.grash.factory.StorageServiceFactory;
import com.grash.model.User;
import com.grash.utils.CsvFileGenerator;
import com.grash.utils.Helper;
import com.grash.utils.MultipartFileImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncExportService {

    private final WorkOrderService workOrderService;
    private final AssetService assetService;
    private final LocationService locationService;
    private final PartService partService;
    private final MeterService meterService;
    private final PreventiveMaintenanceService preventiveMaintenanceService;
    private final CsvFileGenerator csvFileGenerator;
    private final StorageServiceFactory storageServiceFactory;
    private final SimpMessageSendingOperations messagingTemplate;

    @Async
    public void exportWorkOrders(User user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeWorkOrdersToCsv(
                    workOrderService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Work Orders.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/work-orders");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for work-orders, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for work-orders, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void exportAssets(User user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeAssetsToCsv(
                    assetService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Assets.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/assets");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for assets, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for assets, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void exportLocations(User user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeLocationsToCsv(
                    locationService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Locations.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/locations");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for locations, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for locations, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void exportParts(User user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writePartsToCsv(
                    partService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Parts.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/parts");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for parts, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for parts, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void exportMeters(User user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeMetersToCsv(
                    meterService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Meters.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/meters");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for meters, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for meters, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }

    @Async
    public void exportPreventiveMaintenances(User user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writePreventiveMaintenancesToCsv(
                    preventiveMaintenanceService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Preventive Maintenances.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/preventive-maintenances");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for preventive-maintenances, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for preventive-maintenances, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }
}