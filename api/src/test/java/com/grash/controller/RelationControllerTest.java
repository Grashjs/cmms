package com.grash.controller;

import com.grash.dto.RelationPatchDTO;
import com.grash.dto.RelationPostDTO;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Relation;
import com.grash.model.WorkOrder;
import com.grash.service.RelationService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationControllerTest {

    @Mock
    private RelationService relationService;
    @Mock
    private UserService userService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RelationController relationController;

    private OwnUser user;
    private Relation relation;
    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        user = new OwnUser();
        user.setCompany(company);

        workOrder = new WorkOrder();
        workOrder.setId(1L);

        relation = new Relation();
        relation.setId(1L);
    }

    @Nested
    @DisplayName("Get All Relations Tests")
    class GetAllRelationsTests {

        @Test
        @DisplayName("Should return all relations for a company")
        void shouldReturnAllRelationsForCompany() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findByCompany(anyLong())).thenReturn(Collections.singletonList(relation));

            Collection<Relation> result = relationController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get By Work Order Tests")
    class GetByWorkOrderTests {

        @Test
        @DisplayName("Should throw not found when work order does not exist")
        void shouldThrowNotFoundWhenWorkOrderDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> relationController.getByWorkOrder(1L, request));
        }

        @Test
        @DisplayName("Should return relations for a work order")
        void shouldReturnRelationsForWorkOrder() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.of(workOrder));
            when(relationService.findByWorkOrder(anyLong())).thenReturn(Collections.singletonList(relation));

            Collection<Relation> result = relationController.getByWorkOrder(1L, request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when relation does not exist")
        void shouldThrowNotFoundWhenRelationDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> relationController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return relation by id")
        void shouldReturnRelationById() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findById(anyLong())).thenReturn(Optional.of(relation));

            Relation result = relationController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        private RelationPostDTO relationPostDTO;

        @BeforeEach
        void setup() {
            WorkOrder parent = new WorkOrder();
            parent.setId(1L);
            WorkOrder child = new WorkOrder();
            child.setId(2L);

            relationPostDTO = new RelationPostDTO();
            relationPostDTO.setParent(parent);
            relationPostDTO.setChild(child);
        }

        @Test
        @DisplayName("Should throw not acceptable when relation already exists")
        void shouldThrowNotAcceptableWhenRelationExists() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findByParentAndChild(anyLong(), anyLong())).thenReturn(Collections.singletonList(relation));

            assertThrows(CustomException.class, () -> relationController.create(relationPostDTO, request));
        }

        @Test
        @DisplayName("Should create relation successfully")
        void shouldCreateRelationSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findByParentAndChild(anyLong(), anyLong())).thenReturn(Collections.emptyList());
            when(relationService.createPost(any(RelationPostDTO.class))).thenReturn(relation);

            Relation result = relationController.create(relationPostDTO, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        private RelationPatchDTO relationPatchDTO;

        @BeforeEach
        void setup() {
            relationPatchDTO = new RelationPatchDTO();
        }

        @Test
        @DisplayName("Should return null when relation not found")
        void shouldReturnNullWhenRelationNotFound() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findById(anyLong())).thenReturn(Optional.empty());

            Relation result = relationController.patch(relationPatchDTO, 1L, request);

            assertNull(result);
        }

        @Test
        @DisplayName("Should patch relation successfully")
        void shouldPatchRelationSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findById(anyLong())).thenReturn(Optional.of(relation));
            when(relationService.update(anyLong(), any(RelationPatchDTO.class))).thenReturn(relation);

            Relation result = relationController.patch(relationPatchDTO, 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should throw not found on delete")
        void shouldThrowNotFoundOnDelete() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> relationController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete relation successfully")
        void shouldDeleteRelationSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(relationService.findById(anyLong())).thenReturn(Optional.of(relation));

            ResponseEntity response = relationController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(relationService).delete(1L);
        }
    }
}
