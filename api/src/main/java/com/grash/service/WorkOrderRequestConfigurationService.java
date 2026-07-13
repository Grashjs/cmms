package com.grash.service;

import com.grash.model.OwnUser;
import com.grash.model.WorkOrderRequestConfiguration;
import com.grash.model.enums.RoleType;
import com.grash.repository.WorkOrderRequestConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkOrderRequestConfigurationService {

    private final WorkOrderRequestConfigurationRepository workOrderRequestConfigurationRepository;

    public WorkOrderRequestConfiguration create(WorkOrderRequestConfiguration WorkOrderRequestConfiguration) {
        return workOrderRequestConfigurationRepository.save(WorkOrderRequestConfiguration);
    }

    public WorkOrderRequestConfiguration update(WorkOrderRequestConfiguration WorkOrderRequestConfiguration) {
        return workOrderRequestConfigurationRepository.save(WorkOrderRequestConfiguration);
    }

    public Collection<WorkOrderRequestConfiguration> getAll() {
        return workOrderRequestConfigurationRepository.findAll();
    }

    public void delete(Long id) {
        workOrderRequestConfigurationRepository.deleteById(id);
    }

    public Optional<WorkOrderRequestConfiguration> findById(Long id) {
        return workOrderRequestConfigurationRepository.findById(id);
    }
}
