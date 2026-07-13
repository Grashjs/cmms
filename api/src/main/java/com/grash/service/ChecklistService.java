package com.grash.service;

import com.grash.dto.ChecklistPatchDTO;
import com.grash.dto.ChecklistPostDTO;
import com.grash.exception.CustomException;
import com.grash.model.Checklist;
import com.grash.model.Company;
import com.grash.model.TaskBase;
import com.grash.repository.CheckListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistService {
    private final CheckListRepository checklistRepository;
    private final CompanySettingsService companySettingsService;
    private final TaskBaseService taskBaseService;
    private final EntityManager em;

    @Transactional
    public Checklist create(Checklist checklist) {
        Checklist savedChecklist = checklistRepository.saveAndFlush(checklist);
        em.refresh(savedChecklist);
        return savedChecklist;
    }

    @Transactional
    public Checklist createPost(ChecklistPostDTO checklistReq, Company company) {
        List<TaskBase> taskBases = checklistReq.getTaskBases().stream()
                .map(taskBaseDto -> taskBaseService.createFromTaskBaseDTO(taskBaseDto, company)).collect(Collectors.toList());
        Checklist checklist = Checklist.builder()
                .name(checklistReq.getName())
                .companySettings(checklistReq.getCompanySettings())
                .taskBases(taskBases)
                .category(checklistReq.getCategory())
                .description(checklistReq.getDescription())
                .build();
        Checklist savedChecklist = checklistRepository.saveAndFlush(checklist);
        em.refresh(savedChecklist);
        return savedChecklist;
    }

    @Transactional
    public Checklist update(Long id, ChecklistPatchDTO checklistReq, Company company) {
        if (checklistRepository.existsById(id)) {
            Checklist savedChecklist = checklistRepository.getById(id);
            savedChecklist.setCategory(checklistReq.getCategory());
            savedChecklist.setDescription(checklistReq.getDescription());
            savedChecklist.setName(checklistReq.getName());
            savedChecklist.getTaskBases().clear();
            List<TaskBase> taskBases = checklistReq.getTaskBases().stream()
                    .map(taskBaseDto -> taskBaseService.createFromTaskBaseDTO(taskBaseDto, company)).collect(Collectors.toList());
            savedChecklist.getTaskBases().addAll(taskBases);
            Checklist updatedChecklist = checklistRepository.saveAndFlush(savedChecklist);
            em.refresh(updatedChecklist);
            return updatedChecklist;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Checklist> getAll() {
        return checklistRepository.findAll();
    }

    public void delete(Long id) {
        checklistRepository.deleteById(id);
    }

    public Optional<Checklist> findById(Long id) {
        return checklistRepository.findById(id);
    }

    public Collection<Checklist> findByCompanySettings(Long id) {
        return checklistRepository.findByCompanySettings_Id(id);
    }

}
