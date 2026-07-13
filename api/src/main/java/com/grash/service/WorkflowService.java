package com.grash.service;

import com.grash.dto.WorkflowPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowMapper;
import com.grash.model.*;
import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.repository.WorkflowRepository;
import com.grash.utils.AuditComparator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final WorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final WorkOrderService workOrderService;
    private final RequestService requestService;
    private final AssetService assetService;
    private final PurchaseOrderService purchaseOrderService;

    public Workflow create(Workflow Workflow) {
        return workflowRepository.save(Workflow);
    }

    public Workflow update(Long id, WorkflowPatchDTO workflowsPatchDTO) {
        if (workflowRepository.existsById(id)) {
            Workflow savedWorkflow = workflowRepository.findById(id).get();
            return workflowRepository.save(workflowMapper.updateWorkflow(savedWorkflow, workflowsPatchDTO));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Workflow> getAll() {
        return workflowRepository.findAll();
    }

    public void delete(Long id) {
        workflowRepository.deleteById(id);
    }

    public Optional<Workflow> findById(Long id) {
        return workflowRepository.findById(id);
    }

    public Collection<Workflow> findByMainConditionAndCompany(WFMainCondition mainCondition, Long id) {
        return workflowRepository.findByMainConditionAndCompany_Id(mainCondition, id);
    }

    public Collection<Workflow> findByCompany(Long id) {
        return workflowRepository.findByCompany_Id(id);
    }

    public void runWorkOrder(Workflow workflow, WorkOrder workOrder) {
        if (workflow.getSecondaryConditions().stream().allMatch(workflowCondition -> workflowCondition.isMetForWorkOrder(workOrder))) {
            WorkflowAction action = workflow.getAction();
            switch (action.getWorkOrderAction()) {
                case ADD_CHECKLIST:
                    //TODO
                    return;
                case SEND_REMINDER_EMAIL:
                    //TODO
                    return;
                case ASSIGN_TEAM:
                    workOrder.setTeam(action.getTeam());
                    break;
                case ASSIGN_USER:
                    workOrder.setPrimaryUser(action.getUser());
                    break;
                case ASSIGN_ASSET:
                    workOrder.setAsset(action.getAsset());
                    break;
                case ASSIGN_CATEGORY:
                    workOrder.setCategory(action.getWorkOrderCategory());
                    break;
                case ASSIGN_LOCATION:
                    workOrder.setLocation(action.getLocation());
                    break;
                case ASSIGN_PRIORITY:
                    workOrder.setPriority(action.getPriority());
                    break;
                default:
                    break;
            }
            workOrderService.save(workOrder);
        }
    }

    public void runRequest(Workflow workflow, Request request) {
        if (workflow.getSecondaryConditions().stream().allMatch(workflowCondition -> workflowCondition.isMetForRequest(request))) {
            WorkflowAction action = workflow.getAction();
            switch (action.getRequestAction()) {
                case ADD_CHECKLIST:
                    //TODO
                    return;
                case SEND_REMINDER_EMAIL:
                    //TODO
                    return;
                case ASSIGN_TEAM:
                    request.setTeam(action.getTeam());
                    break;
                case ASSIGN_USER:
                    request.setPrimaryUser(action.getUser());
                    break;
                case ASSIGN_ASSET:
                    request.setAsset(action.getAsset());
                    break;
                case ASSIGN_CATEGORY:
                    request.setCategory(action.getWorkOrderCategory());
                    break;
                case ASSIGN_LOCATION:
                    request.setLocation(action.getLocation());
                    break;
                case ASSIGN_PRIORITY:
                    request.setPriority(action.getPriority());
                    break;
                default:
                    break;
            }
            requestService.save(request);
        }
    }

    public void runPurchaseOrder(Workflow workflow, PurchaseOrder purchaseOrder) {
        if (workflow.getSecondaryConditions().stream().allMatch(workflowCondition -> workflowCondition.isMetForPurchaseOrder(purchaseOrder))) {
            WorkflowAction action = workflow.getAction();
            switch (action.getPurchaseOrderAction()) {
                case APPROVE:
                    purchaseOrder.setStatus(ApprovalStatus.APPROVED);
                    break;
                case REJECT:
                    purchaseOrder.setStatus(ApprovalStatus.REJECTED);
                    break;
                case ASSIGN_VENDOR:
                    purchaseOrder.setVendor(action.getVendor());
                    break;
                case SEND_REMINDER_EMAIL:
//                    TODO
                    return;
                default:
                    break;
            }
            purchaseOrderService.save(purchaseOrder);
        }
    }

    public void runPart(Workflow workflow, Part part) {
        if (workflow.getSecondaryConditions().stream().allMatch(workflowCondition -> workflowCondition.isMetForPart(part))) {
            WorkflowAction action = workflow.getAction();
            switch (action.getPartAction()) {
                case CREATE_PURCHASE_ORDER:
                    //TODO
                    break;
                default:
                    break;
            }
        }
    }

    public void runTask(Workflow workflow, Task task) {
        if (workflow.getSecondaryConditions().stream().allMatch(workflowCondition -> workflowCondition.isMetForTask(task))) {
            WorkflowAction action = workflow.getAction();
            switch (action.getTaskAction()) {
                case CREATE_REQUEST:
                case CREATE_WORK_ORDER:
//                   TODO
                    break;
                case SET_ASSET_STATUS:
                    Asset asset = task.getWorkOrder().getAsset();
                    if (asset != null) {
                        asset.setStatus(action.getAssetStatus());
                        assetService.save(asset);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void disableWorkflows(Long companyId) {
        Collection<Workflow> workflows = findByCompany(companyId);
        if (workflows.size() > 0) {
            Workflow firstWorkflow = Collections.min(workflows, new AuditComparator());
            Collection<Workflow> workflowsToDisable = workflows.stream().filter(workflow -> !workflow.getId().equals(firstWorkflow.getId())).collect(Collectors.toList());
            workflowsToDisable.forEach(workflow -> workflow.setEnabled(false));
            workflowRepository.saveAll(workflowsToDisable);
        }
    }

    public void enableWorkflows(Long companyId) {
        Collection<Workflow> workflows = findByCompany(companyId);
        workflows.forEach(workflow -> workflow.setEnabled(true));
        workflowRepository.saveAll(workflows);
    }
}
