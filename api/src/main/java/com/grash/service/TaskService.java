package com.grash.service;

import com.grash.dto.TaskPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.Task;
import com.grash.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
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

    @Transactional
    public Task create(Task Task) {
        Task savedTask = taskRepository.saveAndFlush(Task);
        em.refresh(savedTask);
        return savedTask;
    }

    @Transactional
    public Task update(Long id, TaskPatchDTO task) {
        if (taskRepository.existsById(id)) {
            Task savedTask = taskRepository.findById(id).get();
            Task updatedTask = taskRepository.saveAndFlush(taskMapper.updateTask(savedTask, task));
            em.refresh(updatedTask);
            return updatedTask;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
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
