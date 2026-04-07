package com.grash.service;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.requestPortal.RequestPortalCriteria;
import com.grash.dto.requestPortal.RequestPortalPatchDTO;
import com.grash.dto.requestPortal.RequestPortalPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestPortalMapper;
import com.grash.model.RequestPortal;
import com.grash.model.RequestPortal_;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.RequestPortalRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RequestPortalService {
    private final RequestPortalRepository requestPortalRepository;
    private final RequestPortalMapper requestPortalMapper;
    private final LicenseService licenseService;

    public RequestPortal create(@Valid RequestPortalPostDTO requestPortalReq, OwnUser user) {
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.REQUEST_PORTAL) || !user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) || !licenseService.hasEntitlement(LicenseEntitlement.REQUEST_PORTAL))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        RequestPortal requestPortal =
                requestPortalMapper.fromPostDto(requestPortalReq);
        requestPortal.setUuid(UUID.randomUUID().toString());
        requestPortal.getFields().forEach(field -> field.setRequestPortal(requestPortal));

        return requestPortalRepository.save(requestPortal);
    }


    public List<RequestPortal> getAll() {
        return requestPortalRepository.findAll();
    }

    public void delete(Long id) {
        requestPortalRepository.deleteById(id);
    }

    public Optional<RequestPortal> findById(Long id) {
        return requestPortalRepository.findById(id);
    }

    public RequestPortal update(Long id, RequestPortalPatchDTO requestPortalPatchDTO, OwnUser user) {
        RequestPortal savedRequestPortal =
                requestPortalRepository.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        RequestPortal newRequestPortal = requestPortalMapper.updateRequestPortal(savedRequestPortal,
                requestPortalPatchDTO);
        newRequestPortal.getFields().forEach(field -> field.setRequestPortal(savedRequestPortal));
        return requestPortalRepository.save(newRequestPortal);
    }

    public Page<RequestPortal> findByCriteria(RequestPortalCriteria criteria, Pageable pageable, OwnUser user) {
        Specification<RequestPortal> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get(RequestPortal_.company).get("id"), user.getCompany().getId()));

//            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
//                predicates.add(cb.like(cb.lower(root.get(RequestPortal_.recipientName)),
//                        "%" + criteria.getQuery().toLowerCase().trim() + "%"));
//            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return requestPortalRepository.findAll(specification, pageable);
    }

    public Optional<RequestPortal> findByUuidByUser(String uuid) {
        return requestPortalRepository.findByUuid(uuid).map(requestPortal -> {
            if (requestPortal.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.REQUEST_PORTAL) && licenseService.hasEntitlement(LicenseEntitlement.REQUEST_PORTAL))
                return requestPortal;
            else return null;
        });
    }
}
