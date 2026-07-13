package com.grash.service;

import com.grash.model.CompanySettings;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanySettingsService {
    private final CompanySettingsRepository companySettingsRepository;

    public CompanySettings create(CompanySettings CompanySettings) {
        return companySettingsRepository.save(CompanySettings);
    }

    public CompanySettings update(CompanySettings CompanySettings) {
        return companySettingsRepository.save(CompanySettings);
    }

    public Page<CompanySettings> getAll(Pageable paging) {
        return companySettingsRepository.findAll(paging);
    }

    public void delete(Long id) {
        companySettingsRepository.deleteById(id);
    }

    public Optional<CompanySettings> findById(Long id) {
        return companySettingsRepository.findById(id);
    }
}
