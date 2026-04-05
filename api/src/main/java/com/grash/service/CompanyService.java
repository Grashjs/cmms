package com.grash.service;

import com.grash.dto.CompanyPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CompanyMapper;
import com.grash.model.Company;
import com.grash.model.CompanyFeatureOverride;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.CompanyFeatureOverrideRepository;
import com.grash.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final EntityManager em;
    private final CompanyFeatureOverrideRepository featureOverrideRepository;
//    private final RoleService roleService;

    public Company create(Company company) {
//        company.getCompanySettings().setRoleList(roleService.findDefaultRoles());
        return companyRepository.save(company);
    }

    public Company update(Company Company) {
        return companyRepository.save(Company);
    }

    public Collection<Company> getAll() {
        return companyRepository.findAll();
    }

    public void delete(Long id) {
        companyRepository.deleteById(id);
    }

    public Optional<Company> findById(Long id) {
        return companyRepository.findById(id);
    }

    @Transactional
    public Company update(Long id, CompanyPatchDTO company) {
        if (companyRepository.existsById(id)) {
            Company savedCompany = companyRepository.findById(id).get();
            Company updatedCompany = companyRepository.saveAndFlush(companyMapper.updateCompany(savedCompany, company));
            em.refresh(updatedCompany);
            return updatedCompany;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public boolean existsAtLeastOneWithMinWorkOrders() {//superAdmin and user's company
        return companyRepository.existsAtLeastOneWithMinWorkOrders();
    }

    /**
     * Returns effective features for a company: plan features + superadmin overrides.
     * Override enabled=true adds a feature; enabled=false removes it.
     */
    public Set<PlanFeatures> getEffectiveFeatures(Company company) {
        Set<PlanFeatures> features = new HashSet<>();
        if (company.getSubscription() != null && company.getSubscription().getSubscriptionPlan() != null) {
            features.addAll(company.getSubscription().getSubscriptionPlan().getFeatures());
        }
        List<CompanyFeatureOverride> overrides = featureOverrideRepository.findByCompanyId(company.getId());
        for (CompanyFeatureOverride override : overrides) {
            if (override.isEnabled()) {
                features.add(override.getFeature());
            } else {
                features.remove(override.getFeature());
            }
        }
        return features;
    }

    public List<CompanyFeatureOverride> getFeatureOverrides(Long companyId) {
        return featureOverrideRepository.findByCompanyId(companyId);
    }

}

