package com.grash.integration;

import com.grash.dto.TaskPatchDTO;
import com.grash.dto.offline.MobileOfflineOperation;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.model.enums.workflow.TaskAction;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.repository.MobileOfflineRepository;
import com.grash.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.*;
import static com.grash.dto.offline.MobileOfflineResult.Status.*;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {"mobile-offline.enabled=true", "mobile-offline.allowed-accounts=*:*"})
@RecordApplicationEvents
@Transactional
class MobileOfflinePersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired MobileOfflineService offline;
    @Autowired TaskService taskService;
    @Autowired MobileOfflineAccessService access;
    @Autowired MobileOfflineRepository receipts;
    @Autowired ApplicationEvents events;
    @Autowired PlatformTransactionManager transactions;
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

    @Test void valueAndNotesUseOneCausalJpaVersionAndExternalNotesStillConflict() {
        long base = task.getVersion();
        var value = operation(base);
        var appliedValue = offline.submit(value, user);
        var notes = new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(), workOrder.getId(), task.getId(),
                MobileOfflineOperation.Type.SET_TASK_NOTES, base, null, Instant.now(), "field notes", null, value.operationId());
        var appliedNotes = offline.submit(notes, user);
        assertEquals(APPLIED, appliedNotes.status()); assertEquals(base, appliedNotes.chainBaseVersion());
        assertTrue(appliedNotes.task().getVersion() > appliedValue.task().getVersion());
        assertEquals("B", appliedNotes.task().getValue()); assertEquals("field notes", appliedNotes.task().getNotes());
        TaskPatchDTO connected = new TaskPatchDTO(); connected.setVersion(appliedNotes.task().getVersion()); connected.setNotes("web notes");
        taskService.update(task.getId(), connected, user);
        var later = new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(), workOrder.getId(), task.getId(),
                MobileOfflineOperation.Type.SET_TASK_VALUE, base, "C", Instant.now(), null, null, notes.operationId());
        assertEquals(CONFLICT, offline.submit(later, user).status());
        em.clear();
        Task stored = em.find(Task.class, task.getId());
        assertEquals("B", stored.getValue()); assertEquals("web notes", stored.getNotes());
    }

    MobileOfflineOperation comment(String content) {
        return new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(), workOrder.getId(), null,
                MobileOfflineOperation.Type.CREATE_COMMENT, null, null, Instant.now(), null, content, null);
    }

    @Test void commentIdentitySurvivesReplayEditingDeletionAndSnapshotReconciliation() {
        var operation = comment("<b>field comment</b>");
        var result = offline.submit(operation, user);
        assertEquals(APPLIED, result.status()); assertNotNull(result.commentId()); assertNull(result.task());
        em.flush(); em.clear();
        user = em.find(User.class, user.getId()); setCurrentUser(user);
        Comment stored = em.find(Comment.class, result.commentId());
        assertEquals(operation.operationId(), stored.getSourceOperationId()); assertEquals("field comment", stored.getContent());
        assertEquals(user.getId(), stored.getUser().getId());
        assertEquals(user.getCompany().getId(), stored.getCompany().getId());
        assertEquals(workOrder.getId(), stored.getWorkOrder().getId());
        assertTrue(receipts.receipt(user.getCompany().getId(), user.getId(), operation.operationId()).isPresent());
        assertTrue(receipts.receipt(Long.MAX_VALUE, user.getId(), operation.operationId()).isEmpty());
        assertTrue(receipts.receipt(user.getCompany().getId(), Long.MAX_VALUE, operation.operationId()).isEmpty());
        assertEquals(1, events.stream(CommentService.OfflineCommentCreated.class).count());
        stored.setContent("edited on web"); em.flush();
        assertEquals(result, offline.submit(operation, user));
        assertEquals(1, events.stream(CommentService.OfflineCommentCreated.class).count(), "Replay does not publish another notification event");
        var snapshot = offline.prepare(workOrder.getId(), UUID.randomUUID(), user);
        assertTrue(snapshot.canComment()); assertEquals(1, snapshot.comments().size());
        assertEquals(operation.operationId(), snapshot.comments().get(0).getSourceOperationId());
        assertEquals("edited on web", snapshot.comments().get(0).getContent());
        em.remove(stored); em.flush(); em.clear();
        user = em.find(User.class, user.getId()); setCurrentUser(user);
        var receipt = offline.receipts(List.of(operation.operationId()), user).results().get(0);
        assertEquals(APPLIED, receipt.status()); assertEquals(result.commentId(), receipt.commentId());
        assertEquals(com.grash.dto.offline.MobileOfflineResult.Visibility.COMMENT_DELETED, receipt.visibility());
        assertTrue(offline.prepare(workOrder.getId(), UUID.randomUUID(), user).comments().isEmpty());
    }

    @Test void rejectedOfflineCommentKeepsItsReceiptAndTransactionUsable() {
        var rejected = comment("<p> </p>");
        var result = offline.submit(rejected, user);
        assertEquals(REJECTED, result.status());
        assertEquals("INVALID_COMMENT_CONTENT", result.code());
        em.flush(); em.clear();
        user = em.find(User.class, user.getId()); setCurrentUser(user);
        assertEquals(0L, em.createQuery("select count(c) from Comment c where c.workOrder.id = :workOrderId", Long.class)
                .setParameter("workOrderId", workOrder.getId()).getSingleResult());
        assertEquals(result, offline.receipts(List.of(rejected.operationId()), user).results().get(0));
        assertEquals(0, events.stream(CommentService.OfflineCommentCreated.class).count());
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                assertFalse(status.isRollbackOnly(), "Validation must not invalidate the shared receipt transaction"));

        // A refused input must not mark the shared transaction rollback-only.
        var accepted = comment("next valid comment");
        var applied = offline.submit(accepted, user);
        assertEquals(APPLIED, applied.status());
        em.flush(); em.clear();
        assertEquals(accepted.operationId(), em.find(Comment.class, applied.commentId()).getSourceOperationId());
        assertTrue(receipts.receipt(user.getCompany().getId(), user.getId(), rejected.operationId()).isPresent());
        assertTrue(receipts.receipt(user.getCompany().getId(), user.getId(), accepted.operationId()).isPresent());
        assertEquals(1, events.stream(CommentService.OfflineCommentCreated.class).count());
    }

    @Test void sourceOperationIsUniqueForAnAuthorInTheCompany() {
        var operation = comment("first");
        offline.submit(operation, user);
        Comment duplicate = new Comment(); duplicate.setCompany(user.getCompany()); duplicate.setUser(user);
        duplicate.setWorkOrder(workOrder); duplicate.setContent("duplicate"); duplicate.setSourceOperationId(operation.operationId());
        assertThrows(RuntimeException.class, () -> { em.persist(duplicate); em.flush(); });
    }

    @Test void meterTaskValueDoesNotCreateAMeterReading() {
        task.getTaskBase().setTaskType(TaskType.METER); em.flush();
        long before = em.createQuery("select count(r) from Reading r", Long.class).getSingleResult();
        var operation = new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(), workOrder.getId(), task.getId(),
                MobileOfflineOperation.Type.SET_TASK_VALUE, task.getVersion(), "123.5", Instant.now());
        assertEquals(APPLIED, offline.submit(operation, user).status());
        em.flush(); em.clear();
        assertEquals("123.5", em.find(Task.class, task.getId()).getValue());
        assertEquals(before, em.createQuery("select count(r) from Reading r", Long.class).getSingleResult());
    }

    @Test void taskWorkflowChangesAssetOnceAndReplayDoesNotRunItAgain() {
        Asset asset = new Asset(); asset.setName("offline workflow asset"); asset.setCompany(user.getCompany());
        asset.setStatus(AssetStatus.OPERATIONAL); em.persist(asset);
        workOrder.setAsset(asset);
        WorkflowAction action = new WorkflowAction(); action.setCompany(user.getCompany());
        action.setTaskAction(TaskAction.SET_ASSET_STATUS); action.setAssetStatus(AssetStatus.DOWN);
        Workflow workflow = new Workflow(); workflow.setCompany(user.getCompany()); workflow.setTitle("offline task update");
        workflow.setMainCondition(WFMainCondition.TASK_UPDATED); workflow.setAction(action); em.persist(workflow); em.flush();
        var operation = operation(task.getVersion());
        assertEquals(APPLIED, offline.submit(operation, user).status());
        assertEquals(AssetStatus.DOWN, asset.getStatus());
        asset.setStatus(AssetStatus.OPERATIONAL); em.flush();
        assertEquals(APPLIED, offline.submit(operation, user).status());
        em.clear();
        assertEquals(AssetStatus.OPERATIONAL, em.find(Asset.class, asset.getId()).getStatus());
    }
}
