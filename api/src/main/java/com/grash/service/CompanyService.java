package com.grash.service;

import com.grash.dto.CompanyPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CompanyMapper;
import com.grash.model.Company;
import com.grash.repository.CompanyRepository;
import com.grash.utils.Sanitizer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final EntityManager em;
    private PaddleService paddleService;
    
    @Autowired
    public void setDeps(@Lazy PaddleService paddleService
    ) {
        this.paddleService = paddleService;
    }

    public Company create(Company company) {
//        company.getCompanySettings().setRoleList(roleService.findDefaultRoles());
        Sanitizer.sanitizeCompany(company);
        return companyRepository.save(company);
    }

    public Company update(Company Company) {
        return companyRepository.save(Company);
    }

    public Collection<Company> getAll() {
        return companyRepository.findAll();
    }

    public void delete(Long id) {
        Company company = companyRepository.findById(id).orElseThrow(() -> new CustomException("Company not found",
                HttpStatus.NOT_FOUND));
        if (company.getSubscription().getPaddleSubscriptionId() != null)
            paddleService.pauseSubscription(company.getSubscription().getPaddleSubscriptionId());
        companyRepository.deleteById(id);
    }

    public Optional<Company> findById(Long id) {
        return companyRepository.findById(id);
    }

    @Transactional
    public Company update(Long id, CompanyPatchDTO company) {
        if (companyRepository.existsById(id)) {
            Company savedCompany = companyRepository.findById(id).get();
            Company updatedCompany = companyMapper.updateCompany(savedCompany, company);
            Sanitizer.sanitizeCompany(updatedCompany);
            updatedCompany = companyRepository.saveAndFlush(updatedCompany);
            em.refresh(updatedCompany);
            return updatedCompany;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public boolean existsAtLeastOneWithMinWorkOrders() {//superAdmin and user's company
        return companyRepository.existsAtLeastOneWithMinWorkOrders();
    }

    public Optional<Company> findByOwnerEmailAndOwnsCompany(String email) {
        return companyRepository.findByOwnerEmailAndOwnsCompany(email);
    }
}
