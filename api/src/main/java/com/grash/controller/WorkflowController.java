package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.WorkflowPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowActionMapper;
import com.grash.mapper.WorkflowConditionMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.UserService;
import com.grash.service.WorkflowActionService;
import com.grash.service.WorkflowConditionService;
import com.grash.service.WorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/workflows")
@Api(tags = "workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UserService userService;
    private final WorkflowConditionMapper workflowConditionMapper;
    private final WorkflowConditionService workflowConditionService;
    private final WorkflowActionMapper workflowActionMapper;
    private final WorkflowActionService workflowActionService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkflowCategory not found")})
    public Collection<Workflow> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            return workflowService.findByCompany(user.getCompany().getId());
        } else return workflowService.getAll();
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Workflow create(@ApiParam("Workflow") @Valid @RequestBody WorkflowPostDTO workflowReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            int workflowsCount = (int) workflowService.findByCompany(user.getCompany().getId()).stream().filter(Workflow::isEnabled).count();
            if (user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.WORKFLOW) || workflowsCount == 0) {
                return createWorkflow(workflowReq, user.getCompany());
            } else throw new CustomException("You can't create a new workflow", HttpStatus.NOT_ACCEPTABLE);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Workflow not found")})
    public Workflow getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Workflow> optionalWorkflow = workflowService.findById(id);
        if (optionalWorkflow.isPresent()) {
            Workflow savedWorkflow = optionalWorkflow.get();
            return savedWorkflow;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Workflow not found")})
    public Workflow patch(@ApiParam("Workflow") @Valid @RequestBody WorkflowPostDTO workflow, @ApiParam("id") @PathVariable("id") Long id,
                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Workflow> optionalWorkflow = workflowService.findById(id);

        if (optionalWorkflow.isPresent()) {
            Workflow savedWorkflow = optionalWorkflow.get();
            workflowService.delete(id);
            return createWorkflow(workflow, user.getCompany());
        } else throw new CustomException("Workflow not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Workflow not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Workflow> optionalWorkflow = workflowService.findById(id);
        if (optionalWorkflow.isPresent()) {
            Workflow savedWorkflow = optionalWorkflow.get();
            workflowService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Workflow not found", HttpStatus.NOT_FOUND);
    }

    private Workflow createWorkflow(WorkflowPostDTO workflowReq, Company company) {
        List<WorkflowCondition> workflowConditions = workflowReq.getSecondaryConditions().stream().map(workflowConditionMapper::toModel)
                .collect(Collectors.toList());
        Collection<WorkflowCondition> savedWorkOrderConditions = workflowConditionService.saveAll(workflowConditions);
        WorkflowAction workflowAction = workflowActionMapper.toModel(workflowReq.getAction());
        WorkflowAction savedWorkflowAction = workflowActionService.create(workflowAction);
        Workflow workflow = Workflow.builder()
                .title(workflowReq.getTitle())
                .mainCondition(workflowReq.getMainCondition())
                .secondaryConditions(savedWorkOrderConditions)
                .action(savedWorkflowAction)
                .enabled(true)
                .build();
        return workflowService.create(workflow);
    }
}
