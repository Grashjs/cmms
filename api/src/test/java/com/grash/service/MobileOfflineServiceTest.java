package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.configuration.MobileOfflineProperties;
import com.grash.dto.TaskShowDTO;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.*;
import com.grash.model.enums.TaskType;
import com.grash.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.stream.LongStream;
import static com.grash.dto.offline.MobileOfflineResult.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileOfflineServiceTest {
    @Mock MobileOfflineProperties properties;
    @Mock MobileOfflineRepository repository;
    @Mock MobileOfflineSnapshotService snapshots;
    @Mock MobileOfflineAccessService access;
    @Mock TaskRepository tasks;
    @Mock TaskService mutations;
    @Mock TaskMapper mapper;
    @Mock CommentService comments;
    @Mock MobileOfflineFileService files;
    @Mock WorkOrderService workOrders;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    @Mock com.grash.repository.CommentRepository commentRepository;
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    MobileOfflineService service;
    User user;
    Task task;

    @BeforeEach void setup() {
        service = new MobileOfflineService(properties, repository, snapshots, access, tasks, mutations, mapper, json, comments, commentRepository, files, workOrders, events);
        Company company = new Company(); company.setId(52L);
        user = new User(); user.setId(34L); user.setCompany(company);
        WorkOrder wo = new WorkOrder(); wo.setId(10L); wo.setCompany(company);
        TaskBase base = new TaskBase(); base.setTaskType(TaskType.TEXT);
        task = new Task(base, wo, null, "A"); task.setVersion(3); task.setId(7L); task.setCompany(company);
    }

    MobileOfflineOperation operation() {
        return new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, 7L, MobileOfflineOperation.Type.SET_TASK_VALUE,
                3L, "B", Instant.parse("2026-11-03T12:00:00Z"));
    }

    void eligible() { when(properties.isEligible(user)).thenReturn(true); }
    void writable() {
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        when(access.canEdit(10L, user)).thenReturn(true);
        when(tasks.findForUpdate(7L, 52L)).thenReturn(Optional.of(task));
    }
    void first(MobileOfflineOperation op) {
        when(repository.reserveOperation(eq(52L), eq(34L), eq(op.operationId()), eq(10L), anyString(), any())).thenReturn(true);
    }

    Map<UUID, MobileOfflineRepository.Receipt> ledger() {
        Map<UUID, MobileOfflineRepository.Receipt> saved = new HashMap<>();
        when(repository.reserveOperation(eq(52L), eq(34L), any(), eq(10L), anyString(), any())).thenAnswer(call -> {
            UUID id = call.getArgument(2);
            if (saved.containsKey(id)) return false;
            saved.put(id, new MobileOfflineRepository.Receipt(id, 10L, call.getArgument(4), null));
            return true;
        });
        doAnswer(call -> {
            UUID id = call.getArgument(2);
            var previous = saved.get(id);
            saved.put(id, new MobileOfflineRepository.Receipt(id, 10L, previous.payloadHash(), call.getArgument(3)));
            return null;
        }).when(repository).finishOperation(eq(52L), eq(34L), any(), anyString());
        lenient().when(repository.receipt(eq(52L), eq(34L), any())).thenAnswer(call -> Optional.ofNullable(saved.get(call.getArgument(2))));
        return saved;
    }

    void taskMutation() {
        when(mutations.applyPatch(eq(task), any(), eq(user))).thenAnswer(call -> {
            com.grash.dto.TaskPatchDTO patch = call.getArgument(1);
            assertEquals(task.getVersion(), patch.getVersion());
            if (patch.hasValue()) task.setValue(patch.getValue());
            if (patch.hasNotes()) task.setNotes(patch.getNotes());
            task.setVersion(task.getVersion() + 1);
            return task;
        });
        when(mapper.toShowDto(task)).thenAnswer(call -> {
            TaskShowDTO result = new TaskShowDTO();
            result.setId(task.getId()); result.setVersion(task.getVersion());
            result.setValue(task.getValue()); result.setNotes(task.getNotes());
            return result;
        });
    }

    MobileOfflineOperation notesOperation(UUID predecessor) {
        return new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, 7L,
                MobileOfflineOperation.Type.SET_TASK_NOTES, 3L, null, Instant.now(), "notes B", null, predecessor);
    }

    MobileOfflineOperation commentOperation(String content) {
        return new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.CREATE_COMMENT, null, null, Instant.now(), null, content, null);
    }

    @Test void localGrantIsSevenDaysAndMissingEligibilityDoesNotGrantAccess() {
        var denied = service.eligibility(user); assertNull(denied.authorizedUntil()); assertFalse(denied.eligible());
        eligible();
        var allowed = service.eligibility(user);
        assertEquals(Duration.ofDays(7), Duration.between(allowed.serverTime(), allowed.authorizedUntil()));
    }

    @Test void disabledFeatureNeverMutatesOrPrepares() {
        assertEquals(403, assertThrows(CustomException.class, () -> service.submit(operation(), user)).getHttpStatus().value());
        assertThrows(CustomException.class, () -> service.prepare(10, UUID.randomUUID(), user));
        verifyNoInteractions(repository, tasks, mutations, snapshots);
    }

    @Test void appliedReceiptAndMutationUseStableIdAndReplayDoesNotInvokeMutationAgain() throws Exception {
        eligible(); writable();
        var op = operation();
        Map<UUID, MobileOfflineRepository.Receipt> saved = new HashMap<>();
        when(repository.reserveOperation(eq(52L), eq(34L), eq(op.operationId()), eq(10L), anyString(), any()))
                .thenAnswer(invocation -> {
                    if (saved.containsKey(op.operationId())) return false;
                    saved.put(op.operationId(), new MobileOfflineRepository.Receipt(op.operationId(), 10L,
                            invocation.getArgument(4), null));
                    return true;
                });
        doAnswer(invocation -> {
            var old = saved.get(op.operationId());
            saved.put(op.operationId(), new MobileOfflineRepository.Receipt(old.operationId(), old.workOrderId(),
                    old.payloadHash(), invocation.getArgument(3)));
            return null;
        }).when(repository).finishOperation(eq(52L), eq(34L), eq(op.operationId()), anyString());
        when(repository.receipt(52L, 34L, op.operationId())).thenAnswer(i -> Optional.ofNullable(saved.get(op.operationId())));
        when(mutations.applyPatch(eq(task), any(), eq(user))).thenReturn(task);
        TaskShowDTO dto = new TaskShowDTO(); dto.setId(7L); dto.setVersion(4); dto.setValue("B");
        when(mapper.toShowDto(task)).thenReturn(dto);
        when(tasks.existsByIdAndCompany_IdAndWorkOrder_Id(7L, 52L, 10L)).thenReturn(true);

        var initial = service.submit(op, user);
        var replay = service.submit(op, user);
        assertEquals(APPLIED, initial.status()); assertEquals(initial, replay);
        verify(mutations, times(1)).applyPatch(eq(task), argThat(p -> p.hasValue() && !p.hasNotes() && p.getVersion() == 3), eq(user));
        var altered = new MobileOfflineOperation(op.operationId(), 52L, 34L, 10L, 7L, op.type(), 3L, "different", op.occurredAt());
        assertEquals("OPERATION_ID_REUSED", assertThrows(CustomException.class, () -> service.submit(altered, user)).getMessage());
    }

    @Test void webConflictIsDurableAndNeverCallsMutation() {
        eligible(); writable();
        var op = operation(); first(op); task.setVersion(4);
        when(mapper.toShowDto(task)).thenReturn(new TaskShowDTO());
        var result = service.submit(op, user);
        assertEquals(CONFLICT, result.status()); assertEquals("VERSION_CONFLICT", result.code());
        verify(mutations, never()).applyPatch(any(), any(), any());
        verify(repository).finishOperation(eq(52L), eq(34L), eq(op.operationId()), contains("CONFLICT"));
    }

    @Test void deletedTaskIsRejectedAndNoMatchingByLabelIsAttempted() {
        eligible();
        var op = operation(); first(op);
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        when(access.canEdit(10L, user)).thenReturn(true);
        when(tasks.findForUpdate(7L, 52L)).thenReturn(Optional.empty());
        assertEquals("TARGET_NOT_FOUND", service.submit(op, user).code());
        verify(mutations, never()).applyPatch(any(), any(), any());
    }

    @Test void receiptLookupRemainsAvailableWhenDisabledAndRedactsRevokedTarget() throws Exception {
        UUID id = UUID.randomUUID();
        TaskShowDTO dto = new TaskShowDTO(); dto.setValue("private");
        String stored = json.writeValueAsString(new MobileOfflineResult(id, APPLIED, "APPLIED", dto, Instant.now()));
        when(repository.receipt(52L, 34L, id)).thenReturn(Optional.of(new MobileOfflineRepository.Receipt(id, 10, "hash", stored)));
        var receipts = service.receipts(List.of(id), user);
        assertNull(receipts.results().get(0).task()); assertEquals(APPLIED, receipts.results().get(0).status());
        verifyNoInteractions(properties);
    }

    @Test void receiptLookupIsBoundedAndAlwaysScopedToCurrentOwner() {
        List<UUID> ids = java.util.stream.IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).toList();
        when(repository.receipt(eq(52L), eq(34L), any())).thenReturn(Optional.empty());
        assertEquals(ids, service.receipts(ids, user).missingOperationIds());
        List<UUID> over = new ArrayList<>(ids); over.add(UUID.randomUUID());
        assertThrows(CustomException.class, () -> service.receipts(over, user));
        verify(repository, times(100)).receipt(eq(52L), eq(34L), any());
    }

    @Test void preparationsQuotaAllowsRefreshAndFailedSnapshotDoesNotReserve() {
        eligible();
        UUID device = UUID.randomUUID();
        when(repository.preparedIds(52L, 34L, device)).thenReturn(LongStream.rangeClosed(1, 25).boxed().toList());
        assertEquals("PREPARED_LIMIT_EXCEEDED", assertThrows(CustomException.class,
                () -> service.prepare(26, device, user)).getMessage());
        when(snapshots.build(10L, user)).thenThrow(new CustomException("SNAPSHOT_LIMIT_EXCEEDED", org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE));
        assertThrows(CustomException.class, () -> service.prepare(10, device, user));
        verify(repository, never()).prepare(anyLong(), anyLong(), any(), anyLong(), any());
    }

    @Test void removePreparationIsOwnerScopedAndAllowedAfterEligibilityWithdrawal() {
        UUID device = UUID.randomUUID();
        service.removePreparation(10, device, user);
        verify(repository).removePreparation(52, 34, device, 10);
        verifyNoInteractions(properties, snapshots, tasks);
    }

    @Test void anotherAccountCannotSubmitTheOriginalImmutableOperation() {
        var op = operation();
        user.setId(35L);
        assertEquals("OFFLINE_CONTEXT_MISMATCH", assertThrows(CustomException.class, () -> service.submit(op, user)).getMessage());
        verifyNoInteractions(repository, properties, mutations);
    }

    @Test void ownValueAndNotesChainUsesOnlyPredecessorVersionAndDetectsInterveningWebEdit() {
        eligible(); writable(); ledger(); taskMutation();
        var first = operation();
        var applied = service.submit(first, user);
        assertEquals(3L, applied.chainBaseVersion()); assertEquals(4, applied.task().getVersion());
        var next = notesOperation(first.operationId());
        var saved = service.submit(next, user);
        assertEquals(APPLIED, saved.status()); assertEquals(3L, saved.chainBaseVersion());
        assertEquals("B", saved.task().getValue()); assertEquals("notes B", saved.task().getNotes());
        task.setVersion(6); // A concurrent connected edit after our second receipt.
        var third = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, 7L,
                first.type(), 3L, "C", Instant.now(), null, null, next.operationId());
        assertEquals(CONFLICT, service.submit(third, user).status());
        verify(mutations, times(2)).applyPatch(any(), any(), any());
    }

    @Test void unrelatedRootsTargetsAndLegacyReceiptsCannotAuthorizeAChain() throws Exception {
        eligible(); writable(); ledger();
        UUID predecessorId = UUID.randomUUID();
        var dto = new TaskShowDTO(); dto.setId(7L); dto.setVersion(4);
        var invalid = List.of(
                new MobileOfflineResult(predecessorId, APPLIED, "APPLIED", dto, Instant.now()),
                new MobileOfflineResult(predecessorId, APPLIED, "APPLIED", dto, Instant.now(), 2L, null, null),
                new MobileOfflineResult(predecessorId, CONFLICT, "VERSION_CONFLICT", dto, Instant.now(), 3L, null, null));
        for (var result : invalid) {
            when(repository.receipt(52L, 34L, predecessorId)).thenReturn(Optional.of(new MobileOfflineRepository.Receipt(predecessorId, 10L, "hash", json.writeValueAsString(result))));
            assertEquals("INVALID_PREDECESSOR", service.submit(notesOperation(predecessorId), user).code());
        }
        dto.setId(8L);
        var otherTask = new MobileOfflineResult(predecessorId, APPLIED, "APPLIED", dto, Instant.now(), 3L, null, null);
        when(repository.receipt(52L, 34L, predecessorId)).thenReturn(Optional.of(new MobileOfflineRepository.Receipt(predecessorId, 10L, "hash", json.writeValueAsString(otherTask))));
        assertEquals("INVALID_PREDECESSOR", service.submit(notesOperation(predecessorId), user).code());
        when(repository.receipt(52L, 34L, predecessorId)).thenReturn(Optional.empty());
        assertEquals("INVALID_PREDECESSOR", service.submit(notesOperation(predecessorId), user).code());
        verify(mutations, never()).applyPatch(any(), any(), any());
    }

    @Test void oldOperationKeepsItsExactShaAndCanReadAnOldReceipt() throws Exception {
        eligible(); when(access.canView(10L, user)).thenReturn(true);
        json.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var old = new MobileOfflineOperation(UUID.fromString("00000000-0000-0000-0000-000000000001"), 52L, 34L, 10L, 7L,
                MobileOfflineOperation.Type.SET_TASK_VALUE, 3L, "B", Instant.parse("2026-11-03T12:00:00Z"));
        String legacy = "{\"operationId\":\"00000000-0000-0000-0000-000000000001\",\"status\":\"APPLIED\",\"code\":\"APPLIED\",\"task\":null,\"receivedAt\":\"2026-11-03T12:00:01Z\"}";
        String sha = "68b9ed9055d48ed6425801da74ee92f140b2f9f0c8e2e45f9b0c8efef96530dc";
        when(repository.receipt(52L, 34L, old.operationId())).thenReturn(Optional.of(new MobileOfflineRepository.Receipt(old.operationId(), 10L, sha, legacy)));
        assertEquals(APPLIED, service.submit(old, user).status());
        verify(repository).reserveOperation(eq(52L), eq(34L), eq(old.operationId()), eq(10L), eq(sha), any());
        verify(mutations, never()).applyPatch(any(), any(), any());
    }

    @Test void anOperationCannotDependOnItsOwnUnfinishedReceipt() {
        eligible(); writable(); ledger();
        UUID id = UUID.randomUUID();
        var self = new MobileOfflineOperation(id, 52L, 34L, 10L, 7L,
                MobileOfflineOperation.Type.SET_TASK_NOTES, 3L, null, Instant.now(), "notes", null, id);
        assertEquals("INVALID_PREDECESSOR", service.submit(self, user).code());
        verify(mutations, never()).applyPatch(any(), any(), any());
    }

    @Test void invalidNumbersAndOverlongNotesAreDurableRejectionsBeforeMutation() {
        eligible(); writable(); var records = ledger();
        task.getTaskBase().setTaskType(TaskType.NUMBER);
        var invalidNumber = operation();
        assertEquals("INVALID_NUMBER", service.submit(invalidNumber, user).code());
        var invalidNotes = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, 7L,
                MobileOfflineOperation.Type.SET_TASK_NOTES, 3L, null, Instant.now(), "x".repeat(256), null, null);
        assertEquals("NOTES_TOO_LONG", service.submit(invalidNotes, user).code());
        assertTrue(records.values().stream().allMatch(receipt -> receipt.resultJson().contains("REJECTED")));
        verify(mutations, never()).applyPatch(any(), any(), any());
    }

    @Test void commentCreationNeedsViewNotTaskEditAndReplayHasNoSideEffectsOrContent() throws Exception {
        eligible(); var records = ledger();
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        when(commentRepository.existsByIdAndCompany_IdAndWorkOrder_Id(99L, 52L, 10L)).thenReturn(true);
        var op = commentOperation("<b>new comment</b>");
        Comment created = new Comment(); created.setId(99L);
        when(comments.createOffline(argThat(request -> request.getContent().equals("new comment") && request.getFiles().isEmpty()), eq(user), eq(op.operationId())))
                .thenReturn(org.springframework.data.util.Pair.of(created, new com.grash.dto.comment.CommentShowDTO()));
        var result = service.submit(op, user);
        assertEquals(99L, result.commentId()); assertEquals(MobileOfflineResult.Visibility.VISIBLE, result.visibility());
        assertNull(result.task());
        assertEquals(result, service.submit(op, user));
        assertFalse(records.get(op.operationId()).resultJson().contains("new comment"));
        verify(comments, times(1)).createOffline(any(), eq(user), eq(op.operationId()));
        verify(access, never()).canEdit(anyLong(), any()); verifyNoInteractions(tasks, mutations);
    }

    @Test void commentWhitespaceAfterSanitizationIsRejectedDurablyButLongTextHasNoArbitraryLimit() {
        eligible(); var records = ledger();
        when(access.existsInCompany(10L, user)).thenReturn(true); when(access.canView(10L, user)).thenReturn(true);
        var empty = commentOperation("<p> </p>");
        assertEquals("INVALID_COMMENT_CONTENT", service.submit(empty, user).code());
        assertTrue(records.get(empty.operationId()).resultJson().contains("REJECTED"));
        verifyNoInteractions(comments);
        var longComment = commentOperation("x".repeat(20_000));
        Comment created = new Comment(); created.setId(99L);
        when(comments.createOffline(any(), eq(user), eq(longComment.operationId())))
                .thenReturn(org.springframework.data.util.Pair.of(created, new com.grash.dto.comment.CommentShowDTO()));
        assertEquals(APPLIED, service.submit(longComment, user).status());
    }

    @Test void commentOperationCannotCrossCompanyOrUserBeforeAnyReservation() {
        var op = commentOperation("private comment");
        user.setId(35L);
        assertEquals("OFFLINE_CONTEXT_MISMATCH", assertThrows(CustomException.class,
                () -> service.submit(op, user)).getMessage());
        user.setId(34L);
        Company anotherCompany = new Company(); anotherCompany.setId(53L); user.setCompany(anotherCompany);
        assertEquals("OFFLINE_CONTEXT_MISMATCH", assertThrows(CustomException.class,
                () -> service.submit(op, user)).getMessage());
        verifyNoInteractions(repository, properties, comments, commentRepository, access);
    }

    @Test void commentWithoutCurrentViewAccessRecordsRejectionWithoutCreation() {
        eligible(); var records = ledger();
        when(access.existsInCompany(10L, user)).thenReturn(true);
        var op = commentOperation("no longer accessible");
        var result = service.submit(op, user);
        assertEquals(REJECTED, result.status()); assertEquals("ACCESS_REVOKED", result.code());
        assertTrue(records.get(op.operationId()).resultJson().contains("REJECTED"));
        verifyNoInteractions(comments, commentRepository);
    }

    @Test void commentReceiptAlwaysKeepsIdentityAndReportsCurrentVisibilityWithoutHistoricalContent() throws Exception {
        UUID id = UUID.randomUUID();
        var result = new MobileOfflineResult(id, APPLIED, "APPLIED", null, Instant.now(), null, 99L, MobileOfflineResult.Visibility.VISIBLE);
        when(repository.receipt(52L, 34L, id)).thenReturn(Optional.of(new MobileOfflineRepository.Receipt(id, 10L, "hash", json.writeValueAsString(result))));
        var inaccessible = service.receipts(List.of(id), user).results().get(0);
        assertEquals(99L, inaccessible.commentId()); assertEquals(APPLIED, inaccessible.status());
        assertEquals(MobileOfflineResult.Visibility.WORK_ORDER_UNAVAILABLE, inaccessible.visibility());
        when(access.canView(10L, user)).thenReturn(true);
        var deleted = service.receipts(List.of(id), user).results().get(0);
        assertEquals(MobileOfflineResult.Visibility.COMMENT_DELETED, deleted.visibility());
        assertEquals(99L, deleted.commentId());
        verifyNoInteractions(properties, comments);
    }

    @Test void serializedOperationAboveTwoMiBIsRejectedBeforeReservation() {
        eligible();
        var huge = commentOperation("é".repeat(1024 * 1024));
        var error = assertThrows(CustomException.class, () -> service.submit(huge, user));
        assertEquals(413, error.getHttpStatus().value()); assertEquals("OPERATION_LIMIT_EXCEEDED", error.getMessage());
        verifyNoInteractions(repository, comments, tasks);
    }
    @Test void taskPhotosUseTransferIdsWithoutReplayingValueWorkflowsOrTheAttachment() {
        eligible(); writable(); ledger();
        UUID transfer = UUID.randomUUID();
        var photo = new com.grash.model.File(); photo.setId(123L);
        var op = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, 7L,
                MobileOfflineOperation.Type.ADD_TASK_IMAGES, null, null, Instant.now(), null, null, null,
                List.of(transfer), null, null);
        when(files.resolve(List.of(transfer), MobileOfflineFileUpload.Target.TASK, 10, 7L, user))
                .thenReturn(new MobileOfflineFileService.Resolution(List.of(photo), null));
        when(files.visibility(any(), eq(10L), eq(user))).thenReturn(MobileOfflineResult.Visibility.VISIBLE);
        var applied = service.submit(op, user); var replay = service.submit(op, user);
        assertEquals(List.of(123L), applied.fileIds()); assertEquals(applied, replay);
        verify(files, times(1)).attachTaskImages(task, List.of(photo));
        verify(mutations, never()).applyPatch(any(), any(), any());
        verifyNoInteractions(events);
    }

    @Test void anEmptyImageResolutionIsRejectedBeforeAccessOrWorkOrderMutation() {
        eligible(); var records = ledger();
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        when(access.canEdit(10L, user)).thenReturn(true);
        UUID transfer = UUID.randomUUID();
        var op = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.UPDATE_WORK_ORDER_ATTACHMENTS, null, null, Instant.now(), null, null, null,
                null, List.of(), new MobileOfflineOperation.ImageChange(transfer, null));
        when(files.resolve(List.of(), MobileOfflineFileUpload.Target.WORK_ORDER, 10, null, user))
                .thenReturn(new MobileOfflineFileService.Resolution(List.of(), null));
        when(files.resolve(List.of(transfer), MobileOfflineFileUpload.Target.WORK_ORDER, 10, null, user))
                .thenReturn(new MobileOfflineFileService.Resolution(List.of(), null));

        assertEquals("FILE_NOT_FOUND", service.submit(op, user).code());
        assertTrue(records.get(op.operationId()).resultJson().contains("REJECTED"));
        verifyNoInteractions(workOrders, events);
    }

    @Test void aRefusedCommentTransferProducesADurableRefusalWithoutCreatingTheComment() {
        eligible(); var ledger = ledger();
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        UUID transfer = UUID.randomUUID();
        var op = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.CREATE_COMMENT, null, null, Instant.now(), null, "Field comment", null,
                List.of(transfer), null, null);
        when(files.resolve(List.of(transfer), MobileOfflineFileUpload.Target.COMMENT, 10, null, user))
                .thenReturn(new MobileOfflineFileService.Resolution(List.of(), "TRANSFER_REJECTED"));
        assertEquals("TRANSFER_REJECTED", service.submit(op, user).code());
        assertTrue(ledger.get(op.operationId()).resultJson().contains("REJECTED"));
        verifyNoInteractions(comments);
    }

    @Test void fileReferencesDoNotMakeAnEmptyCommentValidOrTriggerAttachmentMutation() {
        eligible(); var records = ledger();
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        var op = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.CREATE_COMMENT, null, null, Instant.now(), null, "<p> </p>", null,
                List.of(UUID.randomUUID()), null, null);
        assertEquals("INVALID_COMMENT_CONTENT", service.submit(op, user).code());
        assertTrue(records.get(op.operationId()).resultJson().contains("REJECTED"));
        verifyNoInteractions(comments, files);
    }

}
