package com.grash.integration;

import com.grash.dto.TaskPatchDTO;
import com.grash.dto.offline.MobileOfflineOperation;
import com.grash.dto.offline.MobileOfflineTime;
import com.grash.dto.LaborPatchDTO;
import com.grash.dto.WorkOrderChangeStatusDTO;
import com.grash.exception.CustomException;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import javax.sql.DataSource;

import java.time.Instant;
import java.util.*;
import static com.grash.dto.offline.MobileOfflineResult.Status.*;
import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {"mobile-offline.enabled=true", "mobile-offline.allowed-accounts=*:*",
        "license-key=", "license-file-path=", "spring.quartz.job-store-type=jdbc",
        "spring.quartz.jdbc.initialize-schema=never", "spring.quartz.auto-startup=false",
        "spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate",
        "spring.quartz.scheduler-name=offline-completion-persistence-test"})
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
    @Autowired LaborService laborService;
    @Autowired WorkOrderService workOrderService;
    @Autowired Scheduler scheduler;
    @Autowired DataSource dataSource;
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
        user = em.find(User.class, user.getId()); setCurrentUser(user);
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

    private Labor timer(User owner, long seconds) {
        Labor labor = new Labor(owner, 120, Date.from(Instant.now().minusSeconds(18000)), workOrder,
                true, TimeStatus.STOPPED);
        labor.setCompany(owner.getCompany()); labor.setDuration(seconds);
        em.persist(labor); em.flush();
        return labor;
    }

    private MobileOfflineOperation timeOperation(MobileOfflineOperation.Type type, MobileOfflineTime time,
                                                  UUID predecessor) {
        return new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(),
                workOrder.getId(), null, type, null, null, Instant.now(), null, null, predecessor,
                null, null, null, time);
    }

    private MobileOfflineOperation start(Labor labor, Instant at, UUID predecessor) {
        return timeOperation(MobileOfflineOperation.Type.START_TIMER,
                new MobileOfflineTime(UUID.randomUUID(), at, null, labor.getId(), labor.getVersion(), null,
                        workOrder.getTimerStatusRevision(), false, null), predecessor);
    }

    private MobileOfflineOperation stop(MobileOfflineOperation start, Instant at) {
        return timeOperation(MobileOfflineOperation.Type.STOP_TIMER,
                new MobileOfflineTime(start.time().periodId(), start.time().startedAt(), at,
                        null, null, null, start.time().timerStatusRevision(), false, start), start.operationId());
    }

    private void reattachTimeContext() {
        em.flush(); em.clear();
        user = em.find(User.class, user.getId()); setCurrentUser(user);
        workOrder = em.find(WorkOrder.class, workOrder.getId());
    }

    @Test void closedPeriodsPersistActualVersionsAndReachEightyMinutesWithoutReplayingStarts() {
        Labor labor = timer(user, 1800);
        long laborId = labor.getId(), originalVersion = labor.getVersion();
        Instant firstAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(14400);
        var firstStart = start(labor, firstAt, null);
        var firstStop = stop(firstStart, firstAt.plusSeconds(1800));
        // This next period was also captured before any network response existed.
        var secondStart = start(labor, firstAt.plusSeconds(3600), firstStop.operationId());
        var secondStop = stop(secondStart, firstAt.plusSeconds(4800));

        var first = offline.submit(firstStop, user);
        assertEquals(APPLIED, first.status(), first.code());
        assertEquals(APPLIED, first.startReceipt().status());
        assertTrue(first.timeResult().closedWithoutActivation());
        assertEquals(1800L, first.timeResult().durationAdded());
        assertTrue(first.timeResult().laborVersion() > originalVersion);
        var second = offline.submit(secondStop, user);
        assertEquals(APPLIED, second.status(), second.code());
        assertEquals(1200L, second.timeResult().durationAdded());
        reattachTimeContext();
        Labor persisted = em.find(Labor.class, laborId);
        assertEquals(4800L, persisted.getDuration());
        assertEquals(TimeStatus.STOPPED, persisted.getStatus());
        assertEquals(160L, persisted.getCost());
        assertEquals(second.timeResult().laborVersion(), persisted.getVersion());
        assertEquals(first.startReceipt(), offline.submit(firstStart, user));
        assertEquals(second.startReceipt(), offline.submit(secondStart, user));
        assertEquals(second, offline.submit(secondStop, user));
        assertEquals(first, offline.submit(firstStop, user));
        assertEquals(4800L, em.find(Labor.class, laborId).getDuration());
        var snapshot = offline.prepare(workOrder.getId(), UUID.randomUUID(), user);
        assertEquals(4800L, snapshot.labors().get(0).getDuration());
        assertEquals(second.timeResult().laborVersion(), snapshot.labors().get(0).getVersion());
    }

    @Test void connectedStartThenOfflineStopUsesFieldEndAndSurvivesLostResponses() {
        Labor labor = timer(user, 1800);
        long laborId = labor.getId();
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(10800);
        var start = start(labor, at, null);
        var started = offline.submit(start, user);
        assertEquals(APPLIED, started.status(), started.code());
        UUID run = started.timeResult().timerRunId();
        assertNotNull(run);
        reattachTimeContext();
        assertEquals(TimeStatus.RUNNING, em.find(Labor.class, laborId).getStatus());
        assertEquals(Date.from(at), em.find(Labor.class, laborId).getStartedAt());
        assertEquals(Status.IN_PROGRESS, workOrder.getStatus());
        var stop = stop(start, at.plusSeconds(1800));
        var stopped = offline.submit(stop, user);
        assertEquals(APPLIED, stopped.status(), stopped.code());
        assertEquals(1800L, stopped.timeResult().durationAdded());
        reattachTimeContext();
        Labor stored = em.find(Labor.class, laborId);
        assertEquals(3600L, stored.getDuration());
        assertEquals(TimeStatus.STOPPED, stored.getStatus());
        assertEquals(run, stored.getTimerRunId());
        assertEquals(started, offline.submit(start, user));
        assertEquals(stopped, offline.submit(stop, user));
        assertEquals(3600L, em.find(Labor.class, laborId).getDuration());
    }

    @Test void aRemoteRestartCannotBeStoppedByTheOldOfflinePeriod() {
        Labor labor = timer(user, 1800);
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(7200);
        var start = start(labor, at, null);
        var started = offline.submit(start, user);
        assertEquals(APPLIED, started.status(), started.code());
        Labor current = em.find(Labor.class, labor.getId());
        current.setStatus(TimeStatus.STOPPED); current.setDuration(2400); em.flush();
        UUID replacement = UUID.randomUUID();
        current.setStatus(TimeStatus.RUNNING); current.setTimerRunId(replacement);
        current.setStartedAt(Date.from(at.plusSeconds(3600))); em.flush();

        var refused = offline.submit(stop(start, at.plusSeconds(1800)), user);
        assertEquals(CONFLICT, refused.status(), refused.code());
        reattachTimeContext();
        current = em.find(Labor.class, labor.getId());
        assertEquals(replacement, current.getTimerRunId());
        assertEquals(TimeStatus.RUNNING, current.getStatus());
        assertEquals(2400L, current.getDuration());
    }

    @Test void anOldConnectedCorrectionOrDeletionCannotEraseAnAcceptedPeriod() {
        Labor labor = timer(user, 1800);
        long version = labor.getVersion();
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(7200);
        var start = start(labor, at, null);
        assertEquals(APPLIED, offline.submit(stop(start, at.plusSeconds(1800)), user).status());
        LaborPatchDTO old = new LaborPatchDTO(); old.setVersion(version); old.setDuration(60);
        assertThrows(CustomException.class, () -> laborService.update(labor.getId(), old, user));
        assertThrows(CustomException.class, () -> laborService.delete(labor.getId(), version, user));
        assertEquals(3600L, em.find(Labor.class, labor.getId()).getDuration());
    }

    @Test void anotherTechniciansTimeAndOverlappingHoursDoNotInvalidateTheFirstLine() {
        Labor first = timer(user, 1800);
        User firstUser = user;
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(7200);
        var firstStart = start(first, at, null);
        var firstStop = stop(firstStart, at.plusSeconds(1800));
        User other = new User(); other.setFirstName("Second"); other.setLastName("Worker");
        other.setEmail(UUID.randomUUID() + "@offline.test"); other.setUsername(other.getEmail());
        other.setPassword("test-only"); other.setCompany(user.getCompany()); other.setRole(user.getRole());
        other.setEnabled(true); em.persist(other); em.flush();
        user = other; setCurrentUser(other);
        Labor second = timer(other, 600);
        var secondStart = start(second, at, null);
        assertEquals(APPLIED, offline.submit(stop(secondStart, at.plusSeconds(1200)), user).status());
        user = firstUser; setCurrentUser(firstUser);
        assertEquals(APPLIED, offline.submit(firstStop, user).status());
        assertEquals(3600L, em.find(Labor.class, first.getId()).getDuration());
        assertEquals(1800L, em.find(Labor.class, second.getId()).getDuration());
        assertEquals(0L, workOrder.getTimerStatusRevision());
    }

    @Test void observedLegacyTimerStopsByItsOwnRunWithoutInventingAStartReceipt() {
        Labor labor = timer(user, 1800);
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(7200);
        UUID run = UUID.randomUUID();
        labor.setStatus(TimeStatus.RUNNING); labor.setTimerRunId(run); labor.setStartedAt(Date.from(at)); em.flush();
        var stop = timeOperation(MobileOfflineOperation.Type.STOP_TIMER,
                new MobileOfflineTime(UUID.randomUUID(), at, at.plusSeconds(1800), labor.getId(),
                        labor.getVersion(), run, workOrder.getTimerStatusRevision(), null, null), null);
        var result = offline.submit(stop, user);
        assertEquals(APPLIED, result.status(), result.code()); assertNull(result.startReceipt());
        reattachTimeContext();
        Labor stored = em.find(Labor.class, labor.getId());
        assertEquals(3600L, stored.getDuration()); assertEquals(TimeStatus.STOPPED, stored.getStatus());
        assertEquals(run, stored.getTimerRunId());
        assertEquals(result, offline.submit(stop, user));
        assertEquals(3600L, stored.getDuration());
    }

    @Test void connectedPauseStopsTheRunAndCannotBeUndoneByItsLateOfflineStop() {
        Labor labor = timer(user, 1800);
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(7200);
        var begin = start(labor, at, null);
        assertEquals(APPLIED, offline.submit(begin, user).status());
        WorkOrderChangeStatusDTO pause = new WorkOrderChangeStatusDTO(); pause.setStatus(Status.ON_HOLD);
        pause.setBaseRevision(workOrder.getTimerStatusRevision()); pause.setActiveTimerRuns(workOrderService.activeTimerRuns(workOrder.getId()));
        workOrderService.changeStatus(pause, workOrder.getId(), user, "web");
        reattachTimeContext();
        Labor stopped = em.find(Labor.class, labor.getId());
        long counted = stopped.getDuration();
        assertTrue(counted >= 9000); assertEquals(TimeStatus.STOPPED, stopped.getStatus());
        assertEquals(Status.ON_HOLD, workOrder.getStatus()); assertEquals(1L, workOrder.getTimerStatusRevision());
        var late = offline.submit(stop(begin, at.plusSeconds(1800)), user);
        assertEquals(CONFLICT, late.status(), late.code());
        assertEquals(counted, stopped.getDuration()); assertEquals(Status.ON_HOLD, workOrder.getStatus());
    }
    private MobileOfflineOperation status(Status target, long base, UUID predecessor, List<UUID> dependencies,
                                          List<com.grash.dto.WorkOrderTimerRun> runs) {
        return new MobileOfflineOperation(UUID.randomUUID(), user.getCompany().getId(), user.getId(), workOrder.getId(), null,
                MobileOfflineOperation.Type.CHANGE_WORK_ORDER_STATUS, null, null, Instant.now().minusSeconds(3600),
                null, null, predecessor, null, null, null, null,
                new com.grash.dto.offline.MobileOfflineStatusChange(target, base, "Work completed on site", null, runs, dependencies));
    }

    @Test void acceptedLocalStopPrecedesPauseAndCannotBeCountedUntilReception() {
        Labor labor = timer(user, 0);
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).minusSeconds(7200);
        var begin = start(labor, at, null); var running = offline.submit(begin, user);
        var stop = stop(begin, at.plusSeconds(1800));
        var runs = List.of(new com.grash.dto.WorkOrderTimerRun(running.timeResult().laborId(), running.timeResult().timerRunId()));
        var premature = status(Status.ON_HOLD, 0, null, List.of(stop.operationId()), runs);
        assertEquals(REJECTED, offline.submit(premature, user).status());
        assertEquals(Status.IN_PROGRESS, em.find(WorkOrder.class, workOrder.getId()).getStatus());
        assertEquals(APPLIED, offline.submit(stop, user).status());
        var pause = status(Status.ON_HOLD, 0, null, List.of(stop.operationId()), runs);
        var accepted = offline.submit(pause, user);
        assertEquals(APPLIED, accepted.status(), accepted.code());
        reattachTimeContext();
        assertEquals(1800L, em.find(Labor.class, labor.getId()).getDuration());
        assertEquals(Status.ON_HOLD, workOrder.getStatus());
        assertEquals(accepted, offline.submit(pause, user));
        assertEquals(accepted.statusResult().revision(), workOrder.getTimerStatusRevision());
    }

    @Test void completionReceiptAndPersistedStateKeepServerAndFieldDatesSeparate() {
        var close = status(Status.COMPLETE, 0, null, List.of(), List.of());
        var accepted = offline.submit(close, user);
        assertEquals(APPLIED, accepted.status(), accepted.code());
        reattachTimeContext();
        assertEquals(Status.COMPLETE, workOrder.getStatus());
        assertEquals(user.getId(), workOrder.getCompletedBy().getId());
        assertEquals(accepted.statusResult().completedOn(), workOrder.getCompletedOn().toInstant());
        assertTrue(accepted.statusResult().completedOn().isAfter(close.occurredAt()));
        assertEquals(close.occurredAt(), accepted.statusResult().occurredAt());
        assertEquals(accepted, offline.submit(close, user));
        assertEquals(CONFLICT, offline.submit(status(Status.COMPLETE, workOrder.getTimerStatusRevision(), null, List.of(), List.of()), user).status());
    }

    @Test void statusChainsUseTheirOwnReceiptRevisionAndRejectAConnectedDecisionBetweenThem() {
        var first = status(Status.ON_HOLD, 0, null, List.of(), List.of());
        var accepted = offline.submit(first, user);
        assertEquals(APPLIED, accepted.status());
        var second = status(Status.OPEN, 0, first.operationId(), List.of(), List.of());
        assertEquals(APPLIED, offline.submit(second, user).status());
        reattachTimeContext();
        WorkOrderChangeStatusDTO connected = new WorkOrderChangeStatusDTO();
        connected.setStatus(Status.ON_HOLD); connected.setBaseRevision(workOrder.getTimerStatusRevision());
        connected.setActiveTimerRuns(List.of());
        workOrderService.changeStatus(connected, workOrder.getId(), user, "web");
        var third = status(Status.IN_PROGRESS, 0, second.operationId(), List.of(), List.of());
        assertEquals(CONFLICT, offline.submit(third, user).status());
        assertEquals(Status.ON_HOLD, em.find(WorkOrder.class, workOrder.getId()).getStatus());
    }

    /** Unlike a mocked callback, this checks the real Spring proxy and Quartz JDBC commit boundary. */
    @Test void offlineCompletionPersistsQuartzJobAndTriggerAfterTheReceiptCommit() throws Exception {
        assertTrue(scheduler.getMetaData().isJobStoreSupportsPersistence());
        assertFalse(scheduler.isStarted(), "The test stores jobs without executing them");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate freshTransaction = new TransactionTemplate(transactions);
        freshTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String schedulerName = scheduler.getSchedulerName();
        long companyId = user.getCompany().getId(), userId = user.getId(), roleId = user.getRole().getId();
        long workOrderId = workOrder.getId(), taskId = task.getId(), taskBaseId = task.getTaskBase().getId();
        var preferences = user.getCompany().getCompanySettings().getGeneralPreferences();
        preferences.setDaysBeforePrevMaintNotification(0);
        String timeZone = preferences.getTimeZone();
        PreventiveMaintenance pm = new PreventiveMaintenance();
        pm.setName("Offline completion Quartz fixture"); pm.setTitle("Offline completion Quartz fixture");
        pm.setCompany(user.getCompany());
        pm.getSchedule().setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
        pm.getSchedule().setRecurrenceType(RecurrenceType.DAILY); pm.getSchedule().setFrequency(1);
        em.persist(pm); workOrder.setParentPreventiveMaintenance(pm); em.flush();
        long pmId = pm.getId();
        JobKey scheduledJob = null;
        try {
            var close = status(Status.COMPLETE, workOrder.getTimerStatusRevision(), null, List.of(), List.of());
            var accepted = offline.submit(close, user);
            assertEquals(APPLIED, accepted.status(), accepted.code());
            Calendar nextDate = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
            nextDate.setTime(Date.from(accepted.statusResult().completedOn())); nextDate.add(Calendar.DAY_OF_YEAR, 1);
            scheduledJob = new JobKey("wo-job-chained-" + pm.getSchedule().getId() + "-" + nextDate.getTimeInMillis(), "wo-group");
            String jobName = scheduledJob.getName();
            assertEquals(0L, jdbc.queryForObject("select count(*) from qrtz_job_details where sched_name=? and job_name=? and job_group='wo-group'",
                    Long.class, schedulerName, jobName), "Nothing is scheduled before the receipt transaction commits");

            // Commit the actual closure and fixture. Its afterCommit callback must enter ScheduleService through the proxy.
            TestTransaction.flagForCommit(); TestTransaction.end();
            freshTransaction.executeWithoutResult(transaction -> {
                em.clear();
                assertEquals(Status.COMPLETE, em.find(WorkOrder.class, workOrderId).getStatus());
                assertTrue(receipts.receipt(companyId, userId, close.operationId()).isPresent());
                assertEquals(1L, jdbc.queryForObject("select count(*) from qrtz_job_details where sched_name=? and job_name=? and job_group='wo-group'",
                        Long.class, schedulerName, jobName));
                assertEquals(1L, jdbc.queryForObject("""
                        select count(*) from qrtz_triggers t join qrtz_simple_triggers s
                        on s.sched_name=t.sched_name and s.trigger_name=t.trigger_name and s.trigger_group=t.trigger_group
                        where t.sched_name=? and t.job_name=? and t.job_group='wo-group' and t.start_time=?
                        """, Long.class, schedulerName, jobName, nextDate.getTimeInMillis()));
            });
            freshTransaction.executeWithoutResult(transaction -> {
                User current = em.find(User.class, userId); setCurrentUser(current);
                assertEquals(accepted, offline.submit(close, current), "A lost response is resolved by the receipt");
            });
            freshTransaction.executeWithoutResult(transaction -> {
                assertEquals(1L, jdbc.queryForObject("select count(*) from qrtz_job_details where sched_name=? and job_name=? and job_group='wo-group'",
                        Long.class, schedulerName, jobName));
                assertEquals(1L, jdbc.queryForObject("select count(*) from qrtz_triggers where sched_name=? and job_name=? and job_group='wo-group'",
                        Long.class, schedulerName, jobName), "Replay does not create another trigger");
            });
        } finally {
            if (TestTransaction.isActive()) { TestTransaction.flagForRollback(); TestTransaction.end(); }
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            JobKey cleanupJob = scheduledJob;
            freshTransaction.executeWithoutResult(transaction -> {
                if (cleanupJob != null) {
                    try { scheduler.deleteJob(cleanupJob); }
                    catch (SchedulerException failure) { throw new IllegalStateException("Cannot remove the test Quartz job", failure); }
                }
                jdbc.update("delete from mobile_offline_receipt where company_id=? and user_id=?", companyId, userId);
                // The committed fixture is the exception to this class's normal per-test rollback.
                em.clear();
                Task storedTask = em.find(Task.class, taskId); if (storedTask != null) em.remove(storedTask);
                em.flush();
                jdbc.update("delete from work_order_aud where id=?", workOrderId);
                jdbc.update("delete from work_order where id=?", workOrderId);
                jdbc.update("delete from revinfo where user_id=?", userId);
                TaskBase base = em.find(TaskBase.class, taskBaseId); if (base != null) em.remove(base);
                PreventiveMaintenance maintenance = em.find(PreventiveMaintenance.class, pmId);
                if (maintenance != null) em.remove(maintenance);
                em.flush();
                User worker = em.find(User.class, userId); if (worker != null) em.remove(worker);
                em.flush();
                Role role = em.find(Role.class, roleId); if (role != null) em.remove(role);
                em.flush();
                Company company = em.find(Company.class, companyId);
                if (company != null) {
                    CompanySettings settings = company.getCompanySettings();
                    company.setCompanySettings(null);
                    if (settings != null) settings.setCompany(null);
                    em.flush();
                    if (settings != null) em.remove(settings);
                    em.remove(company);
                }
            });
        }
    }

}
