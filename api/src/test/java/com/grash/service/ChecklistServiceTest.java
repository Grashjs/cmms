package com.grash.service;

import com.grash.dto.ChecklistPatchDTO;
import com.grash.dto.ChecklistPostDTO;
import com.grash.dto.TaskBaseDTO;
import com.grash.exception.CustomException;
import com.grash.model.Checklist;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.TaskBase;
import com.grash.repository.CheckListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    @Mock
    private CheckListRepository checklistRepository;

    @Mock
    private TaskBaseService taskBaseService;

    @Mock
    private EntityManager em;

    @InjectMocks
    private ChecklistService checklistService;

    private Checklist checklist;
    private Company company;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        companySettings = new CompanySettings();
        companySettings.setId(1L);

        checklist = new Checklist();
        checklist.setId(1L);
        checklist.setName("Test Checklist");
        checklist.setCompanySettings(companySettings);
    }

    @Test
    void create() {
        when(checklistRepository.saveAndFlush(any(Checklist.class))).thenReturn(checklist);

        Checklist result = checklistService.create(checklist);

        assertNotNull(result);
        assertEquals(checklist.getId(), result.getId());
        verify(checklistRepository).saveAndFlush(checklist);
        verify(em).refresh(checklist);
    }

    @Test
    void createPost() {
        ChecklistPostDTO postDTO = new ChecklistPostDTO();
        postDTO.setName("Test Checklist");
        postDTO.setCompanySettings(companySettings);
        postDTO.setTaskBases(Collections.singletonList(new TaskBaseDTO()));

        when(taskBaseService.createFromTaskBaseDTO(any(TaskBaseDTO.class), any(Company.class))).thenReturn(new TaskBase());
        when(checklistRepository.saveAndFlush(any(Checklist.class))).thenReturn(checklist);

        Checklist result = checklistService.createPost(postDTO, company);

        assertNotNull(result);
        assertEquals(checklist.getId(), result.getId());
        verify(checklistRepository).saveAndFlush(any(Checklist.class));
        verify(em).refresh(any(Checklist.class));
    }

    @Test
    void update_whenExists() {
        ChecklistPatchDTO patchDTO = new ChecklistPatchDTO();
        patchDTO.setName("Updated Checklist");
        patchDTO.setTaskBases(Collections.singletonList(new TaskBaseDTO()));

        when(checklistRepository.existsById(1L)).thenReturn(true);
        when(checklistRepository.getById(1L)).thenReturn(checklist);
        when(taskBaseService.createFromTaskBaseDTO(any(TaskBaseDTO.class), any(Company.class))).thenReturn(new TaskBase());
        when(checklistRepository.saveAndFlush(any(Checklist.class))).thenReturn(checklist);

        Checklist result = checklistService.update(1L, patchDTO, company);

        assertNotNull(result);
        assertEquals(checklist.getId(), result.getId());
        verify(checklistRepository).saveAndFlush(checklist);
        verify(em).refresh(checklist);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        ChecklistPatchDTO patchDTO = new ChecklistPatchDTO();
        when(checklistRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> checklistService.update(1L, patchDTO, company));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(checklistRepository.findAll()).thenReturn(Collections.singletonList(checklist));

        assertEquals(1, checklistService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(checklistRepository).deleteById(1L);
        checklistService.delete(1L);
        verify(checklistRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(checklist));

        assertTrue(checklistService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(checklistRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(checklist));

        assertEquals(1, checklistService.findByCompanySettings(1L).size());
    }
}
