
package com.grash.service;

import com.grash.dto.WorkOrderPatchDTO;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import javax.persistence.EntityManager;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private CustomerService customerService;

    @Mock
    private TeamService teamService;

    @Mock
    private AssetService assetService;

    @Mock
    private UserService userService;

    @Mock
    private CompanyService companyService;

    @Mock
    private LaborService laborService;

    @Mock
    private AdditionalCostService additionalCostService;

    @Mock
    private PartQuantityService partQuantityService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WorkOrderMapper workOrderMapper;

    @Mock
    private EntityManager em;

    @Mock
    private EmailService2 emailService2;

    @Mock
    private WorkOrderCategoryService workOrderCategoryService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private CustomSequenceService customSequenceService;

    @InjectMocks
    private WorkOrderService workOrderService;

    private WorkOrder workOrder;
    private Company company;
    private OwnUser user;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);

        workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setTitle("Test Work Order");
        workOrder.setCompany(company);

        workOrderService.setDeps(workflowService);
        workOrderService.setDeps(laborService, additionalCostService, partQuantityService);
    }

    @Test
    void testCreateWorkOrder() {
        when(customSequenceService.getNextWorkOrderSequence(any(Company.class))).thenReturn(1L);
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenReturn(workOrder);
        WorkOrder createdWorkOrder = workOrderService.create(workOrder, company);
        assertNotNull(createdWorkOrder);
        assertEquals("Test Work Order", createdWorkOrder.getTitle());
        verify(workOrderRepository, times(1)).saveAndFlush(workOrder);
        verify(em, times(1)).refresh(any(WorkOrder.class));
    }

    @Test
    void testUpdateWorkOrder() {
        WorkOrderPatchDTO patchDTO = new WorkOrderPatchDTO();
        patchDTO.setTitle("Updated Work Order");

        when(workOrderRepository.existsById(1L)).thenReturn(true);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        when(workOrderMapper.updateWorkOrder(any(WorkOrder.class), any(WorkOrderPatchDTO.class))).thenReturn(workOrder);
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenReturn(workOrder);

        WorkOrder updatedWorkOrder = workOrderService.update(1L, patchDTO, user);

        assertNotNull(updatedWorkOrder);
        verify(em, times(1)).refresh(any(WorkOrder.class));
    }

    @Test
    void testGetAllWorkOrders() {
        workOrderService.getAll();
        verify(workOrderRepository, times(1)).findAll();
    }

    @Test
    void testDeleteWorkOrder() {
        workOrderService.delete(1L);
        verify(workOrderRepository, times(1)).deleteById(1L);
    }

    @Test
    void testFindWorkOrderById() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        Optional<WorkOrder> foundWorkOrder = workOrderService.findById(1L);
        assertTrue(foundWorkOrder.isPresent());
        assertEquals("Test Work Order", foundWorkOrder.get().getTitle());
        verify(workOrderRepository, times(1)).findById(1L);
    }

    @Test
    void testFindByCompany() {
        workOrderService.findByCompany(1L);
        verify(workOrderRepository, times(1)).findByCompany_Id(1L);
    }

    @Test
    void testFindByAsset() {
        workOrderService.findByAsset(1L);
        verify(workOrderRepository, times(1)).findByAsset_Id(1L);
    }

    @Test
    void testFindByLocation() {
        workOrderService.findByLocation(1L);
        verify(workOrderRepository, times(1)).findByLocation_Id(1L);
    }

    @Test
    void testIsWorkOrderInCompany() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        assertTrue(workOrderService.isWorkOrderInCompany(workOrder, 1L, false));
    }
}
