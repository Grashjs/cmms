package com.grash.integration;

import com.grash.dto.TaskPatchDTO;
import com.grash.dto.offline.MobileOfflineOperation;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.*;
import static com.grash.dto.offline.MobileOfflineResult.Status.*;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {"mobile-offline.enabled=true", "mobile-offline.allowed-accounts=*:*"})
@Transactional
class MobileOfflinePersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired MobileOfflineService offline;
    @Autowired TaskService taskService;
    @Autowired MobileOfflineAccessService access;
    User user;
    WorkOrder workOrder;
    Task task;

    @BeforeEach void fixture() {
        Company company = new Company(); company.setName("Offline isolated test"); em.persist(company);
        Role role = new Role(); role.setName("Offline worker"); role.setRoleType(RoleType.ROLE_CLIENT);
        role.setCompanySettings(company.getCompanySettings());
        role.setViewPermissions(new HashSet<>(Set.of(PermissionEntity.WORK_ORDERS)));
        role.setViewOtherPermissions(new HashSet<>(Set.of(PermissionEntity.WORK_ORDERS)));
        role.setEditOtherPermissions(new HashSet<>(Set.of(PermissionEntity.WORK_ORDERS))); em.persist(role);
        user = new User(); user.setFirstName("Offline"); user.setLastName("Worker");
        user.setEmail(UUID.randomUUID() + "@offline.test"); user.setUsername(user.getEmail()); user.setPassword("test-only");
        user.setCompany(company); user.setRole(role); user.setEnabled(true); em.persist(user); em.flush();
        setCurrentUser(user);
        workOrder = new WorkOrder(); workOrder.setTitle("Offline test"); workOrder.setCompany(company); em.persist(workOrder);
        TaskBase base = new TaskBase(); base.setLabel("Inspection observation"); base.setTaskType(TaskType.TEXT);
        base.setCompany(company); em.persist(base);
        task = new Task(base, workOrder, null, "A"); task.setCompany(company); task.setNotes("old notes"); em.persist(task);
        em.flush(); em.clear();
        user = em.find(User.class, user.getId()); setCurrentUser(user);
        task = em.find(Task.class, task.getId()); workOrder = em.find(WorkOrder.class, workOrder.getId());
    }

    MobileOfflineOperation operation(long version) {
        return new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(), workOrder.getId(), task.getId(),
                MobileOfflineOperation.Type.SET_TASK_VALUE, version, "B", Instant.now());
    }

    @Test void connectedNotesPreserveValueAndOfflineDraftBaseThenConflicts() {
        long base = task.getVersion();
        TaskPatchDTO notes = new TaskPatchDTO(); notes.setNotes("new notes"); notes.setVersion(base);
        taskService.update(task.getId(), notes, user);
        em.clear();
        // Fixture accounts are still uncommitted in this test transaction. Reattach before loading
        // lazy role collections; a detached lazy-load connection cannot see uncommitted fixture rows.
        user = em.find(User.class, user.getId());
        setCurrentUser(user);
        Task fresh = em.find(Task.class, task.getId());
        assertEquals("A", fresh.getValue()); assertEquals("new notes", fresh.getNotes());
        assertTrue(fresh.getVersion() > base);
        assertTrue(access.canView(workOrder.getId(), user), "Current worker still has read access");
        assertTrue(access.canEdit(workOrder.getId(), user), "Current worker still has edit access");
        assertDoesNotThrow(() -> taskService.checkEditAccess(fresh, user, true));
        var result = offline.submit(operation(base), user);
        assertEquals(CONFLICT, result.status(), result.code());
        assertEquals("A", result.task().getValue());
    }

    @Test void sharedMutationReceiptAndSnapshotUseTheActualJpaVersion() {
        long base = task.getVersion();
        var op = operation(base);
        var applied = offline.submit(op, user);
        assertEquals(APPLIED, applied.status()); assertTrue(applied.task().getVersion() > base);
        var replay = offline.submit(op, user); assertEquals(applied, replay);
        var snapshot = offline.prepare(workOrder.getId(), UUID.randomUUID(), user);
        assertTrue(snapshot.complete()); assertEquals(1, snapshot.tasks().size());
        assertEquals(applied.task().getVersion(), snapshot.tasks().get(0).getVersion());
        assertEquals("B", snapshot.tasks().get(0).getValue());
    }

    @Test void deletedTaskIsMissingFromCompleteSnapshotButReceiptRemains() {
        var op = operation(task.getVersion());
        assertEquals(APPLIED, offline.submit(op, user).status());
        em.remove(em.find(Task.class, task.getId())); em.flush(); em.clear();
        var snapshot = offline.prepare(workOrder.getId(), UUID.randomUUID(), user);
        assertTrue(snapshot.complete()); assertTrue(snapshot.tasks().isEmpty());
        assertEquals(APPLIED, offline.receipts(List.of(op.operationId()), user).results().get(0).status());
        assertEquals(REJECTED, offline.submit(operation(0), user).status());
    }
}
