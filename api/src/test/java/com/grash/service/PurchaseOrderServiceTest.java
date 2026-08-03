package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.WorkOrder;
import com.grash.repository.PurchaseOrderRepository;
import com.grash.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Guards the company scoping of the work-order link. The create endpoint binds the
 * PurchaseOrder entity straight from the request body, so without this check a caller
 * could attach a work order of another company and probe for valid ids.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    private static final long OWN_COMPANY = 1L;
    private static final long OTHER_COMPANY = 2L;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private WorkOrder ownWorkOrder;
    private WorkOrder foreignWorkOrder;

    private static WorkOrder workOrder(Long id, long companyId) {
        Company company = new Company();
        company.setId(companyId);
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setCompany(company);
        return workOrder;
    }

    @BeforeEach
    void setUp() {
        ownWorkOrder = workOrder(10L, OWN_COMPANY);
        foreignWorkOrder = workOrder(20L, OTHER_COMPANY);
    }

    @Nested
    class CheckWorkOrderInCompany {

        @Test
        @DisplayName("accepts a work order of the caller's own company")
        void ownWorkOrder_passes() {
            when(workOrderRepository.findById(10L)).thenReturn(Optional.of(ownWorkOrder));

            assertDoesNotThrow(() ->
                    purchaseOrderService.checkWorkOrderInCompany(ownWorkOrder, OWN_COMPANY));
        }

        @Test
        @DisplayName("rejects a work order of another company")
        void foreignWorkOrder_throws() {
            when(workOrderRepository.findById(20L)).thenReturn(Optional.of(foreignWorkOrder));

            CustomException ex = assertThrows(CustomException.class, () ->
                    purchaseOrderService.checkWorkOrderInCompany(foreignWorkOrder, OWN_COMPANY));

            // Deliberately the same 404 as a missing id — a distinct 403 would confirm that
            // the id exists and turn the check into an id oracle.
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        @DisplayName("rejects an id that does not exist")
        void unknownWorkOrder_throws() {
            WorkOrder reference = new WorkOrder();
            reference.setId(999L);
            when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () ->
                    purchaseOrderService.checkWorkOrderInCompany(reference, OWN_COMPANY));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        @DisplayName("no link at all is allowed — the field is optional")
        void nullWorkOrder_passes() {
            assertDoesNotThrow(() ->
                    purchaseOrderService.checkWorkOrderInCompany(null, OWN_COMPANY));
        }

        @Test
        @DisplayName("a reference without an id is treated as no link, not as an error")
        void workOrderWithoutId_passes() {
            assertDoesNotThrow(() ->
                    purchaseOrderService.checkWorkOrderInCompany(new WorkOrder(), OWN_COMPANY));
        }
    }
}
