package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.model.Company;
import com.grash.model.File;
import com.grash.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private File file;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        file = new File();
        file.setId(1L);
        file.setCompany(company);
    }

    @Test
    void create() {
        when(fileRepository.save(any(File.class))).thenReturn(file);

        File result = fileService.create(file);

        assertNotNull(result);
        assertEquals(file.getId(), result.getId());
        verify(fileRepository).save(file);
    }

    @Test
    void update() {
        when(fileRepository.save(any(File.class))).thenReturn(file);

        File result = fileService.update(file);

        assertNotNull(result);
        assertEquals(file.getId(), result.getId());
        verify(fileRepository).save(file);
    }

    @Test
    void getAll() {
        when(fileRepository.findAll()).thenReturn(Collections.singletonList(file));

        assertEquals(1, fileService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(fileRepository).deleteById(1L);
        fileService.delete(1L);
        verify(fileRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        assertTrue(fileService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(fileRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(file));

        assertEquals(1, fileService.findByCompany(1L).size());
    }

    @Test
    void findBySearchCriteria() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setPageNum(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setSortField("id");
        searchCriteria.setDirection(Sort.Direction.ASC);
        searchCriteria.setFilterFields(Collections.emptyList());

        Page<File> result = fileService.findBySearchCriteria(searchCriteria);
    }
}
