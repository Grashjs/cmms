package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.TaskPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceOfflineTest {
    @Mock TaskRepository repository;
    @Mock WorkOrderService workOrders;
    @Mock CompanyService companies;
    @Mock FileService files;
    @Mock EntityManager em;
    @Mock WorkflowService workflows;
    private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);
    private TaskService service;
    private User user;
    private Task task;

    @BeforeEach void setUp() {
        service = new TaskService(repository, workOrders, companies, files, mapper, em, workflows);
        Company company = new Company(); company.setId(52L);
        Role role = new Role(); role.setRoleType(RoleType.ROLE_CLIENT);
        role.setEditOtherPermissions(Set.of(PermissionEntity.WORK_ORDERS));
        user = new User(); user.setId(34L); user.setCompany(company); user.setRole(role);
        WorkOrder workOrder = new WorkOrder(); workOrder.setId(10L); workOrder.setCompany(company);
        TaskBase base = new TaskBase(); base.setTaskType(TaskType.TEXT);
        task = new Task(base, workOrder, null, "A");
        task.setCompany(company); task.setId(7L); task.setVersion(3); task.setNotes("old notes");
    }

    private void successfulWrite() {
        when(repository.saveAndFlush(task)).thenAnswer(invocation -> { task.setVersion(task.getVersion() + 1); return task; });
        when(workflows.findByMainConditionAndCompany(any(), eq(52L))).thenReturn(List.of());
    }

    @Test void connectedNotesPatchPreservesServerValueAndVersionIsNotMapped() throws Exception {
        TaskPatchDTO patch = new ObjectMapper().readValue("{\"notes\":\"new notes\",\"version\":3}", TaskPatchDTO.class);
        assertFalse(patch.hasValue());
        successfulWrite();
        service.applyPatch(task, patch, user);
        assertEquals("A", task.getValue());
        assertEquals("new notes", task.getNotes());
        assertEquals(4, task.getVersion());
    }

    @Test void emptyTextIsAnExplicitValueAndKeepsNotes() throws Exception {
        TaskPatchDTO patch = new ObjectMapper().readValue("{\"value\":\"\",\"version\":3}", TaskPatchDTO.class);
        successfulWrite();
        service.applyPatch(task, patch, user);
        assertEquals("", task.getValue()); assertEquals("old notes", task.getNotes());
        verify(workflows).findByMainConditionAndCompany(any(), eq(52L));
    }

    @Test void explicitNullClearsNotesButAbsentNotesDoNot() throws Exception {
        var patch = new ObjectMapper().readValue("{\"notes\":null}", TaskPatchDTO.class);
        assertTrue(patch.hasNotes()); assertFalse(patch.hasValue());
        mapper.updateTask(task, patch);
        assertNull(task.getNotes()); assertEquals("A", task.getValue());
    }

    @Test void staleWebVersionIsRejectedBeforeAnyMutation() {
        var patch = new TaskPatchDTO(); patch.setValue("B"); patch.setVersion(2L);
        CustomException error = assertThrows(CustomException.class, () -> service.applyPatch(task, patch, user));
        assertEquals(409, error.getHttpStatus().value()); assertEquals("A", task.getValue());
        verifyNoInteractions(repository, workflows);
    }

    @Test void valueCannotBeWrittenByAnotherTaskAssigneeButNotesKeepExistingRights() {
        User other = new User(); other.setId(35L); task.getTaskBase().setUser(other);
        var value = new TaskPatchDTO(); value.setValue("B");
        assertEquals(403, assertThrows(CustomException.class, () -> service.applyPatch(task, value, user)).getHttpStatus().value());
        successfulWrite();
        var notes = new TaskPatchDTO(); notes.setNotes("allowed");
        service.applyPatch(task, notes, user);
        assertEquals("A", task.getValue()); assertEquals("allowed", task.getNotes());
    }

    @Test void legacyConnectedPatchWithoutVersionStillWorks() {
        successfulWrite();
        var patch = new TaskPatchDTO(); patch.setValue("legacy");
        service.applyPatch(task, patch, user);
        assertEquals("legacy", task.getValue());
    }

    @Test void valueOnlyPatchDoesNotEvenResanitizeAbsentHistoricalNotes() {
        successfulWrite();
        String historical = "Historical <script>text</script>";
        task.setNotes(historical);
        var patch = new TaskPatchDTO(); patch.setValue("B");
        service.applyPatch(task, patch, user);
        assertEquals(historical, task.getNotes());
    }

    @Test void overlongValueAndCrossCompanyAreRejected() {
        var patch = new TaskPatchDTO(); patch.setValue("x".repeat(256));
        assertEquals(400, assertThrows(CustomException.class, () -> service.applyPatch(task, patch, user)).getHttpStatus().value());
        Company other = new Company(); other.setId(53L); task.setCompany(other);
        patch.setValue("B");
        assertEquals(403, assertThrows(CustomException.class, () -> service.applyPatch(task, patch, user)).getHttpStatus().value());
    }
}
