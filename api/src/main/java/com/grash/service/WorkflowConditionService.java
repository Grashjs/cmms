package com.grash.service;

import com.grash.dto.WorkflowConditionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowConditionMapper;
import com.grash.model.WorkflowCondition;
import com.grash.repository.WorkflowConditionRepository;
import com.grash.utils.Sanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkflowConditionService {
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowConditionMapper workflowConditionMapper;

    public WorkflowCondition create(WorkflowCondition WorkflowCondition) {
        Sanitizer.sanitizeWorkflowCondition(WorkflowCondition);
        return workflowConditionRepository.save(WorkflowCondition);
    }

    public WorkflowCondition update(Long id, WorkflowConditionPatchDTO workflowConditionsPatchDTO) {
        if (workflowConditionRepository.existsById(id)) {
            WorkflowCondition savedWorkflowCondition = workflowConditionRepository.findById(id).get();
            WorkflowCondition updatedWorkflowCondition =
                    workflowConditionMapper.updateWorkflowCondition(savedWorkflowCondition,
                            workflowConditionsPatchDTO);
            Sanitizer.sanitizeWorkflowCondition(updatedWorkflowCondition);
            return workflowConditionRepository.save(updatedWorkflowCondition);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<WorkflowCondition> getAll() {
        return workflowConditionRepository.findAll();
    }

    public void delete(Long id) {
        workflowConditionRepository.deleteById(id);
    }

    public Optional<WorkflowCondition> findById(Long id) {
        return workflowConditionRepository.findById(id);
    }

    public Collection<WorkflowCondition> saveAll(Collection<WorkflowCondition> workflowConditions) {
        return workflowConditionRepository.saveAll(workflowConditions);
    }

}
