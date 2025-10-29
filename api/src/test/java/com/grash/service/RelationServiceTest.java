package com.grash.service;

import com.grash.dto.RelationPatchDTO;
import com.grash.dto.RelationPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RelationMapper;
import com.grash.model.Company;
import com.grash.model.Relation;
import com.grash.model.WorkOrder;
import com.grash.model.enums.RelationType;
import com.grash.model.enums.RelationTypeInternal;
import com.grash.repository.RelationRepository;
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
class RelationServiceTest {

    @Mock
    private RelationRepository relationRepository;

    @Mock
    private RelationMapper relationMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private RelationService relationService;

    private Relation relation;
    private Company company;
    private WorkOrder parent;
    private WorkOrder child;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        parent = new WorkOrder();
        parent.setId(1L);

        child = new WorkOrder();
        child.setId(2L);

        relation = new Relation();
        relation.setId(1L);
        relation.setParent(parent);
        relation.setChild(child);
        relation.setCompany(company);
    }

    @Test
    void create() {
        when(relationRepository.saveAndFlush(any(Relation.class))).thenReturn(relation);

        Relation result = relationService.create(relation);

        assertNotNull(result);
        assertEquals(relation.getId(), result.getId());
        verify(relationRepository).saveAndFlush(relation);
        verify(em).refresh(relation);
    }

    @Test
    void update_whenExists() {
        RelationPatchDTO patchDTO = new RelationPatchDTO();
        when(relationRepository.existsById(1L)).thenReturn(true);
        when(relationRepository.findById(1L)).thenReturn(Optional.of(relation));
        when(relationRepository.saveAndFlush(any(Relation.class))).thenReturn(relation);
        when(relationMapper.updateRelation(any(Relation.class), any(RelationPatchDTO.class))).thenReturn(relation);

        Relation result = relationService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(relation.getId(), result.getId());
        verify(relationRepository).saveAndFlush(relation);
        verify(em).refresh(relation);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        RelationPatchDTO patchDTO = new RelationPatchDTO();
        when(relationRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> relationService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(relationRepository.findAll()).thenReturn(Collections.singletonList(relation));

        assertEquals(1, relationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(relationRepository).deleteById(1L);
        relationService.delete(1L);
        verify(relationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(relationRepository.findById(1L)).thenReturn(Optional.of(relation));

        assertTrue(relationService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(relationRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(relation));

        assertEquals(1, relationService.findByCompany(1L).size());
    }

    @Test
    void createPost() {
        RelationPostDTO postDTO = new RelationPostDTO();
        postDTO.setParent(parent);
        postDTO.setChild(child);
        postDTO.setRelationType(RelationType.RELATED_TO);

        when(relationRepository.saveAndFlush(any(Relation.class))).thenReturn(relation);

        Relation result = relationService.createPost(postDTO);

        assertNotNull(result);
        assertEquals(relation.getId(), result.getId());
        assertEquals(RelationTypeInternal.RELATED_TO, result.getRelationType());
    }

    @Test
    void findByWorkOrder() {
        when(relationRepository.findByParent_Id(1L)).thenReturn(Collections.singletonList(relation));
        when(relationRepository.findByChild_Id(1L)).thenReturn(Collections.emptyList());

        assertEquals(1, relationService.findByWorkOrder(1L).size());
    }

    @Test
    void findByParentAndChild() {
        when(relationRepository.findByParent_IdAndChild_Id(1L, 2L)).thenReturn(Collections.singletonList(relation));

        assertEquals(1, relationService.findByParentAndChild(1L, 2L).size());
    }
}
