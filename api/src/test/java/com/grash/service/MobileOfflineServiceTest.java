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
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    MobileOfflineService service;
    User user;
    Task task;

    @BeforeEach void setup() {
        service = new MobileOfflineService(properties, repository, snapshots, access, tasks, mutations, mapper, json);
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
}
