package com.grash.service;

import com.grash.dto.WorkflowPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowMapper;
import com.grash.model.*;
import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.model.enums.workflow.WorkOrderAction;
import com.grash.model.enums.workflow.RequestAction;
import com.grash.model.enums.workflow.PurchaseOrderAction;
import com.grash.model.enums.workflow.PartAction;
import com.grash.model.enums.workflow.TaskAction;
import com.grash.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.grash.model.enums.Priority;
import com.grash.model.enums.AssetStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkflowMapper workflowMapper;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private RequestService requestService;
    @Mock
    private AssetService assetService;
    @Mock
    private PurchaseOrderService purchaseOrderService;
    @Mock
    private WorkflowCondition workflowCondition;

    @InjectMocks
    private WorkflowService workflowService;

    private Workflow workflow;
    private Company company;
    private WorkOrder workOrder;
    private Request request;
    private PurchaseOrder purchaseOrder;
    private Part part;
    private Task task;
    private Asset asset;
    private WorkflowAction workflowAction;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        workflow = new Workflow();
        workflow.setId(1L);
        workflow.setCompany(company);
        workflow.setSecondaryConditions(Collections.singletonList(workflowCondition));
        workflowAction = new WorkflowAction();
        workflow.setAction(workflowAction);

        workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setCompany(company);

        request = new Request();
        request.setId(1L);
        request.setCompany(company);

        purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(1L);
        purchaseOrder.setCompany(company);

        part = new Part();
        part.setId(1L);
        part.setCompany(company);

        asset = new Asset();
        asset.setId(1L);
        asset.setCompany(company);

        task = new Task();
        workflow.setCreatedAt(new Date());
    }

    @Nested
    class Create {
        @Test
        void create_shouldReturnSavedWorkflow() {
            when(workflowRepository.save(any(Workflow.class))).thenReturn(workflow);

            Workflow result = workflowService.create(workflow);

            assertNotNull(result);
            assertEquals(workflow.getId(), result.getId());
            verify(workflowRepository).save(workflow);
        }
    }

    @Nested
    class Update {
        @Test
        void update_whenWorkflowExists_shouldReturnUpdatedWorkflow() {
            WorkflowPatchDTO patchDTO = new WorkflowPatchDTO();
            when(workflowRepository.existsById(anyLong())).thenReturn(true);
            when(workflowRepository.findById(anyLong())).thenReturn(Optional.of(workflow));
            when(workflowMapper.updateWorkflow(any(Workflow.class), any(WorkflowPatchDTO.class))).thenReturn(workflow);
            when(workflowRepository.save(any(Workflow.class))).thenReturn(workflow);

            Workflow result = workflowService.update(1L, patchDTO);

            assertNotNull(result);
            assertEquals(workflow.getId(), result.getId());
            verify(workflowRepository).existsById(1L);
            verify(workflowRepository).findById(1L);
            verify(workflowMapper).updateWorkflow(workflow, patchDTO);
            verify(workflowRepository).save(workflow);
        }

        @Test
        void update_whenWorkflowDoesNotExist_shouldThrowCustomException() {
            WorkflowPatchDTO patchDTO = new WorkflowPatchDTO();
            when(workflowRepository.existsById(anyLong())).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> workflowService.update(1L, patchDTO));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
            verify(workflowRepository).existsById(1L);
            verify(workflowRepository, never()).findById(anyLong());
            verify(workflowMapper, never()).updateWorkflow(any(Workflow.class), any(WorkflowPatchDTO.class));
            verify(workflowRepository, never()).save(any(Workflow.class));
        }
    }

    @Nested
    class GetAll {
        @Test
        void getAll_shouldReturnAllWorkflows() {
            when(workflowRepository.findAll()).thenReturn(Collections.singletonList(workflow));

            Collection<Workflow> result = workflowService.getAll();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.contains(workflow));
            verify(workflowRepository).findAll();
        }

        @Test
        void getAll_shouldReturnEmptyList_whenNoWorkflowsExist() {
            when(workflowRepository.findAll()).thenReturn(Collections.emptyList());

            Collection<Workflow> result = workflowService.getAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(workflowRepository).findAll();
        }
    }

    @Nested
    class Delete {
        @Test
        void delete_shouldCallRepositoryDeleteById() {
            doNothing().when(workflowRepository).deleteById(anyLong());

            workflowService.delete(1L);

            verify(workflowRepository).deleteById(1L);
        }
    }

    @Nested
    class FindById {
        @Test
        void findById_whenWorkflowExists_shouldReturnOptionalOfWorkflow() {
            when(workflowRepository.findById(anyLong())).thenReturn(Optional.of(workflow));

            Optional<Workflow> result = workflowService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(workflow.getId(), result.get().getId());
            verify(workflowRepository).findById(1L);
        }

        @Test
        void findById_whenWorkflowDoesNotExist_shouldReturnEmptyOptional() {
            when(workflowRepository.findById(anyLong())).thenReturn(Optional.empty());

            Optional<Workflow> result = workflowService.findById(1L);

            assertFalse(result.isPresent());
            verify(workflowRepository).findById(1L);
        }
    }

    @Nested
    class FindByMainConditionAndCompany {
        @Test
        void findByMainConditionAndCompany_shouldReturnWorkflows() {
            when(workflowRepository.findByMainConditionAndCompany_Id(any(WFMainCondition.class), anyLong()))
                    .thenReturn(Collections.singletonList(workflow));

            Collection<Workflow> result = workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CREATED, 1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.contains(workflow));
            verify(workflowRepository).findByMainConditionAndCompany_Id(WFMainCondition.WORK_ORDER_CREATED, 1L);
        }

        @Test
        void findByMainConditionAndCompany_shouldReturnEmptyList_whenNoWorkflowsFound() {
            when(workflowRepository.findByMainConditionAndCompany_Id(any(WFMainCondition.class), anyLong()))
                    .thenReturn(Collections.emptyList());

            Collection<Workflow> result = workflowService.findByMainConditionAndCompany(WFMainCondition.WORK_ORDER_CREATED, 1L);

            assertNotNull(result);
            verify(workflowRepository).findByMainConditionAndCompany_Id(WFMainCondition.WORK_ORDER_CREATED, 1L);
        }
    }

    @Nested
    class FindByCompany {
        @Test
        void findByCompany_shouldReturnWorkflows() {
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(Collections.singletonList(workflow));

            Collection<Workflow> result = workflowService.findByCompany(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.contains(workflow));
            verify(workflowRepository).findByCompany_Id(1L);
        }

        @Test
        void findByCompany_shouldReturnEmptyList_whenNoWorkflowsFound() {
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(Collections.emptyList());

            Collection<Workflow> result = workflowService.findByCompany(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(workflowRepository).findByCompany_Id(1L);
        }
    }

    @Nested
    class RunWorkOrder {
        @BeforeEach
        void setup() {
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAssignTeam_shouldAssignTeamAndSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            Team team = new Team();
            team.setId(1L);
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_TEAM);
            workflowAction.setTeam(team);

            workflowService.runWorkOrder(workflow, workOrder);

            assertEquals(team, workOrder.getTeam());
            verify(workOrderService).save(workOrder);
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAssignUser_shouldAssignUserAndSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            OwnUser user = new OwnUser();
            user.setId(1L);
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_USER);
            workflowAction.setUser(user);

            workflowService.runWorkOrder(workflow, workOrder);

            assertEquals(user, workOrder.getPrimaryUser());
            verify(workOrderService).save(workOrder);
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAssignAsset_shouldAssignAssetAndSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            Asset asset = new Asset();
            asset.setId(1L);
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_ASSET);
            workflowAction.setAsset(asset);

            workflowService.runWorkOrder(workflow, workOrder);

            assertEquals(asset, workOrder.getAsset());
            verify(workOrderService).save(workOrder);
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAssignCategory_shouldAssignCategoryAndSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            WorkOrderCategory category = new WorkOrderCategory();
            category.setId(1L);
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_CATEGORY);
            workflowAction.setWorkOrderCategory(category);

            workflowService.runWorkOrder(workflow, workOrder);

            assertEquals(category, workOrder.getCategory());
            verify(workOrderService).save(workOrder);
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAssignLocation_shouldAssignLocationAndSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            Location location = new Location();
            location.setId(1L);
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_LOCATION);
            workflowAction.setLocation(location);

            workflowService.runWorkOrder(workflow, workOrder);

            assertEquals(location, workOrder.getLocation());
            verify(workOrderService).save(workOrder);
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAssignPriority_shouldAssignPriorityAndSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            com.grash.model.enums.Priority priority = com.grash.model.enums.Priority.HIGH;
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_PRIORITY);
            workflowAction.setPriority(priority);

            workflowService.runWorkOrder(workflow, workOrder);

            assertEquals(priority, workOrder.getPriority());
            verify(workOrderService).save(workOrder);
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsAddChecklist_shouldNotSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            workflowAction.setWorkOrderAction(WorkOrderAction.ADD_CHECKLIST);

            workflowService.runWorkOrder(workflow, workOrder);

            verify(workOrderService, never()).save(any(WorkOrder.class));
        }

        @Test
        void runWorkOrder_whenConditionsMet_andActionIsSendReminderEmail_shouldNotSaveWorkOrder() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(true);
            workflowAction.setWorkOrderAction(WorkOrderAction.SEND_REMINDER_EMAIL);

            workflowService.runWorkOrder(workflow, workOrder);

            verify(workOrderService, never()).save(any(WorkOrder.class));
        }

        @Test
        void runWorkOrder_whenConditionsNotMet_shouldNotPerformAction() {
            when(workflowCondition.isMetForWorkOrder(any(WorkOrder.class))).thenReturn(false);
            workflowAction.setWorkOrderAction(WorkOrderAction.ASSIGN_TEAM);

            workflowService.runWorkOrder(workflow, workOrder);

            verify(workflowCondition).isMetForWorkOrder(workOrder);
            verify(workOrderService, never()).save(any(WorkOrder.class));
        }
    }

    @Nested
    class RunRequest {
        @BeforeEach
        void setup() {
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAssignTeam_shouldAssignTeamAndSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            Team team = new Team();
            team.setId(1L);
            workflowAction.setRequestAction(RequestAction.ASSIGN_TEAM);
            workflowAction.setTeam(team);

            workflowService.runRequest(workflow, request);

            assertEquals(team, request.getTeam());
            verify(requestService).save(request);
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAssignUser_shouldAssignUserAndSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            OwnUser user = new OwnUser();
            user.setId(1L);
            workflowAction.setRequestAction(RequestAction.ASSIGN_USER);
            workflowAction.setUser(user);

            workflowService.runRequest(workflow, request);

            assertEquals(user, request.getPrimaryUser());
            verify(requestService).save(request);
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAssignAsset_shouldAssignAssetAndSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            Asset asset = new Asset();
            asset.setId(1L);
            workflowAction.setRequestAction(RequestAction.ASSIGN_ASSET);
            workflowAction.setAsset(asset);

            workflowService.runRequest(workflow, request);

            assertEquals(asset, request.getAsset());
            verify(requestService).save(request);
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAssignCategory_shouldAssignCategoryAndSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            WorkOrderCategory category = new WorkOrderCategory();
            category.setId(1L);
            workflowAction.setRequestAction(RequestAction.ASSIGN_CATEGORY);
            workflowAction.setWorkOrderCategory(category);

            workflowService.runRequest(workflow, request);

            assertEquals(category, request.getCategory());
            verify(requestService).save(request);
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAssignLocation_shouldAssignLocationAndSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            Location location = new Location();
            location.setId(1L);
            workflowAction.setRequestAction(RequestAction.ASSIGN_LOCATION);
            workflowAction.setLocation(location);

            workflowService.runRequest(workflow, request);

            assertEquals(location, request.getLocation());
            verify(requestService).save(request);
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAssignPriority_shouldAssignPriorityAndSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            com.grash.model.enums.Priority priority = com.grash.model.enums.Priority.HIGH;
            workflowAction.setRequestAction(RequestAction.ASSIGN_PRIORITY);
            workflowAction.setPriority(priority);

            workflowService.runRequest(workflow, request);

            assertEquals(priority, request.getPriority());
            verify(requestService).save(request);
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsAddChecklist_shouldNotSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            workflowAction.setRequestAction(RequestAction.ADD_CHECKLIST);

            workflowService.runRequest(workflow, request);

            verify(requestService, never()).save(any(Request.class));
        }

        @Test
        void runRequest_whenConditionsMet_andActionIsSendReminderEmail_shouldNotSaveRequest() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(true);
            workflowAction.setRequestAction(RequestAction.SEND_REMINDER_EMAIL);

            workflowService.runRequest(workflow, request);

            verify(requestService, never()).save(any(Request.class));
        }

        @Test
        void runRequest_whenConditionsNotMet_shouldNotPerformAction() {
            when(workflowCondition.isMetForRequest(any(Request.class))).thenReturn(false);
            workflowAction.setRequestAction(RequestAction.ASSIGN_TEAM);

            workflowService.runRequest(workflow, request);

            verify(workflowCondition).isMetForRequest(request);
            verify(requestService, never()).save(any(Request.class));
        }
    }

    @Nested
    class RunPurchaseOrder {
        @BeforeEach
        void setup() {
        }

        @Test
        void runPurchaseOrder_whenConditionsMet_andActionIsApprove_shouldSetStatusApprovedAndSavePurchaseOrder() {
            when(workflowCondition.isMetForPurchaseOrder(any(PurchaseOrder.class))).thenReturn(true);
            workflowAction.setPurchaseOrderAction(PurchaseOrderAction.APPROVE);

            workflowService.runPurchaseOrder(workflow, purchaseOrder);

            assertEquals(ApprovalStatus.APPROVED, purchaseOrder.getStatus());
            verify(purchaseOrderService).save(purchaseOrder);
        }

        @Test
        void runPurchaseOrder_whenConditionsMet_andActionIsReject_shouldSetStatusRejectedAndSavePurchaseOrder() {
            when(workflowCondition.isMetForPurchaseOrder(any(PurchaseOrder.class))).thenReturn(true);
            workflowAction.setPurchaseOrderAction(PurchaseOrderAction.REJECT);

            workflowService.runPurchaseOrder(workflow, purchaseOrder);

            assertEquals(ApprovalStatus.REJECTED, purchaseOrder.getStatus());
            verify(purchaseOrderService).save(purchaseOrder);
        }

        @Test
        void runPurchaseOrder_whenConditionsMet_andActionIsAssignVendor_shouldAssignVendorAndSavePurchaseOrder() {
            when(workflowCondition.isMetForPurchaseOrder(any(PurchaseOrder.class))).thenReturn(true);
            Vendor vendor = new Vendor();
            vendor.setId(1L);
            workflowAction.setPurchaseOrderAction(PurchaseOrderAction.ASSIGN_VENDOR);
            workflowAction.setVendor(vendor);

            workflowService.runPurchaseOrder(workflow, purchaseOrder);

            assertEquals(vendor, purchaseOrder.getVendor());
            verify(purchaseOrderService).save(purchaseOrder);
        }

        @Test
        void runPurchaseOrder_whenConditionsMet_andActionIsSendReminderEmail_shouldNotSavePurchaseOrder() {
            when(workflowCondition.isMetForPurchaseOrder(any(PurchaseOrder.class))).thenReturn(true);
            workflowAction.setPurchaseOrderAction(PurchaseOrderAction.SEND_REMINDER_EMAIL);

            workflowService.runPurchaseOrder(workflow, purchaseOrder);

            verify(purchaseOrderService, never()).save(any(PurchaseOrder.class));
        }

        @Test
        void runPurchaseOrder_whenConditionsNotMet_shouldNotPerformAction() {
            when(workflowCondition.isMetForPurchaseOrder(any(PurchaseOrder.class))).thenReturn(false);
            workflowAction.setPurchaseOrderAction(PurchaseOrderAction.APPROVE);

            workflowService.runPurchaseOrder(workflow, purchaseOrder);

            verify(workflowCondition).isMetForPurchaseOrder(purchaseOrder);
            verify(purchaseOrderService, never()).save(any(PurchaseOrder.class));
        }
    }

    @Nested
    class RunPart {
        @BeforeEach
        void setup() {
        }

        @Test
        void runPart_whenConditionsMet_andActionIsCreatePurchaseOrder_shouldNotPerformAction() {
            when(workflowCondition.isMetForPart(any(Part.class))).thenReturn(true);
            workflowAction.setPartAction(PartAction.CREATE_PURCHASE_ORDER);

            workflowService.runPart(workflow, part);

            // Currently, there's a TODO in the service, so no interaction with purchaseOrderService is expected.
            // If the TODO is implemented, this test will need to be updated.
            verifyNoInteractions(purchaseOrderService);
        }

        @Test
        void runPart_whenConditionsNotMet_shouldNotPerformAction() {
            when(workflowCondition.isMetForPart(any(Part.class))).thenReturn(false);
            workflowAction.setPartAction(PartAction.CREATE_PURCHASE_ORDER);

            workflowService.runPart(workflow, part);

            verify(workflowCondition).isMetForPart(part);
            verifyNoInteractions(purchaseOrderService);
        }
    }

    @Nested
    class RunTask {
        @BeforeEach
        void setup() {
        }

        @Test
        void runTask_whenConditionsMet_andActionIsSetAssetStatus_shouldSetAssetStatusAndSaveAsset() {
            when(workflowCondition.isMetForTask(any(Task.class))).thenReturn(true);
            com.grash.model.enums.AssetStatus assetStatus = com.grash.model.enums.AssetStatus.OPERATIONAL;
            workflowAction.setTaskAction(TaskAction.SET_ASSET_STATUS);
            workflowAction.setAssetStatus(assetStatus);
            task.setWorkOrder(workOrder);
            workOrder.setAsset(asset);

            workflowService.runTask(workflow, task);

            assertEquals(assetStatus, asset.getStatus());
            verify(assetService).save(asset);
        }

        @Test
        void runTask_whenConditionsMet_andActionIsSetAssetStatus_andAssetIsNull_shouldNotSaveAsset() {
            when(workflowCondition.isMetForTask(any(Task.class))).thenReturn(true);
            com.grash.model.enums.AssetStatus assetStatus = com.grash.model.enums.AssetStatus.OPERATIONAL;
            workflowAction.setTaskAction(TaskAction.SET_ASSET_STATUS);
            workflowAction.setAssetStatus(assetStatus);
            task.setWorkOrder(workOrder);
            workOrder.setAsset(null); // Asset is null

            workflowService.runTask(workflow, task);

            verify(assetService, never()).save(any(Asset.class));
        }

        @Test
        void runTask_whenConditionsMet_andActionIsCreateRequest_shouldNotPerformAction() {
            when(workflowCondition.isMetForTask(any(Task.class))).thenReturn(true);
            workflowAction.setTaskAction(TaskAction.CREATE_REQUEST);

            workflowService.runTask(workflow, task);

            verifyNoInteractions(requestService);
            verifyNoInteractions(workOrderService);
            verifyNoInteractions(assetService);
        }

        @Test
        void runTask_whenConditionsMet_andActionIsCreateWorkOrder_shouldNotPerformAction() {
            when(workflowCondition.isMetForTask(any(Task.class))).thenReturn(true);
            workflowAction.setTaskAction(TaskAction.CREATE_WORK_ORDER);

            workflowService.runTask(workflow, task);

            verifyNoInteractions(requestService);
            verifyNoInteractions(workOrderService);
            verifyNoInteractions(assetService);
        }

        @Test
        void runTask_whenConditionsNotMet_shouldNotPerformAction() {
            when(workflowCondition.isMetForTask(any(Task.class))).thenReturn(false);
            workflowAction.setTaskAction(TaskAction.SET_ASSET_STATUS);

            workflowService.runTask(workflow, task);

            verify(workflowCondition).isMetForTask(task);
            verify(assetService, never()).save(any(Asset.class));
        }
    }

    @Nested
    class DisableWorkflows {
        @Test
        void disableWorkflows_whenWorkflowsExist_shouldDisableAllButFirstAndSave() {
            Workflow workflow2 = new Workflow();
            workflow2.setId(2L);
            workflow2.setCompany(company);
            workflow2.setCreatedAt(Date.from(workflow.getCreatedAt().toInstant().plus(1, ChronoUnit.SECONDS))); // Make workflow2 newer

            List<Workflow> workflows = Arrays.asList(workflow, workflow2);
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(workflows);
            when(workflowRepository.saveAll(anyCollection())).thenReturn(Collections.emptyList());

            workflowService.disableWorkflows(1L);

            assertFalse(workflow2.isEnabled());
            assertTrue(workflow.isEnabled()); // The oldest one should remain enabled
            verify(workflowRepository).findByCompany_Id(1L);
            verify(workflowRepository).saveAll(argThat(list -> ((Collection<Workflow>) list).size() == 1 && !((List<Workflow>) list).get(0).isEnabled()));
        }

        @Test
        void disableWorkflows_whenOnlyOneWorkflowExists_shouldNotDisableAny() {
            List<Workflow> workflows = Collections.singletonList(workflow);
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(workflows);

            workflowService.disableWorkflows(1L);

            assertTrue(workflow.isEnabled());
            verify(workflowRepository).findByCompany_Id(1L);
            verify(workflowRepository).saveAll(Collections.emptyList());
        }

        @Test
        void disableWorkflows_whenNoWorkflowsExist_shouldDoNothing() {
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(Collections.emptyList());

            workflowService.disableWorkflows(1L);

            verify(workflowRepository).findByCompany_Id(1L);
            verify(workflowRepository, never()).saveAll(anyCollection());
        }
    }

    @Nested
    class EnableWorkflows {
        @Test
        void enableWorkflows_whenWorkflowsExist_shouldEnableAllAndSave() {
            Workflow workflow2 = new Workflow();
            workflow2.setId(2L);
            workflow2.setCompany(company);
            workflow2.setEnabled(false);

            List<Workflow> workflows = Arrays.asList(workflow, workflow2);
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(workflows);
            when(workflowRepository.saveAll(anyCollection())).thenReturn(Collections.emptyList());

            workflowService.enableWorkflows(1L);

            assertTrue(workflow.isEnabled());
            assertTrue(workflow2.isEnabled());
            verify(workflowRepository).findByCompany_Id(1L);
            verify(workflowRepository).saveAll(argThat(list -> ((Collection<Workflow>) list).size() == 2 && ((List<Workflow>) list).stream().allMatch(Workflow::isEnabled)));
        }

        @Test
        void enableWorkflows_whenNoWorkflowsExist_shouldDoNothing() {
            when(workflowRepository.findByCompany_Id(anyLong())).thenReturn(Collections.emptyList());

            workflowService.enableWorkflows(1L);

            verify(workflowRepository).findByCompany_Id(1L);
            verify(workflowRepository).saveAll(Collections.emptyList());
        }
    }
}
