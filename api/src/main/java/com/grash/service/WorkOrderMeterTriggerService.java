package com.grash.service;

import com.grash.dto.WorkOrderMeterTriggerPatchDTO;
import com.grash.dto.WorkOrderMeterTriggerPostDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMeterTriggerMapper;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.repository.CustomFieldRepository;
import com.grash.repository.WorkOrderMeterTriggerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkOrderMeterTriggerService {
    private final WorkOrderMeterTriggerRepository workOrderMeterTriggerRepository;
    private final WorkOrderService workOrderService;
    private final WorkOrderMeterTriggerMapper workOrderMeterTriggerMapper;
    private final MeterService meterService;
    private final EntityManager em;
    private final LicenseService licenseService;
    private final CustomFieldRepository customFieldRepository;

    @Transactional
    public WorkOrderMeterTrigger create(WorkOrderMeterTriggerPostDTO workOrderMeterTrigger, Company company) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.CONDITION_BASED_PM))
            throw new CustomException("You need a license to create a meter trigger", HttpStatus.FORBIDDEN);

        if (!workOrderMeterTrigger.getCustomFields().isEmpty()) {
            setMeterTriggerCustomFields(workOrderMeterTrigger, workOrderMeterTrigger.getCustomFields(), company);
        }

        WorkOrderMeterTrigger savedWorkOrderMeterTrigger =
                workOrderMeterTriggerRepository.saveAndFlush(workOrderMeterTrigger);
        em.refresh(savedWorkOrderMeterTrigger);
        return savedWorkOrderMeterTrigger;
    }

    @Transactional
    public WorkOrderMeterTrigger update(Long id, WorkOrderMeterTriggerPatchDTO workOrderMeterTrigger, Company company) {
        if (workOrderMeterTriggerRepository.existsById(id)) {
            WorkOrderMeterTrigger savedWorkOrderMeterTrigger = workOrderMeterTriggerRepository.findById(id).get();
            if (!workOrderMeterTrigger.getCustomFields().isEmpty()) {
                setMeterTriggerCustomFields(savedWorkOrderMeterTrigger, workOrderMeterTrigger.getCustomFields(),
                        company);
            }
            return workOrderMeterTriggerRepository.save(workOrderMeterTriggerMapper.updateWorkOrderMeterTrigger(savedWorkOrderMeterTrigger, workOrderMeterTrigger));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    private void setMeterTriggerCustomFields(WorkOrderMeterTrigger workOrderMeterTrigger,
                                             List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
                                             Company company) {
        List<CustomField> customFields =
                customFieldRepository.findByCompanySettingsAndEntityType(company.getCompanySettings(),
                        CustomFieldEntityType.WORK_ORDER);

        workOrderMeterTrigger.getCustomFieldValues().clear();

        for (CustomFieldValuePostDTO customFieldValuePostDTO : customFieldValuePostDTOS) {
            CustomFieldValue customFieldValue = new CustomFieldValue();
            customFieldValue.setWorkOrderMeterTrigger(workOrderMeterTrigger);
            customFieldValue.setValue(customFieldValuePostDTO.getValue());
            customFieldValue.setCustomField(customFields.stream().filter(customField -> customField.getId().equals(customFieldValuePostDTO.getId()))
                    .findFirst().orElseThrow(() -> new CustomException("Custom field not found",
                            HttpStatus.NOT_FOUND)));
            workOrderMeterTrigger.getCustomFieldValues().add(customFieldValue);
        }
    }

    public Collection<WorkOrderMeterTrigger> getAll() {
        return workOrderMeterTriggerRepository.findAll();
    }

    public void delete(Long id) {
        workOrderMeterTriggerRepository.deleteById(id);
    }

    public Optional<WorkOrderMeterTrigger> findById(Long id) {
        return workOrderMeterTriggerRepository.findById(id);
    }

    public Collection<WorkOrderMeterTrigger> findByMeter(Long id) {
        return workOrderMeterTriggerRepository.findByMeter_Id(id);
    }
}

