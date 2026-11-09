package com.grash.service;

import com.grash.dto.TaskPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.Task;
import com.grash.model.User;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.repository.TaskRepository;
import com.grash.utils.Sanitizer;
import com.grash.utils.TaskPatchValidation;
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
public class TaskService {
    private final TaskRepository taskRepository;
    private final WorkOrderService workOrderService;
    private final CompanyService companyService;
    private final FileService fileService;
    private final TaskMapper taskMapper;
    private final EntityManager em;
    private final WorkflowService workflowService;

    @Transactional
    public Task create(Task Task) {
        Sanitizer.sanitizeTask(Task);
        Task savedTask = taskRepository.saveAndFlush(Task);
        em.refresh(savedTask);
        return savedTask;
    }

    @Transactional
    public Task update(Long id, TaskPatchDTO patch, User user) {
        Task task = taskRepository.findForUpdate(id, user.getCompany().getId())
                .orElseThrow(() -> new CustomException("Task not found", HttpStatus.NOT_FOUND));
        return applyPatch(task, patch, user);
    }

    /** Shared by connected PATCH and the offline transaction; absent fields are never changed. */
    @Transactional
    public Task applyPatch(Task task, TaskPatchDTO patch, User user) {
        checkEditAccess(task, user, patch.hasValue());
        if (patch.getVersion() != null && patch.getVersion() != task.getVersion()) {
            throw new CustomException("Task changed. Refresh and review your changes before retrying.", HttpStatus.CONFLICT);
        }
        String validationError = TaskPatchValidation.error(task, patch);
        if (validationError != null) throw new CustomException(validationError, HttpStatus.BAD_REQUEST);
        taskMapper.updateTask(task, patch);
        if (patch.hasNotes()) task.setNotes(Sanitizer.cleanText(task.getNotes()));
        Task updated = taskRepository.saveAndFlush(task);
        workflowService.findByMainConditionAndCompany(WFMainCondition.TASK_UPDATED, user.getCompany().getId())
                .forEach(workflow -> workflowService.runTask(workflow, updated));
        em.flush();
        em.refresh(updated);
        return updated;
    }

    public void checkEditAccess(Task task, User user, boolean changesValue) {
        boolean sameCompany = task.getCompany().getId().equals(user.getCompany().getId());
        boolean editable = task.getWorkOrder() != null ? task.getWorkOrder().canBeEditedBy(user)
                : task.getPreventiveMaintenance() != null && task.getPreventiveMaintenance().canBeEditedBy(user);
        boolean assignedToOther = changesValue && task.getTaskBase().getUser() != null
                && !task.getTaskBase().getUser().getId().equals(user.getId());
        if (!sameCompany || !editable || assignedToOther) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    public Collection<Task> getAll() {
        return taskRepository.findAll();
    }

    public void delete(Long id) {
        taskRepository.deleteById(id);
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> findByWorkOrder(Long id) {
        return taskRepository.findByWorkOrder_IdOrderByCreatedAtAsc(id);
    }

    public List<Task> findByPreventiveMaintenance(Long id) {
        return taskRepository.findByPreventiveMaintenance_Id(id);
    }
}

