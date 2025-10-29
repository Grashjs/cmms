package com.grash.service;

import com.grash.dto.PartQuantityPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartQuantityMapper;
import com.grash.model.*;
import com.grash.repository.PartQuantityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartQuantityServiceTest {

    @Mock
    private PartQuantityRepository partQuantityRepository;

    @Mock
    private PartQuantityMapper partQuantityMapper;

    @InjectMocks
    private PartQuantityService partQuantityService;

    private PartQuantity partQuantity;
    private Company company;
    private WorkOrder workOrder;
    private Part part;
    private PurchaseOrder purchaseOrder;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        workOrder = new WorkOrder();
        workOrder.setId(1L);

        part = new Part();
        part.setId(1L);

        purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(1L);

        partQuantity = new PartQuantity();
        partQuantity.setId(1L);
        partQuantity.setCompany(company);
        partQuantity.setWorkOrder(workOrder);
        partQuantity.setPart(part);
        partQuantity.setPurchaseOrder(purchaseOrder);
    }

    @Test
    void create() {
        when(partQuantityRepository.save(any(PartQuantity.class))).thenReturn(partQuantity);

        PartQuantity result = partQuantityService.create(partQuantity);

        assertNotNull(result);
        assertEquals(partQuantity.getId(), result.getId());
        verify(partQuantityRepository).save(partQuantity);
    }

    @Test
    void update_whenExists() {
        PartQuantityPatchDTO patchDTO = new PartQuantityPatchDTO();
        when(partQuantityRepository.existsById(1L)).thenReturn(true);
        when(partQuantityRepository.findById(1L)).thenReturn(Optional.of(partQuantity));
        when(partQuantityRepository.save(any(PartQuantity.class))).thenReturn(partQuantity);
        when(partQuantityMapper.updatePartQuantity(any(PartQuantity.class), any(PartQuantityPatchDTO.class))).thenReturn(partQuantity);

        PartQuantity result = partQuantityService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(partQuantity.getId(), result.getId());
        verify(partQuantityRepository).save(partQuantity);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        PartQuantityPatchDTO patchDTO = new PartQuantityPatchDTO();
        when(partQuantityRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> partQuantityService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(partQuantityRepository.findAll()).thenReturn(Collections.singletonList(partQuantity));

        assertEquals(1, partQuantityService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(partQuantityRepository).deleteById(1L);
        partQuantityService.delete(1L);
        verify(partQuantityRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(partQuantityRepository.findById(1L)).thenReturn(Optional.of(partQuantity));

        assertTrue(partQuantityService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(partQuantityRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(partQuantity));

        assertEquals(1, partQuantityService.findByCompany(1L).size());
    }

    @Test
    void findByWorkOrder() {
        when(partQuantityRepository.findByWorkOrder_Id(1L)).thenReturn(Collections.singletonList(partQuantity));

        assertEquals(1, partQuantityService.findByWorkOrder(1L).size());
    }

    @Test
    void findByPart() {
        when(partQuantityRepository.findByPart_Id(1L)).thenReturn(Collections.singletonList(partQuantity));

        assertEquals(1, partQuantityService.findByPart(1L).size());
    }

    @Test
    void findByPurchaseOrder() {
        when(partQuantityRepository.findByPurchaseOrder_Id(1L)).thenReturn(Collections.singletonList(partQuantity));

        assertEquals(1, partQuantityService.findByPurchaseOrder(1L).size());
    }

    @Test
    void save() {
        partQuantityService.save(partQuantity);
        verify(partQuantityRepository).save(partQuantity);
    }
}
