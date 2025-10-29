package com.grash.service;

import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.model.envers.WorkOrderAud;
import com.grash.model.WorkOrderHistory;
import com.grash.model.envers.RevInfo;
import com.grash.model.envers.WorkOrderAudId;
import com.grash.repository.WorkOrderAudRepository;
import com.grash.repository.WorkOrderHistoryRepository;
import com.grash.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderHistoryServiceTest {

    @Mock
    private WorkOrderHistoryRepository workOrderHistoryRepository;

    @Mock
    private WorkOrderAudRepository workOrderAudRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private WorkOrderHistoryService workOrderHistoryService;

    private WorkOrderHistory workOrderHistory;
    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        workOrder = new WorkOrder();
        workOrder.setId(1L);

        workOrderHistory = new WorkOrderHistory();
        workOrderHistory.setId(1L);
        workOrderHistory.setWorkOrder(workOrder);
    }

    @Test
    void create() {
        when(workOrderHistoryRepository.save(any(WorkOrderHistory.class))).thenReturn(workOrderHistory);

        WorkOrderHistory result = workOrderHistoryService.create(workOrderHistory);

        assertNotNull(result);
        assertEquals(workOrderHistory.getId(), result.getId());
        verify(workOrderHistoryRepository).save(workOrderHistory);
    }

    @Test
    void update() {
        when(workOrderHistoryRepository.save(any(WorkOrderHistory.class))).thenReturn(workOrderHistory);

        WorkOrderHistory result = workOrderHistoryService.update(workOrderHistory);

        assertNotNull(result);
        assertEquals(workOrderHistory.getId(), result.getId());
        verify(workOrderHistoryRepository).save(workOrderHistory);
    }

    @Test
    void getAll() {
        when(workOrderHistoryRepository.findAll()).thenReturn(Collections.singletonList(workOrderHistory));

        assertEquals(1, workOrderHistoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workOrderHistoryRepository).deleteById(1L);
        workOrderHistoryService.delete(1L);
        verify(workOrderHistoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workOrderHistoryRepository.findById(1L)).thenReturn(Optional.of(workOrderHistory));

        assertTrue(workOrderHistoryService.findById(1L).isPresent());
    }

    @Test
    void findByWorkOrder() {
        WorkOrderAudId workOrderAudId = new WorkOrderAudId();
        RevInfo revInfo = new RevInfo();
        revInfo.setUser(new OwnUser());
        workOrderAudId.setRev(revInfo);
        WorkOrderAud workOrderAud = new WorkOrderAud();
        workOrderAud.setWorkOrderAudId(workOrderAudId);

        when(workOrderAudRepository.findByIdAndRevtype(1L, 1)).thenReturn(Collections.singletonList(workOrderAud));
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));

        assertEquals(1, workOrderHistoryService.findByWorkOrder(1L).size());
    }
}
