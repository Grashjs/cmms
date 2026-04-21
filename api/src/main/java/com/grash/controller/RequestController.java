package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.factory.MailServiceFactory;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.*;
import com.grash.utils.Helper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@Tag(name = "Requests", description = "Operations on maintenance requests")
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
    private final MailServiceFactory mailServiceFactory;
    private final AssetService assetService;
    private final RequestPortalService requestPortalService;
    private final WebhookDispatchService webhookDispatchService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${security.recaptcha-secret-key:}")
    private String recaptchaSecretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private void verifyRecaptcha(String token) {
        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify?secret=" +
                recaptchaSecretKey + "&response=" + token;

        ResponseEntity<RecaptchaResponse> response = restTemplate.postForEntity(verifyUrl, null,
                RecaptchaResponse.class);

        if (response.getBody() == null || !response.getBody().isSuccess()) {
            throw new CustomException("reCAPTCHA verification failed", HttpStatus.BAD_REQUEST);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecaptchaResponse {
        private boolean success;
    }

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<RequestShowDTO>> search(@Parameter(description = "Search criteria for filtering " +
                                                               "requests") @RequestBody SearchCriteria searchCriteria,
                                                       HttpServletRequest req) {
        User user = userService.whoami(req);
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
        User user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT) && user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
            return new SuccessResponse(true, requestService.countPending(user.getCompany().getId()).toString());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public RequestShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.REQUESTS) || savedRequest.getCreatedBy().equals(user.getId()))) {
                return requestMapper.toShowDto(savedRequest);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    private void onRequestCreation(Request createdRequest, Company company, String requesterName) {
        String title = messageSource.getMessage("new_request", null, Helper.getLocale(company));
        String message = messageSource.getMessage("notification_new_request", null, Helper.getLocale(company));
        List<User> usersToNotify = userService.findByCompany(company.getId()).stream()
                .filter(user1 -> user1.isEnabled() && user1.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                        || user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN)).collect(Collectors.toList());
        notificationService.createMultiple(usersToNotify
                .stream().map(user1 -> new Notification(message, user1, NotificationType.REQUEST,
                        createdRequest.getId())).collect(Collectors.toList()), true, title);
        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
            put("requestLink", frontendUrl + "/app/requests/" + createdRequest.getId());
            put("requestTitle", createdRequest.getTitle());
            put("requester", requesterName);
        }};
        mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToNotify.stream().map(User::getEmail)
                .toArray(String[]::new), messageSource.getMessage("new_request", null,
                Helper.getLocale(company)), mailVariables, "new-request.html", Helper.getLocale(company), null);

        Collection<Workflow> workflows =
                workflowService.findByMainConditionAndCompany(WFMainCondition.REQUEST_CREATED,
                        company.getId());
        workflows.forEach(workflow -> workflowService.runRequest(workflow, createdRequest));

    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    RequestShowDTO create(@Parameter(description = "Request data to create") @Valid @RequestBody RequestPostDTO requestReq,
                          HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.REQUESTS)) {
            Request createdRequest = requestService.create(requestReq, user.getCompany());
            onRequestCreation(createdRequest, user.getCompany(), user.getFullName());
            return requestMapper.toShowDto(createdRequest);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/portal/{requestPortalUuid}")
    RequestShowDTO createFromPortal(@Parameter(description = "Request data to create from portal") @Valid @RequestBody Request requestReq,
                                    @PathVariable("requestPortalUuid") String requestPortalUuid,
                                    @RequestParam(value = "recaptchaToken", required = false) @Parameter(description
                                            = "RecaptchaToken value") String recaptchaToken,
                                    HttpServletRequest req) {
        if (recaptchaSecretKey != null && !recaptchaSecretKey.isBlank()) {
            if (recaptchaToken == null || recaptchaToken.isBlank())
                throw new CustomException("Recaptcha token missing", HttpStatus.NOT_ACCEPTABLE);
            verifyRecaptcha(recaptchaToken);
        }
        Optional<RequestPortal> optionalRequestPortal = requestPortalService.findByUuidByUser(requestPortalUuid);
        if (optionalRequestPortal.isEmpty()) {
            throw new CustomException("Request portal not found", HttpStatus.NOT_FOUND);
        }
        RequestPortal requestPortal = optionalRequestPortal.get();
        Request createdRequest = requestService.create(requestReq, requestPortal.getCompany(), requestPortal);
        onRequestCreation(createdRequest, requestPortal.getCompany(),
                requestReq.getContact() == null || requestReq.getContact().isBlank() ? messageSource.getMessage(
                        "someone", null
                        , Helper.getLocale(requestPortal.getCompany())) : requestReq.getContact());
        return requestMapper.toShowDto(createdRequest);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public RequestShowDTO patch(@Parameter(description = "Request fields to update") @Valid @RequestBody RequestPatchDTO request,
                                @PathVariable("id") Long id,
                                HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Request> optionalRequest = requestService.findById(id);

        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Can't patch an approved request", HttpStatus.NOT_ACCEPTABLE);
            }
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.REQUESTS) || savedRequest.getCreatedBy().equals(user.getId())) {
                Request patchedRequest = requestService.update(id, request, user.getCompany());
                return requestMapper.toShowDto(patchedRequest);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public WorkOrderShowDTO approve(@PathVariable("id") Long id,
                                    @Parameter(description = "Request approval data") @RequestBody RequestApproveDTO requestApproveDTO,
                                    HttpServletRequest req) {
        User user = userService.whoami(req);
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

            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("requestId", savedRequest.getId());
            webhookPayload.put("requestTitle", savedRequest.getTitle());
            webhookPayload.put("previousStatus", "PENDING");
            webhookPayload.put("newStatus", "APPROVED");
            webhookPayload.put("workOrderId", result.getId());
            Object serializedRequest = requestMapper.toShowDto(savedRequest);
            webhookDispatchService.dispatchWebhook(user.getCompany(), WebhookEvent.WORK_REQUEST_STATUS_CHANGE,
                    webhookPayload,
                    "changedRequest", serializedRequest, null, null, null, null, null);

            List<User> usersToMail =
                    userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN))
                            .filter(user1 -> user1.isEnabled() && user1.getUserSettings().isEmailNotified()).collect(Collectors.toList());
            String title = messageSource.getMessage("request_approved", null, Helper.getLocale(user));

            if (savedRequest.getCreatedBy() != null) {
                User requester = userService.findById(savedRequest.getCreatedBy()).get();
                String message = messageSource.getMessage("request_approved_description",
                        new Object[]{savedRequest.getTitle()}, Helper.getLocale(user));
                notificationService.createMultiple(Collections.singletonList(new Notification(message, requester,
                        NotificationType.WORK_ORDER, result.getId())), true, title);
                usersToMail.add(requester);
            }
            String message2 = messageSource.getMessage("request_approved_description_limited_admin",
                    new Object[]{user.getFullName(), savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN) && !user1.getId().equals(user.getId())).map(user1 -> new Notification(message2, user1,
                    NotificationType.WORK_ORDER, result.getId())).collect(Collectors.toList()), true, title);

            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("workOrderLink", frontendUrl + "/app/work-orders/" + result.getId());
                put("workOrderTitle", result.getTitle());
            }};
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(User::getEmail)
                            .toArray(String[]::new), title, mailVariables, "approved-request.html",
                    Helper.getLocale(user),
                    null);

            return result;
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public RequestShowDTO cancel(@PathVariable("id") Long id,
                                 @RequestParam @Parameter(description = "Reason of the request") String reason,
                                 HttpServletRequest req) {
        User user = userService.whoami(req);
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

            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("requestId", savedRequest.getId());
            webhookPayload.put("requestTitle", savedRequest.getTitle());
            webhookPayload.put("previousStatus", "PENDING");
            webhookPayload.put("newStatus", "CANCELLED");
            webhookPayload.put("cancellationReason", reason);
            Object serializedRequest = requestMapper.toShowDto(savedRequest);
            webhookDispatchService.dispatchWebhook(user.getCompany(), WebhookEvent.WORK_REQUEST_STATUS_CHANGE,
                    webhookPayload,
                    "changedRequest", serializedRequest, null, null, null, null, null);

            String title = messageSource.getMessage("request_rejected", null, Helper.getLocale(user));
            List<User> usersToMail =
                    userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN))
                            .filter(user1 -> user1.isEnabled() && user1.getUserSettings().isEmailNotified()).collect(Collectors.toList());

            if (savedRequest.getCreatedBy() != null) {
                User requester = userService.findById(savedRequest.getCreatedBy()).get();

                String message = messageSource.getMessage("request_rejected_description",
                        new Object[]{savedRequest.getTitle()}, Helper.getLocale(user));
                notificationService.createMultiple(Collections.singletonList(new Notification(message, requester,
                        NotificationType.INFO, null)), true, title);
                usersToMail.add(requester);
            }
            String message2 = messageSource.getMessage("request_rejected_description_limited_admin",
                    new Object[]{user.getFullName(), savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN) && !user1.getId().equals(user.getId())).map(user1 -> new Notification(message2, user1,
                    NotificationType.INFO, null)).collect(Collectors.toList()), true, title);

            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("requestLink", frontendUrl + "/app/requests/" + savedRequest.getId());
                put("requestTitle", savedRequest.getTitle());
            }};
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(User::getEmail)
                            .toArray(String[]::new), title, mailVariables, "rejected-request.html",
                    Helper.getLocale(user),
                    null);

            return requestMapper.toShowDto(requestService.save(savedRequest));
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

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


