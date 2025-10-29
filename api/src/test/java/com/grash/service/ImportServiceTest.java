package com.grash.service;

import com.grash.dto.imports.*;
import com.grash.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private AssetService assetService;

    @Mock
    private LocationService locationService;

    @Mock
    private PartService partService;

    @Mock
    private MeterService meterService;

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private ImportService importService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
    }

    @Test
    void importWorkOrders_whenNew() {
        WorkOrderImportDTO dto = new WorkOrderImportDTO();
        doNothing().when(workOrderService).importWorkOrder(any(WorkOrder.class), any(WorkOrderImportDTO.class), any(Company.class));

        ImportResponse response = importService.importWorkOrders(Collections.singletonList(dto), company);

        assertEquals(1, response.getCreated());
        assertEquals(0, response.getUpdated());
    }

    @Test
    void importWorkOrders_whenExisting() {
        WorkOrderImportDTO dto = new WorkOrderImportDTO();
        dto.setId(1L);
        when(workOrderService.findByIdAndCompany(1L, 1L)).thenReturn(Optional.of(new WorkOrder()));
        doNothing().when(workOrderService).importWorkOrder(any(WorkOrder.class), any(WorkOrderImportDTO.class), any(Company.class));

        ImportResponse response = importService.importWorkOrders(Collections.singletonList(dto), company);

        assertEquals(0, response.getCreated());
        assertEquals(1, response.getUpdated());
    }

    @Test
    void importAssets_whenNew() {
        AssetImportDTO dto = new AssetImportDTO();
        doNothing().when(assetService).importAsset(any(Asset.class), any(AssetImportDTO.class), any(Company.class));

        ImportResponse response = importService.importAssets(Collections.singletonList(dto), company);

        assertEquals(1, response.getCreated());
        assertEquals(0, response.getUpdated());
    }

    @Test
    void importAssets_whenExisting() {
        AssetImportDTO dto = new AssetImportDTO();
        dto.setId(1L);
        when(assetService.findByIdAndCompany(1L, 1L)).thenReturn(Optional.of(new Asset()));
        doNothing().when(assetService).importAsset(any(Asset.class), any(AssetImportDTO.class), any(Company.class));

        ImportResponse response = importService.importAssets(Collections.singletonList(dto), company);

        assertEquals(0, response.getCreated());
        assertEquals(1, response.getUpdated());
    }

    @Test
    void importLocations_whenNew() {
        LocationImportDTO dto = new LocationImportDTO();
        doNothing().when(locationService).importLocation(any(Location.class), any(LocationImportDTO.class), any(Company.class));

        ImportResponse response = importService.importLocations(Collections.singletonList(dto), company);

        assertEquals(1, response.getCreated());
        assertEquals(0, response.getUpdated());
    }

    @Test
    void importLocations_whenExisting() {
        LocationImportDTO dto = new LocationImportDTO();
        dto.setId(1L);
        when(locationService.findByIdAndCompany(1L, 1L)).thenReturn(Optional.of(new Location()));
        doNothing().when(locationService).importLocation(any(Location.class), any(LocationImportDTO.class), any(Company.class));

        ImportResponse response = importService.importLocations(Collections.singletonList(dto), company);

        assertEquals(0, response.getCreated());
        assertEquals(1, response.getUpdated());
    }

    @Test
    void importMeters_whenNew() {
        MeterImportDTO dto = new MeterImportDTO();
        doNothing().when(meterService).importMeter(any(Meter.class), any(MeterImportDTO.class), any(Company.class));

        ImportResponse response = importService.importMeters(Collections.singletonList(dto), company);

        assertEquals(1, response.getCreated());
        assertEquals(0, response.getUpdated());
    }

    @Test
    void importMeters_whenExisting() {
        MeterImportDTO dto = new MeterImportDTO();
        dto.setId(1L);
        when(meterService.findByIdAndCompany(1L, 1L)).thenReturn(Optional.of(new Meter()));
        doNothing().when(meterService).importMeter(any(Meter.class), any(MeterImportDTO.class), any(Company.class));

        ImportResponse response = importService.importMeters(Collections.singletonList(dto), company);

        assertEquals(0, response.getCreated());
        assertEquals(1, response.getUpdated());
    }

    @Test
    void importParts_whenNew() {
        PartImportDTO dto = new PartImportDTO();
        doNothing().when(partService).importPart(any(Part.class), any(PartImportDTO.class), any(Company.class));

        ImportResponse response = importService.importParts(Collections.singletonList(dto), company);

        assertEquals(1, response.getCreated());
        assertEquals(0, response.getUpdated());
    }

    @Test
    void importParts_whenExisting() {
        PartImportDTO dto = new PartImportDTO();
        dto.setId(1L);
        when(partService.findByIdAndCompany(1L, 1L)).thenReturn(Optional.of(new Part()));
        doNothing().when(partService).importPart(any(Part.class), any(PartImportDTO.class), any(Company.class));

        ImportResponse response = importService.importParts(Collections.singletonList(dto), company);

        assertEquals(0, response.getCreated());
        assertEquals(1, response.getUpdated());
    }
}
