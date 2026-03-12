package com.grash.service;

import com.grash.dto.imports.*;
import com.grash.model.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final AssetService assetService;
    private final LocationService locationService;
    private final PartService partService;
    private final MeterService meterService;
    private final WorkOrderService workOrderService;
    private final PreventiveMaintenanceService preventiveMaintenanceService;

    @Transactional
    public ImportResponse importWorkOrders(List<WorkOrderImportDTO> toImport, Company company) {
        List<Long> idsToCheck = toImport.stream()
                .map(WorkOrderImportDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Map<Long, WorkOrder> existingWorkOrders = new java.util.HashMap<>();
        if (!idsToCheck.isEmpty()) {
            workOrderService.findByIdsAndCompany(idsToCheck, company.getId())
                    .forEach(wo -> existingWorkOrders.put(wo.getId(), wo));
        }

        List<WorkOrder> workOrdersToSave = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;

        for (WorkOrderImportDTO dto : toImport) {
            Long id = dto.getId();
            WorkOrder workOrder;

            if (id == null) {
                workOrder = new WorkOrder();
                created++;
            } else if (existingWorkOrders.containsKey(id)) {
                workOrder = existingWorkOrders.get(id);
                updated++;
            } else {
                workOrder = new WorkOrder();
                created++;
            }

            workOrderService.importWorkOrder(workOrder, dto, company);
            workOrdersToSave.add(workOrder);
        }

        if (!workOrdersToSave.isEmpty()) {
            workOrderService.saveAll(workOrdersToSave);
        }

        return ImportResponse.builder()
                .created(created)
                .updated(updated)
                .build();
    }

    @Transactional
    public ImportResponse importAssets(List<AssetImportDTO> toImport, Company company) {
        List<Long> idsToCheck = toImport.stream()
                .map(AssetImportDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Map<Long, Asset> existingAssets = new java.util.HashMap<>();
        if (!idsToCheck.isEmpty()) {
            assetService.findByIdsAndCompany(idsToCheck, company.getId())
                    .forEach(asset -> existingAssets.put(asset.getId(), asset));
        }

        Map<String, Asset> assetsByName = new java.util.HashMap<>();
        List<Asset> assetsToSave = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;

        for (AssetImportDTO dto : AssetService.orderAssets(toImport)) {
            Long id = dto.getId();
            Asset asset;

            if (id == null) {
                asset = new Asset();
                created++;
            } else if (existingAssets.containsKey(id)) {
                asset = existingAssets.get(id);
                updated++;
            } else {
                asset = new Asset();
                created++;
            }

            assetService.importAsset(asset, dto, company, assetsByName);
            assetsToSave.add(asset);
            assetsByName.put(asset.getName(), asset);
        }

        if (!assetsToSave.isEmpty()) {
            assetService.saveAll(assetsToSave);
        }

        return ImportResponse.builder()
                .created(created)
                .updated(updated)
                .build();
    }

    @Transactional
    public ImportResponse importLocations(List<LocationImportDTO> toImport, Company company) {
        List<Long> idsToCheck = toImport.stream()
                .map(LocationImportDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Map<Long, Location> existingLocations = new java.util.HashMap<>();
        if (!idsToCheck.isEmpty()) {
            locationService.findByIdsAndCompany(idsToCheck, company.getId())
                    .forEach(location -> existingLocations.put(location.getId(), location));
        }

        Map<String, Location> locationsByName = new java.util.HashMap<>();
        List<Location> locationsToSave = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;

        for (LocationImportDTO dto : LocationService.orderLocations(toImport)) {
            Long id = dto.getId();
            Location location;

            if (id == null) {
                location = new Location();
                created++;
            } else if (existingLocations.containsKey(id)) {
                location = existingLocations.get(id);
                updated++;
            } else {
                location = new Location();
                created++;
            }

            locationService.importLocation(location, dto, company, locationsByName);
            locationsToSave.add(location);
            locationsByName.put(location.getName(), location);
        }

        if (!locationsToSave.isEmpty()) {
            locationService.saveAll(locationsToSave);
        }

        return ImportResponse.builder()
                .created(created)
                .updated(updated)
                .build();
    }

    @Transactional
    public ImportResponse importMeters(List<MeterImportDTO> toImport, Company company) {
        List<Long> idsToCheck = toImport.stream()
                .map(MeterImportDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Map<Long, Meter> existingMeters = new java.util.HashMap<>();
        if (!idsToCheck.isEmpty()) {
            meterService.findByIdsAndCompany(idsToCheck, company.getId())
                    .forEach(meter -> existingMeters.put(meter.getId(), meter));
        }

        List<Meter> metersToSave = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;

        for (MeterImportDTO dto : toImport) {
            Long id = dto.getId();
            Meter meter;

            if (id == null) {
                meter = new Meter();
                created++;
            } else if (existingMeters.containsKey(id)) {
                meter = existingMeters.get(id);
                updated++;
            } else {
                meter = new Meter();
                created++;
            }

            meterService.importMeter(meter, dto, company);
            metersToSave.add(meter);
        }

        if (!metersToSave.isEmpty()) {
            meterService.saveAll(metersToSave);
        }

        return ImportResponse.builder()
                .created(created)
                .updated(updated)
                .build();
    }

    @Transactional
    public ImportResponse importParts(List<PartImportDTO> toImport, Company company) {
        List<Long> idsToCheck = toImport.stream()
                .map(PartImportDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Map<Long, Part> existingParts = new java.util.HashMap<>();
        if (!idsToCheck.isEmpty()) {
            partService.findByIdsAndCompany(idsToCheck, company.getId())
                    .forEach(part -> existingParts.put(part.getId(), part));
        }

        List<Part> partsToSave = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;

        for (PartImportDTO dto : toImport) {
            Long id = dto.getId();
            Part part;

            if (id == null) {
                part = new Part();
                created++;
            } else if (existingParts.containsKey(id)) {
                part = existingParts.get(id);
                updated++;
            } else {
                part = new Part();
                created++;
            }

            partService.importPart(part, dto, company);
            partsToSave.add(part);
        }

        if (!partsToSave.isEmpty()) {
            partService.saveAll(partsToSave);
        }

        return ImportResponse.builder()
                .created(created)
                .updated(updated)
                .build();
    }

    public ImportResponse importPreventiveMaintenances(List<PreventiveMaintenanceImportDTO> toImport, Company company) {
        final int[] created = {0};
        final int[] updated = {0};
        toImport.forEach(pmImportDTO -> {
            Long id = pmImportDTO.getId();
            PreventiveMaintenance preventiveMaintenance = new PreventiveMaintenance();
            if (id == null) {
                created[0]++;
            } else {
                Optional<PreventiveMaintenance> optionalPM = preventiveMaintenanceService.findByIdAndCompany(id,
                        company.getId());
                if (optionalPM.isPresent()) {
                    preventiveMaintenance = optionalPM.get();
                    updated[0]++;
                } else {
                    created[0]++;
                }
            }
            preventiveMaintenanceService.importPreventiveMaintenance(preventiveMaintenance, pmImportDTO, company);
        });
        return ImportResponse.builder()
                .created(created[0])
                .updated(updated[0])
                .build();
    }

}

