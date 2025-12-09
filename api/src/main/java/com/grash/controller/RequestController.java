package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.*;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@Api(tags = "request")
@RequiredArgsConstructor
@Transactional
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;
    private final WorkOrderMapper workOrderMapper;
    private final RequestMapper requestMapper;
    private final NotificationService notificationService;
    private final MessageSource messageSource;
    private final WorkflowService workflowService;
    private final EmailService2 emailService2;
    private final AssetService assetService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<RequestShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                       HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.REQUESTS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(requestService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/pending")
    @PreAuthorize("permitAll()")
    public SuccessResponse getPending(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT) && user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
            return new SuccessResponse(true, requestService.countPending(user.getCompany().getId()).toString());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Request not found")})
    public RequestShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.REQUESTS) || savedRequest.getCreatedBy().equals(user.getId()))) {
                return requestMapper.toShowDto(savedRequest);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public RequestShowDTO create(@ApiParam("Request") @Valid @RequestBody Request requestReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.REQUESTS)) {
            Request createdRequest = requestService.create(requestReq, user.getCompany());
            String title = messageSource.getMessage("new_request", null, Helper.getLocale(user));
            String message = messageSource.getMessage("notification_new_request", null, Helper.getLocale(user));
            List<OwnUser> usersToNotify = userService.findByCompany(user.getCompany().getId()).stream()
                    .filter(user1 -> user1.isEnabled() && user1.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                            || user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN)).collect(Collectors.toList());
            notificationService.createMultiple(usersToNotify
                    .stream().map(user1 -> new Notification(message, user1, NotificationType.REQUEST,
                            createdRequest.getId())).collect(Collectors.toList()), true, title);
            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("requestLink", frontendUrl + "/app/requests/" + createdRequest.getId());
                put("featuresLink", frontendUrl + "/#key-features");
                put("requestTitle", createdRequest.getTitle());
                put("requester", user.getFullName());
            }};
            emailService2.sendMessageUsingThymeleafTemplate(usersToNotify.stream().map(OwnUser::getEmail)
                    .toArray(String[]::new), messageSource.getMessage("new_request", null,
                    Helper.getLocale(user)), mailVariables, "new-request.html", Helper.getLocale(user));

            Collection<Workflow> workflows =
                    workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_CREATED,
                            user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runRequest(workflow, createdRequest));
            return requestMapper.toShowDto(createdRequest);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public RequestShowDTO patch(@ApiParam("Request") @Valid @RequestBody RequestPatchDTO request,
                                @ApiParam("id") @PathVariable("id") Long id,
                                HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);

        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Can't patch an approved request", HttpStatus.NOT_ACCEPTABLE);
            }
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.REQUESTS) || savedRequest.getCreatedBy().equals(user.getId())) {
                Request patchedRequest = requestService.update(id, request);
                return requestMapper.toShowDto(patchedRequest);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public WorkOrderShowDTO approve(@ApiParam("id") @PathVariable("id") Long id,
                                    @RequestBody RequestApproveDTO requestApproveDTO,
                                    HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);
        if (!(user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) || user.getRole().getCode().equals(RoleCode.LIMITED_ADMIN))) {
            throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        }
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Request is already approved", HttpStatus.NOT_ACCEPTABLE);
            }
            Collection<Workflow> workflows =
                    workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_APPROVED,
                            user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runRequest(workflow, savedRequest));

            WorkOrderShowDTO result =
                    workOrderMapper.toShowDto(requestService.createWorkOrderFromRequest(savedRequest, user));
            if (savedRequest.getAsset() != null && requestApproveDTO.getAssetStatus() != null) {
                savedRequest.getAsset().setStatus(requestApproveDTO.getAssetStatus());
                assetService.save(savedRequest.getAsset());
            }
            OwnUser requester = userService.findById(savedRequest.getCreatedBy()).get();
            String title = messageSource.getMessage("request_approved", null, Helper.getLocale(user));
            String message = messageSource.getMessage("request_approved_description",
                    new Object[]{savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(Collections.singletonList(new Notification(message, requester,
                    NotificationType.WORK_ORDER, result.getId())), true, title);

            String message2 = messageSource.getMessage("request_approved_description_limited_admin",
                    new Object[]{user.getFullName(), savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN) && !user1.getId().equals(user.getId())).map(user1 -> new Notification(message2, user1,
                    NotificationType.WORK_ORDER, result.getId())).collect(Collectors.toList()), true, title);

            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("workOrderLink", frontendUrl + "/app/work-orders/" + result.getId());
                put("featuresLink", frontendUrl + "/#key-features");
                put("workOrderTitle", result.getTitle());
            }};
            List<OwnUser> usersToMail =
                    userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN))
                            .filter(user1 -> user1.isEnabled() && user1.getUserSettings().isEmailNotified()).collect(Collectors.toList());
            usersToMail.add(requester);
            emailService2.sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail)
                    .toArray(String[]::new), title, mailVariables, "approved-request.html", Helper.getLocale(user));

            return result;
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public RequestShowDTO cancel(@ApiParam("id") @PathVariable("id") Long id,
                                 @RequestParam String reason,
                                 HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);
        if (!(user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) || user.getRole().getCode().equals(RoleCode.LIMITED_ADMIN))) {
            throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        }
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Request is already approved", HttpStatus.NOT_ACCEPTABLE);
            }
            if (reason == null || reason.trim().isEmpty())
                throw new CustomException("Please give a reason", HttpStatus.NOT_ACCEPTABLE);
            savedRequest.setCancellationReason(reason);
            savedRequest.setCancelled(true);
            Collection<Workflow> workflows =
                    workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_REJECTED,
                            user.getCompany().getId());
            workflows.forEach(workflow -> workflowService.runRequest(workflow, savedRequest));

            OwnUser requester = userService.findById(savedRequest.getCreatedBy()).get();

            String title = messageSource.getMessage("request_rejected", null, Helper.getLocale(user));
            String message = messageSource.getMessage("request_rejected_description",
                    new Object[]{savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(Collections.singletonList(new Notification(message, requester,
                    NotificationType.INFO, null)), true, title);

            String message2 = messageSource.getMessage("request_rejected_description_limited_admin",
                    new Object[]{user.getFullName(), savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN) && !user1.getId().equals(user.getId())).map(user1 -> new Notification(message2, user1,
                    NotificationType.INFO, null)).collect(Collectors.toList()), true, title);

            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("requestLink", frontendUrl + "/app/requests/" + savedRequest.getId());
                put("featuresLink", frontendUrl + "/#key-features");
                put("requestTitle", savedRequest.getTitle());
            }};
            List<OwnUser> usersToMail =
                    userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN))
                            .filter(user1 -> user1.isEnabled() && user1.getUserSettings().isEmailNotified()).collect(Collectors.toList());
            usersToMail.add(requester);
            emailService2.sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail)
                    .toArray(String[]::new), title, mailVariables, "rejected-request.html", Helper.getLocale(user));

            return requestMapper.toShowDto(requestService.save(savedRequest));
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Request not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Request> optionalRequest = requestService.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (user.getId().equals(savedRequest.getId()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.REQUESTS)) {
                requestService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

}
