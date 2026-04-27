package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestPostDTO;
import com.grash.dto.RequestShowDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.PortalFieldType;
import com.grash.model.enums.Priority;
import com.grash.model.enums.webhook.WebhookEvent;

import com.grash.repository.FieldConfigurationRepository;
import com.grash.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final RequestRepository requestRepository;
    private final CompanyService companyService;
    private final FileService fileService;
    private final LocationService locationService;
    private final UserService userService;
    private final TeamService teamService;
    private final AssetService assetService;
    private final WorkOrderService workOrderService;
    private final RequestMapper requestMapper;
    private final EntityManager em;
    private final CustomSequenceService customSequenceService;
    private final LicenseService licenseService;
    private final RequestPortalService requestPortalService;
    private final FieldConfigurationRepository fieldConfigurationRepository;
    private final WebhookDispatchService webhookDispatchService;
    private final CustomFieldValueService customFieldValueService;


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
            Request updatedRequest = requestRepository.saveAndFlush(requestMapper.updateRequest(savedRequest, request));
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

    public Page<RequestShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
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
                    Join<Request, WorkOrder> workOrderJoin = requestRoot.join("workOrder", JoinType.INNER);
                    predicates.add(workOrderJoin.get("priority").in(priorities));
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
        return requestRepository.findAll(builder.build(), page).map(requestMapper::toShowDto);
    }

    public boolean isRequestInCompany(Request request, long companyId, boolean optional) {
        if (optional) {
            Optional<Request> optionalRequest = request == null ? Optional.empty() : findById(request.getId());
            return request == null || (optionalRequest.isPresent() && optionalRequest.get().getCompany().getId().equals(companyId));
        } else {
            Optional<Request> optionalRequest = findById(request.getId());
            return optionalRequest.isPresent() && optionalRequest.get().getCompany().getId().equals(companyId);
        }
    }

    public Integer countPending(Long companyId) {
        return requestRepository.countPending(companyId);
    }

    public List<Request> findByCategoryAndCreatedAtBetween(Long id, Date start, Date end) {
        return requestRepository.findByCategory_IdAndCreatedAtBetween(id, start, end);
    }
}

