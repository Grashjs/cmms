package com.grash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.configuration.MobileOfflineProperties;
import com.grash.dto.TaskPatchDTO;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.Task;
import com.grash.model.User;
import com.grash.model.enums.TaskType;
import com.grash.repository.MobileOfflineRepository;
import com.grash.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static com.grash.dto.offline.MobileOfflineResult.Status.*;

@Service
@RequiredArgsConstructor
public class MobileOfflineService {
    private final MobileOfflineProperties properties;
    private final MobileOfflineRepository repository;
    private final MobileOfflineSnapshotService snapshots;
    private final MobileOfflineAccessService access;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final ObjectMapper json;

    public MobileOfflineEligibility eligibility(User user) {
        Instant now = Instant.now();
        boolean eligible = properties.isEligible(user);
        return new MobileOfflineEligibility(properties.isEnabled(), eligible, user.getCompany().getId(), user.getId(),
                now, eligible ? now.plus(Duration.ofDays(7)) : null, MobileOfflineLimits.PILOT);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public MobileOfflineSnapshot prepare(long workOrderId, UUID deviceId, User user) {
        requireEligible(user);
        long companyId = user.getCompany().getId();
        repository.lockDevice(companyId, user.getId(), deviceId);
        List<Long> prepared = repository.preparedIds(companyId, user.getId(), deviceId);
        if (!prepared.contains(workOrderId) && prepared.size() >= MobileOfflineLimits.PILOT.preparedWorkOrders())
            throw new CustomException("PREPARED_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE);
        MobileOfflineSnapshot snapshot = snapshots.build(workOrderId, user);
        repository.prepare(companyId, user.getId(), deviceId, workOrderId, snapshot.preparedAt());
        return snapshot;
    }

    /** Removing a reservation remains possible after withdrawal of offline eligibility or target deletion. */
    @Transactional
    public void removePreparation(long workOrderId, UUID deviceId, User user) {
        repository.lockDevice(user.getCompany().getId(), user.getId(), deviceId);
        repository.removePreparation(user.getCompany().getId(), user.getId(), deviceId, workOrderId);
    }

    @Transactional
    public MobileOfflineResult submit(MobileOfflineOperation operation, User user) {
        if (!user.getCompany().getId().equals(operation.companyId()) || !user.getId().equals(operation.userId()))
            throw new CustomException("OFFLINE_CONTEXT_MISMATCH", HttpStatus.FORBIDDEN);
        requireEligible(user);
        long company = user.getCompany().getId();
        String hash = hash(operation);
        Instant receivedAt = Instant.now();
        boolean first = repository.reserveOperation(company, user.getId(), operation.operationId(),
                operation.workOrderId(), hash, receivedAt);
        if (!first) {
            var saved = repository.receipt(company, user.getId(), operation.operationId()).orElseThrow();
            if (!saved.payloadHash().equals(hash)) throw new CustomException("OPERATION_ID_REUSED", HttpStatus.CONFLICT);
            return readable(saved, user);
        }

        MobileOfflineResult result = apply(operation, user, receivedAt);
        repository.finishOperation(company, user.getId(), operation.operationId(), encode(result));
        return result;
    }

    private MobileOfflineResult apply(MobileOfflineOperation operation, User user, Instant now) {
        if (!access.existsInCompany(operation.workOrderId(), user))
            return result(operation, REJECTED, "TARGET_NOT_FOUND", null, now);
        if (!access.canView(operation.workOrderId(), user) || !access.canEdit(operation.workOrderId(), user))
            return result(operation, REJECTED, "ACCESS_REVOKED", null, now);
        Optional<Task> found = taskRepository.findForUpdate(operation.taskId(), user.getCompany().getId());
        if (found.isEmpty() || found.get().getWorkOrder() == null
                || !found.get().getWorkOrder().getId().equals(operation.workOrderId()))
            return result(operation, REJECTED, "TARGET_NOT_FOUND", null, now);
        Task task = found.get();
        try {
            taskService.checkEditAccess(task, user, true);
        } catch (CustomException e) {
            return result(operation, REJECTED, "ACCESS_REVOKED", null, now);
        }
        if (task.getTaskBase().getTaskType() != TaskType.TEXT)
            return result(operation, REJECTED, "TYPE_NOT_SUPPORTED", null, now);
        if (task.getVersion() != operation.baseVersion())
            return result(operation, CONFLICT, "VERSION_CONFLICT", task, now);
        TaskPatchDTO patch = new TaskPatchDTO();
        patch.setValue(operation.value());
        patch.setVersion(operation.baseVersion());
        Task saved = taskService.applyPatch(task, patch, user);
        return result(operation, APPLIED, "APPLIED", saved, now);
    }

    private MobileOfflineResult result(MobileOfflineOperation operation, MobileOfflineResult.Status status,
                                       String code, Task task, Instant now) {
        return new MobileOfflineResult(operation.operationId(), status, code,
                task == null ? null : taskMapper.toShowDto(task), now);
    }

    @Transactional(readOnly = true)
    public MobileOfflineReceipts receipts(List<UUID> ids, User user) {
        if (ids == null || ids.isEmpty() || ids.size() > MobileOfflineLimits.PILOT.receiptIds() || ids.stream().anyMatch(Objects::isNull))
            throw new CustomException("RECEIPT_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST);
        List<MobileOfflineResult> results = new ArrayList<>();
        List<UUID> missing = new ArrayList<>();
        for (UUID id : new LinkedHashSet<>(ids)) {
            var receipt = repository.receipt(user.getCompany().getId(), user.getId(), id);
            if (receipt.isPresent()) results.add(readable(receipt.get(), user));
            else missing.add(id);
        }
        return new MobileOfflineReceipts(results, missing);
    }

    private MobileOfflineResult readable(MobileOfflineRepository.Receipt receipt, User user) {
        try {
            MobileOfflineResult result = json.readValue(receipt.resultJson(), MobileOfflineResult.class);
            boolean visible = access.canView(receipt.workOrderId(), user)
                    && (result.task() == null || taskRepository.existsByIdAndCompany_IdAndWorkOrder_Id(
                    result.task().getId(), user.getCompany().getId(), receipt.workOrderId()));
            return visible ? result : result.withoutTask();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid stored offline receipt", e);
        }
    }

    private String hash(MobileOfflineOperation operation) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(encode(operation).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not encode offline operation", e); }
    }

    private void requireEligible(User user) {
        if (!properties.isEligible(user)) throw new CustomException("OFFLINE_NOT_AUTHORIZED", HttpStatus.FORBIDDEN);
    }
}
