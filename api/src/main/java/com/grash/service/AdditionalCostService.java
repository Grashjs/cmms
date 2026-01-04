package com.grash.service;

import com.grash.dto.AdditionalCostPatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.AdditionalCostMapper;
import com.grash.model.AdditionalCost;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.AdditionalCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdditionalCostService {

    private final AdditionalCostRepository additionalCostRepository;
    private final EntityManager em;


    private final AdditionalCostMapper additionalCostMapper;
    private final LicenseService licenseService;

    @Transactional
    public AdditionalCost create(AdditionalCost additionalCost) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.COST_TRACKING))
            throw new CustomException("You need a license to create a additional cost", HttpStatus.FORBIDDEN);
        AdditionalCost savedAdditionalCost = additionalCostRepository.saveAndFlush(additionalCost);
        em.refresh(savedAdditionalCost);
        return savedAdditionalCost;
    }

    @Transactional
    public AdditionalCost update(Long id, AdditionalCostPatchDTO additionalCost) {
        if (additionalCostRepository.existsById(id)) {
            AdditionalCost savedAdditionalCost = additionalCostRepository.findById(id).get();
            AdditionalCost updatedAdditionalCost =
                    additionalCostRepository.saveAndFlush(additionalCostMapper.updateAdditionalCost(savedAdditionalCost, additionalCost));
            em.refresh(updatedAdditionalCost);
            return updatedAdditionalCost;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<AdditionalCost> getAll() {
        return additionalCostRepository.findAll();
    }

    public void delete(Long id) {
        additionalCostRepository.deleteById(id);
    }

    public Optional<AdditionalCost> findById(Long id) {
        return additionalCostRepository.findById(id);
    }

    public Collection<AdditionalCost> findByWorkOrder(Long id) {
        return additionalCostRepository.findByWorkOrder_Id(id);
    }
}
