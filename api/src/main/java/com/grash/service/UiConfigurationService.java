package com.grash.service;

import com.grash.dto.UiConfigurationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.UiConfigurationMapper;
import com.grash.model.UiConfiguration;
import com.grash.repository.UiConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UiConfigurationService {
    private final UiConfigurationRepository uiConfigurationRepository;
    private final UiConfigurationMapper uiConfigurationMapper;

    public UiConfiguration create(UiConfiguration UiConfiguration) {
        return uiConfigurationRepository.save(UiConfiguration);
    }

    public UiConfiguration update(Long id, UiConfigurationPatchDTO uiConfiguration) {
        if (uiConfigurationRepository.existsById(id)) {
            UiConfiguration savedUiConfiguration = uiConfigurationRepository.findById(id).get();
            return uiConfigurationRepository.save(uiConfigurationMapper.updateUiConfiguration(savedUiConfiguration,
                    uiConfiguration));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<UiConfiguration> getAll() {
        return uiConfigurationRepository.findAll();
    }

    public void delete(Long id) {
        uiConfigurationRepository.deleteById(id);
    }

    public Optional<UiConfiguration> findById(Long id) {
        return uiConfigurationRepository.findById(id);
    }

    public Optional<UiConfiguration> findByCompanySettings(Long id) {
        return uiConfigurationRepository.findByCompanySettings_Id(id);
    }
}
