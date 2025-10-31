package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PurchaseOrderPatchDTO;
import com.grash.dto.PurchaseOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PurchaseOrderMapper;
import com.grash.model.Company;
import com.grash.model.PurchaseOrder;
import com.grash.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private EntityManager em;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrder purchaseOrder;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(1L);
        purchaseOrder.setCompany(company);
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {

        @Test
        @DisplayName("should create a purchase order")
        void create() {
            when(purchaseOrderRepository.saveAndFlush(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

            PurchaseOrder result = purchaseOrderService.create(purchaseOrder);

            assertNotNull(result);
            assertEquals(purchaseOrder.getId(), result.getId());
            verify(purchaseOrderRepository).saveAndFlush(purchaseOrder);
            verify(em).refresh(purchaseOrder);
        }

        @Test
        @DisplayName("should update a purchase order")
        void update() {
            PurchaseOrderPatchDTO patchDTO = new PurchaseOrderPatchDTO();
            when(purchaseOrderRepository.existsById(1L)).thenReturn(true);
            when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
            when(purchaseOrderMapper.updatePurchaseOrder(any(PurchaseOrder.class), any(PurchaseOrderPatchDTO.class))).thenReturn(purchaseOrder);
            when(purchaseOrderRepository.saveAndFlush(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

            PurchaseOrder result = purchaseOrderService.update(1L, patchDTO);

            assertNotNull(result);
            verify(purchaseOrderRepository).saveAndFlush(purchaseOrder);
            verify(em).refresh(purchaseOrder);
        }

        @Test
        @DisplayName("should throw exception when updating non-existent purchase order")
        void updateNotFound() {
            PurchaseOrderPatchDTO patchDTO = new PurchaseOrderPatchDTO();
            when(purchaseOrderRepository.existsById(1L)).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                purchaseOrderService.update(1L, patchDTO);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should save a purchase order")
        void save() {
            when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
            PurchaseOrder result = purchaseOrderService.save(purchaseOrder);
            assertNotNull(result);
            verify(purchaseOrderRepository).save(purchaseOrder);
        }

        @Test
        @DisplayName("should delete a purchase order")
        void deletePurchaseOrder() {
            doNothing().when(purchaseOrderRepository).deleteById(1L);
            purchaseOrderService.delete(1L);
            verify(purchaseOrderRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("should get all purchase orders")
        void getAll() {
            when(purchaseOrderRepository.findAll()).thenReturn(Collections.singletonList(purchaseOrder));
            Collection<PurchaseOrder> result = purchaseOrderService.getAll();
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(purchaseOrderRepository).findAll();
        }

        @Test
        @DisplayName("should find a purchase order by id")
        void findById() {
            when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
            Optional<PurchaseOrder> result = purchaseOrderService.findById(1L);
            assertTrue(result.isPresent());
            assertEquals(purchaseOrder.getId(), result.get().getId());
            verify(purchaseOrderRepository).findById(1L);
        }

        @Test
        @DisplayName("should find purchase orders by company")
        void findByCompany() {
            when(purchaseOrderRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(purchaseOrder));
            Collection<PurchaseOrder> result = purchaseOrderService.findByCompany(1L);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(purchaseOrderRepository).findByCompany_Id(1L);
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogicTests {

        @Test
        @DisplayName("isPurchaseOrderInCompany should return true for null optional PO")
        void isPurchaseOrderInCompany_nullOptional() {
            assertTrue(purchaseOrderService.isPurchaseOrderInCompany(null, 1L, true));
        }

        @Test
        @DisplayName("isPurchaseOrderInCompany should return true for matching company")
        void isPurchaseOrderInCompany_matchingCompany() {
            when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
            assertTrue(purchaseOrderService.isPurchaseOrderInCompany(purchaseOrder, 1L, false));
        }

        @Test
        @DisplayName("isPurchaseOrderInCompany should return false for non-matching company")
        void isPurchaseOrderInCompany_nonMatchingCompany() {
            when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
            assertFalse(purchaseOrderService.isPurchaseOrderInCompany(purchaseOrder, 2L, false));
        }

        @Test
        @DisplayName("isPurchaseOrderInCompany should return false for non-existent PO")
        void isPurchaseOrderInCompany_nonExistent() {
            when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.empty());
            assertFalse(purchaseOrderService.isPurchaseOrderInCompany(purchaseOrder, 1L, false));
        }

        @Test
        @DisplayName("should find by search criteria")
        void findBySearchCriteria() {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setFilterFields(Collections.singletonList(FilterField.builder().field("id").value("1").operation("eq").build()));
            searchCriteria.setPageNum(0);
            searchCriteria.setPageSize(10);
            searchCriteria.setSortField("id");

            Page<PurchaseOrder> page = new PageImpl<>(Collections.singletonList(purchaseOrder));
            PurchaseOrderShowDTO dto = new PurchaseOrderShowDTO();

            when(purchaseOrderRepository.findAll((Specification<PurchaseOrder>) any(), any(Pageable.class))).thenReturn(page);
            when(purchaseOrderMapper.toShowDto(any(PurchaseOrder.class))).thenReturn(dto);

            Page<PurchaseOrderShowDTO> result = purchaseOrderService.findBySearchCriteria(searchCriteria);

            assertEquals(1, result.getTotalElements());
            assertEquals(dto, result.getContent().get(0));
            verify(purchaseOrderRepository).findAll((Specification<PurchaseOrder>) any(), any(Pageable.class));
            verify(purchaseOrderMapper).toShowDto(purchaseOrder);
        }
    }
}
