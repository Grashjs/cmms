package com.grash.service;

import com.grash.model.Company;
import com.grash.model.Part;
import com.grash.model.PartConsumption;
import com.grash.model.WorkOrder;
import com.grash.repository.PartConsumptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartConsumptionServiceTest {

    @Mock
    private PartConsumptionRepository partConsumptionRepository;

    @InjectMocks
    private PartConsumptionService partConsumptionService;

    private PartConsumption partConsumption;
    private Company company;
    private WorkOrder workOrder;
    private Part part;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        workOrder = new WorkOrder();
        workOrder.setId(1L);

        part = new Part();
        part.setId(1L);

        partConsumption = new PartConsumption();
        partConsumption.setId(1L);
        partConsumption.setCompany(company);
        partConsumption.setWorkOrder(workOrder);
        partConsumption.setPart(part);
    }

    @Test
    void create() {
        when(partConsumptionRepository.save(any(PartConsumption.class))).thenReturn(partConsumption);

        PartConsumption result = partConsumptionService.create(partConsumption);

        assertNotNull(result);
        assertEquals(partConsumption.getId(), result.getId());
        verify(partConsumptionRepository).save(partConsumption);
    }

    @Test
    void getAll() {
        when(partConsumptionRepository.findAll()).thenReturn(Collections.singletonList(partConsumption));

        assertEquals(1, partConsumptionService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(partConsumptionRepository).deleteById(1L);
        partConsumptionService.delete(1L);
        verify(partConsumptionRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(partConsumptionRepository.findById(1L)).thenReturn(Optional.of(partConsumption));

        assertTrue(partConsumptionService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(partConsumptionRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(partConsumption));

        assertEquals(1, partConsumptionService.findByCompany(1L).size());
    }

    @Test
    void findByCreatedAtBetweenAndCompany() {
        Date start = new Date();
        Date end = new Date();
        when(partConsumptionRepository.findByCreatedAtBetweenAndCompany_Id(start, end, 1L))
                .thenReturn(Collections.singletonList(partConsumption));

        assertEquals(1, partConsumptionService.findByCreatedAtBetweenAndCompany(start, end, 1L).size());
    }

    @Test
    void findByWorkOrderAndPart() {
        when(partConsumptionRepository.findByWorkOrder_IdAndPart_Id(1L, 1L))
                .thenReturn(Collections.singletonList(partConsumption));

        assertEquals(1, partConsumptionService.findByWorkOrderAndPart(1L, 1L).size());
    }

    @Test
    void save() {
        partConsumptionService.save(partConsumption);
        verify(partConsumptionRepository).save(partConsumption);
    }

    @Test
    void findByCompanyAndCreatedAtBetween() {
        Date start = new Date();
        Date end = new Date();
        when(partConsumptionRepository.findByCompany_IdAndCreatedAtBetween(1L, start, end))
                .thenReturn(Collections.singletonList(partConsumption));

        assertEquals(1, partConsumptionService.findByCompanyAndCreatedAtBetween(1L, start, end).size());
    }

    @Test
    void findByWorkOrders() {
        when(partConsumptionRepository.findByWorkOrder_IdIn(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(partConsumption));

        List<PartConsumption> result = partConsumptionService.findByWorkOrders(Collections.singletonList(1L));

        assertEquals(1, result.size());
    }
}
