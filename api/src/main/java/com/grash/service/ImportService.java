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

    public ImportResponse importAssets(List<AssetImportDTO> toImport, Company company) {
        final int[] created = {0};
        final int[] updated = {0};
        AssetService.orderAssets(toImport).forEach(assetImportDTO -> {
            Long id = assetImportDTO.getId();
            Asset asset = new Asset();
            if (id == null) {
                created[0]++;
            } else {
                Optional<Asset> optionalAsset = assetService.findByIdAndCompany(id, company.getId());
                if (optionalAsset.isPresent()) {
                    asset = optionalAsset.get();
                    updated[0]++;
                } else {
                    created[0]++;
                }
            }
            assetService.importAsset(asset, assetImportDTO, company);
        });
        return ImportResponse.builder()
                .created(created[0])
                .updated(updated[0])
                .build();
    }

    public ImportResponse importLocations(List<LocationImportDTO> toImport, Company company) {
        final int[] created = {0};
        final int[] updated = {0};
        LocationService.orderLocations(toImport).forEach(locationImportDTO -> {
            Long id = locationImportDTO.getId();
            Location location = new Location();
            if (id == null) {
                created[0]++;
            } else {
                Optional<Location> optionalLocation = locationService.findByIdAndCompany(id, company.getId());
                if (optionalLocation.isPresent()) {
                    location = optionalLocation.get();
                    updated[0]++;
                } else {
                    created[0]++;
                }
            }
            locationService.importLocation(location, locationImportDTO, company);
        });
        return ImportResponse.builder()
                .created(created[0])
                .updated(updated[0])
                .build();
    }

    public ImportResponse importMeters(List<MeterImportDTO> toImport, Company company) {
        final int[] created = {0};
        final int[] updated = {0};
        toImport.forEach(meterImportDTO -> {
            Long id = meterImportDTO.getId();
            Meter meter = new Meter();
            if (id == null) {
                created[0]++;
            } else {
                Optional<Meter> optionalMeter = meterService.findByIdAndCompany(id, company.getId());
                if (optionalMeter.isPresent()) {
                    meter = optionalMeter.get();
                    updated[0]++;
                } else {
                    created[0]++;
                }
            }
            meterService.importMeter(meter, meterImportDTO, company);
        });
        return ImportResponse.builder()
                .created(created[0])
                .updated(updated[0])
                .build();
    }

    public ImportResponse importParts(List<PartImportDTO> toImport, Company company) {
        final int[] created = {0};
        final int[] updated = {0};
        toImport.forEach(partImportDTO -> {
            Long id = partImportDTO.getId();
            Part part = new Part();
            if (id == null) {
                created[0]++;
            } else {
                Optional<Part> optionalPart = partService.findByIdAndCompany(id, company.getId());
                if (optionalPart.isPresent()) {
                    part = optionalPart.get();
                    updated[0]++;
                } else {
                    created[0]++;
                }
            }
            partService.importPart(part, partImportDTO, company);
        });
        return ImportResponse.builder()
                .created(created[0])
                .updated(updated[0])
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

