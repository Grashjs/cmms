package com.grash.service;

import com.grash.model.Company;
import com.grash.model.CustomSequence;
import com.grash.repository.CustomSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class CustomSequenceService {
    private final CustomSequenceRepository customSequenceRepository;

    public CustomSequence findByCompanyId(Long companyId) {
        return customSequenceRepository.findByCompanyId(companyId)
                .orElse(null);
    }

    @Transactional
    public CustomSequence createCustomSequence(Company company) {
        CustomSequence customSequence = new CustomSequence(company);
        return customSequenceRepository.save(customSequence);
    }

    @Transactional
    public CustomSequence getOrCreateCustomSequence(Company company) {
        CustomSequence customSequence = findByCompanyId(company.getId());
        if (customSequence == null) {
            customSequence = createCustomSequence(company);
        }
        return customSequence;
    }

    @Transactional
    public Long getNextWorkOrderSequence(Company company) {
        CustomSequence customSequence = getOrCreateCustomSequence(company);
        Long nextSequence = customSequence.getAndIncrementWorkOrderSequence();
        customSequenceRepository.save(customSequence);
        return nextSequence;
    }


    @Transactional
    public Long getNextAssetSequence(Company company) {
        CustomSequence customSequence = getOrCreateCustomSequence(company);
        Long nextSequence = customSequence.getAndIncrementAssetSequence();
        customSequenceRepository.save(customSequence);
        return nextSequence;
    }

    @Transactional
    public Long getNextPreventiveMaintenanceSequence(Company company) {
        CustomSequence customSequence = getOrCreateCustomSequence(company);
        Long nextSequence = customSequence.getAndIncrementPreventiveMaintenanceSequence();
        customSequenceRepository.save(customSequence);
        return nextSequence;
    }

    @Transactional
    public Long getNextLocationSequence(Company company) {
        CustomSequence customSequence = getOrCreateCustomSequence(company);
        Long nextSequence = customSequence.getAndIncrementLocationSequence();
        customSequenceRepository.save(customSequence);
        return nextSequence;
    }

    @Transactional
    public Long getNextRequestSequence(Company company) {
        CustomSequence customSequence = getOrCreateCustomSequence(company);
        Long nextSequence = customSequence.getAndIncrementRequestSequence();
        customSequenceRepository.save(customSequence);
        return nextSequence;
    }

}
