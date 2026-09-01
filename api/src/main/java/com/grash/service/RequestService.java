package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.RequestApproveDTO;
import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestPostDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.RequestMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.repository.RequestRepository;
import com.grash.utils.Helper;
import com.grash.utils.Sanitizer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final RequestRepository requestRepository;
    private final WorkOrderService workOrderService;
    private final RequestMapper requestMapper;
    private final EntityManager em;
    private final CustomSequenceService customSequenceService;
    private final LicenseService licenseService;
    private final WebhookDispatchService webhookDispatchService;
    private final CustomFieldValueService customFieldValueService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final MessageSource messageSource;
    private final MailServiceFactory mailServiceFactory;
    private final AssetService assetService;
    private final RequestPortalService requestPortalService;
    private WorkflowService workflowService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${security.recaptcha-secret-key:}")
    private String recaptchaSecretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public void setWorkflowService(@Lazy WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecaptchaResponse {
        private boolean success;
    }

    private void verifyRecaptcha(String token) {
        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify?secret=" +
                recaptchaSecretKey + "&response=" + token;

        ResponseEntity<RecaptchaResponse> response = restTemplate.postForEntity(verifyUrl, null,
                RecaptchaResponse.class);

        if (response.getBody() == null || !response.getBody().isSuccess()) {
            throw new CustomException("reCAPTCHA verification failed", HttpStatus.BAD_REQUEST);
        }
    }


    @Transactional
    public Request create(Request request, Company company) {
        if (request instanceof RequestPostDTO requestPostDTO) {
            request = requestMapper.fromPostDTO(requestPostDTO);
            if (!requestPostDTO.getCustomFields().isEmpty()) {
                setRequestCustomFields(request, requestPostDTO.getCustomFields(), company);
            }
        }
        if (request.getAudioDescription() != null && !licenseService.hasEntitlement(LicenseEntitlement.VOICE_NOTES))
            throw new CustomException("You need a license to add voice notes", HttpStatus.FORBIDDEN);
        Long nextSequence = customSequenceService.getNextRequestSequence(company);
        request.setCustomId("R" + String.format("%06d", nextSequence));
        Sanitizer.sanitizeRequest(request);

        Request savedRequest = requestRepository.saveAndFlush(request);
        em.refresh(savedRequest);
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("requestId", savedRequest.getId());
        Object serializedRequest = requestMapper.toShowDto(savedRequest);
        webhookDispatchService.dispatchWebhook(company, WebhookEvent.NEW_REQUEST, webhookPayload,
                "newRequest", serializedRequest, null, null, null, null, null);
        return savedRequest;
    }

    @Transactional
    public Request create(Request request, Company company, RequestPortal requestPortal) {
        if (request.getAudioDescription() != null && !licenseService.hasEntitlement(LicenseEntitlement.VOICE_NOTES))
            throw new CustomException("You need a license to add voice notes", HttpStatus.FORBIDDEN);
        Long nextSequence = customSequenceService.getNextRequestSequence(company);
        request.setCustomId("R" + String.format("%06d", nextSequence));
        request.setRequestPortal(requestPortal);
        request.setCompany(requestPortal.getCompany());
        Sanitizer.sanitizeRequest(request);
        RequestPortalField assetField =
                requestPortal.getFields().stream().filter(field -> field.getType().equals(PortalFieldType.ASSET) && field.getAsset() != null).findFirst().orElse(null);
        RequestPortalField locationField =
                requestPortal.getFields().stream().filter(field -> field.getType().equals(PortalFieldType.LOCATION) && field.getLocation() != null).findFirst().orElse(null);

        if (assetField != null) request.setAsset(assetField.getAsset());
        if (locationField != null) request.setLocation(locationField.getLocation());


        Request savedRequest = requestRepository.saveAndFlush(request);
        em.refresh(savedRequest);
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("requestId", savedRequest.getId());
        Object serializedRequest2 = requestMapper.toShowDto(savedRequest);
        webhookDispatchService.dispatchWebhook(company, WebhookEvent.NEW_REQUEST, webhookPayload,
                "newRequest", serializedRequest2, null, null, null, null, null);
        return savedRequest;
    }

    @Transactional
    public Request update(Long id, RequestPatchDTO request, Company company) {
        if (requestRepository.existsById(id)) {
            Request savedRequest = requestRepository.findById(id).get();
            if (!request.getCustomFields().isEmpty()) {
                setRequestCustomFields(savedRequest, request.getCustomFields(), company);
            }
            Request updatedRequest = requestMapper.updateRequest(savedRequest, request);
            Sanitizer.sanitizeRequest(updatedRequest);
            updatedRequest = requestRepository.saveAndFlush(updatedRequest);
            em.refresh(updatedRequest);
            return updatedRequest;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    private void setRequestCustomFields(Request request, List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
                                        Company company) {
        customFieldValueService.setCustomFields(
                request,
                request.getCustomFieldValues(),
                customFieldValuePostDTOS,
                company,
                CustomFieldEntityType.WORK_ORDER,
                cfv -> cfv.setRequest(request)
        );
    }

    public Collection<Request> getAll() {
        return requestRepository.findAll();
    }

    public void delete(Long id) {
        requestRepository.deleteById(id);
    }

    public Optional<Request> findById(Long id) {
        return requestRepository.findById(id);
    }

    public Collection<Request> findByCompany(Long id) {
        return requestRepository.findByCompany_Id(id);
    }

    public WorkOrder createWorkOrderFromRequest(Request request, User creator) {
        WorkOrderPostDTO workOrder = workOrderService.getWorkOrderFromWorkOrderBase(request);
        if (creator.getCompany().getCompanySettings().getGeneralPreferences().isAutoAssignRequests()) {
            User primaryUser = workOrder.getPrimaryUser();
            workOrder.setPrimaryUser(primaryUser == null ? creator : primaryUser);
        }
        workOrder.setParentRequest(request);
        WorkOrder savedWorkOrder = workOrderService.create(workOrder, creator.getCompany());
        request.setWorkOrder(savedWorkOrder);
        requestRepository.save(request);

        return savedWorkOrder;
    }

    public Request save(Request request) {
        return requestRepository.save(request);
    }

    public Collection<Request> findByCreatedAtBetweenAndCompany(Date date1, Date date2, Long id) {
        return requestRepository.findByCreatedAtBetweenAndCompany_Id(date1, date2, id);
    }

    public Page<Request> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Request> builder = new SpecificationBuilder<>();
        SearchCriteria searchCriteriaClone = searchCriteria.clone();

        builder.with((Specification<Request>) (requestRoot, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (searchCriteriaClone.getFilterFields().stream().anyMatch(filterField -> filterField.getField().equals(
                    "priority"))) {
                List<Priority> priorities = searchCriteriaClone.getFilterFields().stream()
                        .filter(filterField -> filterField.getField().equals("priority"))
                        .findFirst().get().getValues().stream().map(value -> Priority.getPriorityFromString(value.toString())).collect(Collectors.toList());
                if (!priorities.isEmpty()) {
                    Join<Request, WorkOrder> workOrderJoin = requestRoot.join(Request_.workOrder, JoinType.LEFT);
                    predicates.add(criteriaBuilder.or(workOrderJoin.get(WorkOrder_.priority).in(priorities),
                            requestRoot.get(
                                    Request_.priority).in(priorities)));
                }
            }

            if (searchCriteriaClone.getFilterFields().stream().anyMatch(filterField -> filterField.getField().equals(
                    "status"))) {
                List<Object> values = searchCriteriaClone.getFilterFields().stream()
                        .filter(filterField -> filterField.getField().equals("status"))
                        .findFirst().get().getValues();
                predicates.add(criteriaBuilder.or(values.stream().map(value -> {
                    if (value instanceof String) {
                        switch (value.toString()) {
                            case "CANCELLED":
                                return criteriaBuilder.equal(requestRoot.get("cancelled"), true);
                            case "APPROVED":
                                return criteriaBuilder.isNotNull(requestRoot.get("workOrder"));
                            case "PENDING":
                                return criteriaBuilder.and(criteriaBuilder.isNull(requestRoot.get("workOrder")),
                                        criteriaBuilder.equal(requestRoot.get("cancelled"), false));
                            default:
                                return null;
                        }
                    }
                    return null;
                }).toArray(Predicate[]::new)));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        searchCriteria.getFilterFields().
                removeIf(filterField -> filterField.getField().equals("status") || filterField.getField().equals(
                        "priority"));
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return requestRepository.findAll(builder.build(), page);
    }


    public Integer countPending(Long companyId) {
        return requestRepository.countPending(companyId);
    }

    public List<Request> findByCategoryAndCreatedAtBetween(Long id, Date start, Date end) {
        return requestRepository.findByCategory_IdAndCreatedAtBetween(id, start, end);
    }

    public SearchCriteria getSearchCriteria(User user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
                if (!user.getSuperAccountRelations().isEmpty()) {
                    List<Long> childCompanyIds = user.getSuperAccountRelations().stream()
                            .map(rel -> rel.getChildUser().getCompany().getId())
                            .distinct()
                            .toList();
                    searchCriteria.getFilterFields().add(FilterField.builder()
                            .field("company")
                            .operation("inm")
                            .joinType(JoinType.LEFT)
                            .value("")
                            .values(new ArrayList<>(childCompanyIds))
                            .build());
                } else {
                    searchCriteria.filterCompany(user);
                }
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.REQUESTS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return searchCriteria;
    }

    public Request getById(Long id, User user) {
        Optional<Request> optionalRequest = requestRepository.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.canBeViewedBy(user)) {
                return savedRequest;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public Request create(RequestPostDTO requestReq, User user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.REQUESTS)) {
            Request createdRequest = create(requestReq, user.getCompany());
            onRequestCreation(createdRequest, user.getCompany(), user.getFullName());
            return createdRequest;
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @Transactional
    public Request createFromPortal(Request requestReq, String requestPortalUuid, String recaptchaToken) {
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
        Request createdRequest = create(requestReq, requestPortal.getCompany(), requestPortal);
        onRequestCreation(createdRequest, requestPortal.getCompany(),
                requestReq.getContact() == null || requestReq.getContact().isBlank() ? messageSource.getMessage(
                        "someone", null
                        , Helper.getLocale(requestPortal.getCompany())) : requestReq.getContact());
        return createdRequest;
    }

    @Transactional
    public Request patch(Long id, RequestPatchDTO request, User user) {
        Optional<Request> optionalRequest = requestRepository.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.getWorkOrder() != null) {
                throw new CustomException("Can't patch an approved request", HttpStatus.NOT_ACCEPTABLE);
            }
            if (savedRequest.canBeEditedBy(user)) {
                return update(id, request, user.getCompany());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public WorkOrder approve(Long id, RequestApproveDTO requestApproveDTO, User user) {
        Optional<Request> optionalRequest = requestRepository.findById(id);
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

            WorkOrder createdWorkOrder = createWorkOrderFromRequest(savedRequest, user);
            if (savedRequest.getAsset() != null && requestApproveDTO.getAssetStatus() != null) {
                savedRequest.getAsset().setStatus(requestApproveDTO.getAssetStatus());
                assetService.save(savedRequest.getAsset());
            }

            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("requestId", savedRequest.getId());
            webhookPayload.put("requestTitle", savedRequest.getTitle());
            webhookPayload.put("previousStatus", "PENDING");
            webhookPayload.put("newStatus", "APPROVED");
            webhookPayload.put("workOrderId", createdWorkOrder.getId());
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
                        NotificationType.WORK_ORDER, createdWorkOrder.getId())), true, title);
                usersToMail.add(requester);
            }
            String message2 = messageSource.getMessage("request_approved_description_limited_admin",
                    new Object[]{user.getFullName(), savedRequest.getTitle()}, Helper.getLocale(user));
            notificationService.createMultiple(userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.getRole().getCode().equals(RoleCode.LIMITED_ADMIN) && !user1.getId().equals(user.getId())).map(user1 -> new Notification(message2, user1,
                    NotificationType.WORK_ORDER, createdWorkOrder.getId())).collect(Collectors.toList()), true, title);

            Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                put("workOrderLink", frontendUrl + "/app/work-orders/" + createdWorkOrder.getId());
                put("workOrderTitle", createdWorkOrder.getTitle());
            }};
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(usersToMail.stream().map(User::getEmail)
                            .toArray(String[]::new), title, mailVariables, "approved-request.html",
                    Helper.getLocale(user),
                    null);

            return createdWorkOrder;
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public Request cancel(Long id, String reason, User user) {
        Optional<Request> optionalRequest = requestRepository.findById(id);
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

            return save(savedRequest);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
    }

    public void deleteByIdAndUser(Long id, User user) {
        Optional<Request> optionalRequest = requestRepository.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.canBeDeletedBy(user)) {
                delete(id);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Request not found", HttpStatus.NOT_FOUND);
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
}

