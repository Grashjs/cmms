package com.grash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.configuration.MobileOfflineProperties;
import com.grash.dto.TaskPatchDTO;
import com.grash.dto.comment.CommentPostDTO;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.Task;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.repository.CommentRepository;
import com.grash.repository.MobileOfflineRepository;
import com.grash.repository.TaskRepository;
import com.grash.utils.TaskPatchValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
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
    private final CommentService comments;
    private final CommentRepository commentRepository;
    private final MobileOfflineFileService files;
    private final WorkOrderService workOrders;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final LaborService labors;
    private final com.grash.repository.LaborRepository laborRepository;

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
    public MobileOfflineResult uploadFile(UUID transferId, MobileOfflineFileUpload transfer,
                                          org.springframework.web.multipart.MultipartFile upload, User user) {
        if (transfer == null || !transferId.equals(transfer.operationId()) || transfer.workOrderId() == null || transfer.workOrderId() <= 0)
            throw new CustomException("INVALID_OFFLINE_REQUEST", HttpStatus.BAD_REQUEST);
        if (!Objects.equals(transfer.companyId(), user.getCompany().getId()) || !Objects.equals(transfer.userId(), user.getId()))
            throw new CustomException("OFFLINE_CONTEXT_MISMATCH", HttpStatus.FORBIDDEN);
        requireEligible(user);
        byte[] bytes;
        try (var input = upload.getInputStream()) {
            bytes = input.readNBytes((int) MobileOfflineFileService.MAX_FILE_BYTES + 1);
        } catch (IOException failure) { throw new CustomException("FILE_READ_FAILED", HttpStatus.SERVICE_UNAVAILABLE); }
        String digest;
        try { digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        // Include actual byte identity even for a rejected malformed upload. No storage write precedes validation.
        String hash = hash(new UploadFingerprint("UPLOAD_FILE", transfer, digest, bytes.length));
        Instant now = Instant.now();
        long company = user.getCompany().getId();
        if (!repository.reserveOperation(company, user.getId(), transferId, transfer.workOrderId(), hash, now)) {
            var previous = repository.receipt(company, user.getId(), transferId).orElseThrow();
            if (!previous.payloadHash().equals(hash)) throw new CustomException("OPERATION_ID_REUSED", HttpStatus.CONFLICT);
            return readable(previous, user);
        }
        String error = files.validationError(transfer, bytes, digest, user);
        if (error == null) error = files.consumeUpload(user);
        var stored = error == null ? files.store(transfer, bytes, user) : null;
        var result = new MobileOfflineResult(transferId, error == null ? APPLIED : REJECTED,
                error == null ? "APPLIED" : error, null, now, null, null, MobileOfflineResult.Visibility.VISIBLE,
                stored == null ? null : stored.getId(), null, null, transfer);
        repository.finishOperation(company, user.getId(), transferId, encode(result));
        return result;
    }

    private record UploadFingerprint(String type, MobileOfflineFileUpload metadata, String actualSha256, int actualSize) {}

    @Transactional(readOnly = true)
    public MobileOfflineFileDownload downloadFile(long workOrderId, long fileId, User user) {
        requireEligible(user);
        return files.download(workOrderId, fileId, user);
    }

    @Transactional
    public MobileOfflineResult submit(MobileOfflineOperation operation, User user) {
        return submit(operation, user, null);
    }

    @Transactional
    public MobileOfflineResult submit(MobileOfflineOperation operation, User user, String platform) {
        if (!operation.isShapeValid()) throw new CustomException("INVALID_OFFLINE_REQUEST", HttpStatus.BAD_REQUEST);
        if (!user.getCompany().getId().equals(operation.companyId()) || !user.getId().equals(operation.userId()))
            throw new CustomException("OFFLINE_CONTEXT_MISMATCH", HttpStatus.FORBIDDEN);
        requireEligible(user);
        if (operation.type() == MobileOfflineOperation.Type.STOP_TIMER && operation.time().startOperation() != null)
            return submitTimerStop(operation, user);
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

        MobileOfflineResult result = apply(operation, user, receivedAt, platform);
        repository.finishOperation(company, user.getId(), operation.operationId(), encode(result));
        return result;
    }

    private MobileOfflineResult apply(MobileOfflineOperation operation, User user, Instant now, String platform) {
        if (!access.existsInCompany(operation.workOrderId(), user))
            return result(operation, REJECTED, "TARGET_NOT_FOUND", null, now);
        if (!access.canView(operation.workOrderId(), user))
            return result(operation, REJECTED, "ACCESS_REVOKED", null, now);
        if (operation.type() == MobileOfflineOperation.Type.CHANGE_WORK_ORDER_STATUS) return applyStatus(operation, user, now, platform);
        if (operation.isTimeOperation()) return applyTime(operation, user, now, null, false);
        if (operation.type() == MobileOfflineOperation.Type.CREATE_COMMENT) return createComment(operation, user, now);
        if (!access.canEdit(operation.workOrderId(), user))
            return result(operation, REJECTED, "ACCESS_REVOKED", null, now);
        if (operation.type() == MobileOfflineOperation.Type.UPDATE_WORK_ORDER_ATTACHMENTS)
            return updateAttachments(operation, user, now);
        Optional<Task> found = taskRepository.findForUpdate(operation.taskId(), user.getCompany().getId());
        if (found.isEmpty() || found.get().getWorkOrder() == null
                || !found.get().getWorkOrder().getId().equals(operation.workOrderId()))
            return result(operation, REJECTED, "TARGET_NOT_FOUND", null, now);
        Task task = found.get();
        try {
            taskService.checkEditAccess(task, user, operation.type() == MobileOfflineOperation.Type.SET_TASK_VALUE);
        } catch (CustomException e) {
            return result(operation, REJECTED, "ACCESS_REVOKED", null, now);
        }
        if (operation.type() == MobileOfflineOperation.Type.ADD_TASK_IMAGES) {
            var resolved = files.resolve(operation.fileTransferIds(), MobileOfflineFileUpload.Target.TASK,
                    operation.workOrderId(), operation.taskId(), user);
            if (resolved.error() != null) return result(operation, REJECTED, resolved.error(), null, now);
            files.attachTaskImages(task, resolved.files());
            // Like connected task-photo uploads, this association does not replay TASK_UPDATED value workflows.
            return new MobileOfflineResult(operation.operationId(), APPLIED, "APPLIED", null, now,
                    null, null, MobileOfflineResult.Visibility.VISIBLE, null,
                    resolved.files().stream().map(com.grash.model.File::getId).toList(), null, null);
        }
        long expectedVersion = operation.baseVersion();
        if (operation.predecessorOperationId() != null) {
            if (operation.predecessorOperationId().equals(operation.operationId()))
                return result(operation, REJECTED, "INVALID_PREDECESSOR", null, now);
            var predecessor = repository.receipt(user.getCompany().getId(), user.getId(), operation.predecessorOperationId());
            if (predecessor.isEmpty() || predecessor.get().resultJson() == null
                    || predecessor.get().workOrderId() != operation.workOrderId())
                return result(operation, REJECTED, "INVALID_PREDECESSOR", null, now);
            MobileOfflineResult previous = decode(predecessor.get());
            if (previous.status() != APPLIED || previous.task() == null || previous.commentId() != null
                    || !previous.task().getId().equals(operation.taskId())
                    || !Objects.equals(previous.chainBaseVersion(), operation.baseVersion()))
                return result(operation, REJECTED, "INVALID_PREDECESSOR", null, now);
            expectedVersion = previous.task().getVersion();
        }
        if (task.getVersion() != expectedVersion)
            return result(operation, CONFLICT, "VERSION_CONFLICT", task, now);
        TaskPatchDTO patch = new TaskPatchDTO();
        if (operation.type() == MobileOfflineOperation.Type.SET_TASK_VALUE) patch.setValue(operation.value());
        else patch.setNotes(operation.notes());
        patch.setVersion(expectedVersion);
        // Validate before entering the shared transactional mutation: rejected input still gets a committed receipt.
        String validationError = TaskPatchValidation.error(task, patch);
        if (validationError != null) return result(operation, REJECTED, validationError, task, now);
        Task saved = taskService.applyPatch(task, patch, user);
        return result(operation, APPLIED, "APPLIED", saved, now);
    }

    private MobileOfflineResult applyStatus(MobileOfflineOperation operation, User user, Instant now, String platform) {
        var change = operation.statusChange();
        if (operation.occurredAt().isAfter(now.plusSeconds(120)))
            return result(operation, REJECTED, "INVALID_STATUS_DATE", null, now);
        long revision = change.baseRevision();
        Set<com.grash.dto.WorkOrderTimerRun> expectedRuns = new HashSet<>(change.activeTimerRuns());
        if (operation.predecessorOperationId() != null) {
            var previous = acceptedDependency(operation.predecessorOperationId(), operation, user);
            if (previous == null || previous.statusResult() == null
                    || previous.statusResult().chainBaseRevision() != change.baseRevision())
                return result(operation, REJECTED, "INVALID_STATUS_PREDECESSOR", null, now);
            revision = previous.statusResult().revision();
            if (previous.statusResult().stoppedTimerRuns() != null) expectedRuns.removeAll(previous.statusResult().stoppedTimerRuns());
        }
        for (UUID dependencyId : change.dependencyOperationIds()) {
            var dependency = acceptedDependency(dependencyId, operation, user);
            if (dependency == null) return result(operation, REJECTED, "STATUS_DEPENDENCY_NOT_APPLIED", null, now);
            if (change.status() != com.grash.model.enums.Status.COMPLETE
                    && !"TIMER_STOPPED".equals(dependency.code()) && !"TIMER_PERIOD_RECORDED".equals(dependency.code()))
                return result(operation, REJECTED, "INVALID_STATUS_DEPENDENCY", null, now);
            if (dependency.timeResult() != null && "TIMER_STOPPED".equals(dependency.code())
                    && dependency.timeResult().timerRunId() != null) {
                expectedRuns.remove(new com.grash.dto.WorkOrderTimerRun(dependency.timeResult().laborId(), dependency.timeResult().timerRunId()));
            }
            if (dependency.statusResult() != null && dependency.statusResult().stoppedTimerRuns() != null)
                expectedRuns.removeAll(dependency.statusResult().stoppedTimerRuns());
        }
        var outcome = workOrders.applyOfflineStatus(operation.workOrderId(), change, revision,
                List.copyOf(expectedRuns), user, platform, operation.operationId());
        if (outcome.error() != null) return result(operation, outcome.conflict() ? CONFLICT : REJECTED, outcome.error(), null, now);
        WorkOrder saved = outcome.workOrder();
        var details = new MobileOfflineStatusResult(saved.getStatus(), saved.getTimerStatusRevision(), change.baseRevision(),
                operation.occurredAt(), saved.getCompletedOn() == null ? null : saved.getCompletedOn().toInstant(), outcome.stoppedTimerRuns());
        return new MobileOfflineResult(operation.operationId(), APPLIED, "STATUS_CHANGED", null, now,
                null, null, MobileOfflineResult.Visibility.VISIBLE, null, null, null, null, null, null, details);
    }

    private MobileOfflineResult acceptedDependency(UUID dependencyId, MobileOfflineOperation operation, User user) {
        if (dependencyId == null || dependencyId.equals(operation.operationId())) return null;
        var receipt = repository.receipt(user.getCompany().getId(), user.getId(), dependencyId);
        if (receipt.isEmpty() || receipt.get().resultJson() == null || receipt.get().workOrderId() != operation.workOrderId()) return null;
        var result = decode(receipt.get());
        return result.status() == APPLIED ? result : null;
    }

    /** Resolve a closed local period before retrying its start in isolation. Ledger order is always start -> stop -> target. */
    private MobileOfflineResult submitTimerStop(MobileOfflineOperation stop, User user) {
        var start = stop.time().startOperation();
        long company = user.getCompany().getId();
        Instant now = Instant.now();
        String startHash = hash(start);
        boolean newStart = repository.reserveOperation(company, user.getId(), start.operationId(), start.workOrderId(), startHash, now);
        MobileOfflineResult startResult = null;
        if (!newStart) {
            var saved = repository.receipt(company, user.getId(), start.operationId()).orElseThrow();
            if (!saved.payloadHash().equals(startHash)) throw new CustomException("OPERATION_ID_REUSED", HttpStatus.CONFLICT);
            startResult = decode(saved);
        }
        String stopHash = hash(stop);
        boolean newStop = repository.reserveOperation(company, user.getId(), stop.operationId(), stop.workOrderId(), stopHash, now);
        if (!newStop) {
            // This state cannot arise from a committed causal stop. Never commit an empty start reservation.
            if (newStart) throw new IllegalStateException("Timer stop receipt has no start receipt");
            var saved = repository.receipt(company, user.getId(), stop.operationId()).orElseThrow();
            if (!saved.payloadHash().equals(stopHash)) throw new CustomException("OPERATION_ID_REUSED", HttpStatus.CONFLICT);
            return readable(saved, user);
        }
        MobileOfflineResult stopped;
        if (!newStart && startResult.status() != APPLIED) {
            stopped = result(stop, REJECTED, "TIMER_START_NOT_APPLIED", null, now);
        } else if (!access.existsInCompany(stop.workOrderId(), user)) {
            stopped = result(stop, REJECTED, "TARGET_NOT_FOUND", null, now);
        } else if (!access.canView(stop.workOrderId(), user)) {
            stopped = result(stop, REJECTED, "ACCESS_REVOKED", null, now);
        } else {
            stopped = applyTime(stop, user, now, startResult, newStart);
        }
        if (newStart) {
            // A late original START sees this terminal receipt, even if the local closure was refused.
            startResult = new MobileOfflineResult(start.operationId(), stopped.status(),
                    stopped.status() == APPLIED ? "TIMER_START_FINALIZED" : "TIMER_START_CLOSED", null, now,
                    null, null, stopped.visibility(), null, null, null, null, stopped.timeResult(), null);
            repository.finishOperation(company, user.getId(), start.operationId(), encode(startResult));
        }
        stopped = stopped.withStartReceipt(startResult);
        repository.finishOperation(company, user.getId(), stop.operationId(), encode(stopped));
        // The causal start remains useful for local recovery, but its historical
        // time details must not bypass a newly withdrawn work-order permission.
        return access.canView(stop.workOrderId(), user) ? stopped : stopped.withoutTimeDetails();
    }

    private MobileOfflineResult applyTime(MobileOfflineOperation operation, User user, Instant now,
                                          MobileOfflineResult acceptedStart, boolean closeWithoutStart) {
        MobileOfflineOperation basis = closeWithoutStart ? operation.time().startOperation() : operation;
        MobileOfflineResult predecessor = null;
        if (acceptedStart == null && basis.predecessorOperationId() != null) {
            if (basis.predecessorOperationId().equals(basis.operationId()))
                return result(operation, REJECTED, "INVALID_PREDECESSOR", null, now);
            var saved = repository.receipt(user.getCompany().getId(), user.getId(), basis.predecessorOperationId());
            if (saved.isEmpty() || saved.get().resultJson() == null || saved.get().workOrderId() != operation.workOrderId())
                return result(operation, REJECTED, "INVALID_PREDECESSOR", null, now);
            predecessor = decode(saved.get());
        }
        MobileOfflineStatusResult statusPredecessor = null;
        UUID statusId = basis.time().statusPredecessorOperationId();
        if (statusId != null) {
            var previousStatus = acceptedDependency(statusId, operation, user);
            if (previousStatus == null || previousStatus.statusResult() == null
                    || !Objects.equals(basis.time().timerStatusRevision(), previousStatus.statusResult().chainBaseRevision()))
                return result(operation, REJECTED, "INVALID_STATUS_PREDECESSOR", null, now);
            statusPredecessor = previousStatus.statusResult();
        }
        var outcome = labors.applyOfflineTime(operation, user, predecessor, acceptedStart, closeWithoutStart, now, statusPredecessor);
        if (outcome.error() != null)
            return result(operation, outcome.conflict() ? CONFLICT : REJECTED, outcome.error(), null, now);
        var labor = outcome.labor();
        String code = operation.type() == MobileOfflineOperation.Type.START_TIMER ? "TIMER_STARTED"
                : operation.type() == MobileOfflineOperation.Type.CREATE_MANUAL_TIME ? "MANUAL_TIME_CREATED"
                : closeWithoutStart ? "TIMER_PERIOD_RECORDED" : "TIMER_STOPPED";
        var details = new MobileOfflineTimeResult(operation.time().periodId(), labor.getId(), labor.getVersion(),
                closeWithoutStart || operation.type() == MobileOfflineOperation.Type.CREATE_MANUAL_TIME ? null : labor.getTimerRunId(),
                operation.time().startedAt(), operation.time().endedAt(), outcome.durationAdded(), outcome.closedWithoutActivation());
        return new MobileOfflineResult(operation.operationId(), APPLIED, code, null, now, null, null,
                MobileOfflineResult.Visibility.VISIBLE, null, null, null, null, details, null);
    }

    private MobileOfflineResult updateAttachments(MobileOfflineOperation operation, User user, Instant now) {
        var additions = files.resolve(operation.addTransferIds(), MobileOfflineFileUpload.Target.WORK_ORDER,
                operation.workOrderId(), null, user);
        if (additions.error() != null) return result(operation, REJECTED, additions.error(), null, now);
        com.grash.model.File image = null;
        if (operation.imageChange() != null) {
            var resolved = files.resolve(List.of(operation.imageChange().transferId()), MobileOfflineFileUpload.Target.WORK_ORDER,
                    operation.workOrderId(), null, user);
            if (resolved.error() != null) return result(operation, REJECTED, resolved.error(), null, now);
            if (resolved.files().isEmpty()) return result(operation, REJECTED, "FILE_NOT_FOUND", null, now);
            image = resolved.files().get(0);
        }
        var outcome = workOrders.applyOfflineAttachments(operation.workOrderId(), additions.files(), image,
                operation.imageChange() == null ? null : operation.imageChange().baseImageId(), operation.imageChange() != null, user);
        if (outcome.error() != null) return result(operation, outcome.conflict() ? CONFLICT : REJECTED, outcome.error(), null, now);
        if (operation.imageChange() != null)
            events.publishEvent(new WorkOrderService.OfflineAttachmentsChanged(operation.workOrderId(), user.getCompany().getId()));
        return new MobileOfflineResult(operation.operationId(), APPLIED, "APPLIED", null, now, null, null,
                MobileOfflineResult.Visibility.VISIBLE, null,
                outcome.workOrder().getFiles().stream().map(com.grash.model.File::getId).toList(),
                outcome.workOrder().getImage() == null ? null : outcome.workOrder().getImage().getId(), null);
    }

    private MobileOfflineResult createComment(MobileOfflineOperation operation, User user, Instant now) {
        String content;
        try {
            content = CommentService.cleanOfflineContent(operation.content());
        } catch (CustomException invalid) {
            return result(operation, REJECTED, "INVALID_COMMENT_CONTENT", null, now);
        }
        var attachments = operation.fileTransferIds() == null || operation.fileTransferIds().isEmpty()
                ? new MobileOfflineFileService.Resolution(List.of(), null)
                : files.resolve(operation.fileTransferIds(), MobileOfflineFileUpload.Target.COMMENT, operation.workOrderId(), null, user);
        if (attachments.error() != null) return result(operation, REJECTED, attachments.error(), null, now);
        CommentPostDTO request = new CommentPostDTO();
        WorkOrder target = new WorkOrder(); target.setId(operation.workOrderId());
        request.setWorkOrder(target);
        request.setContent(content);
        request.setFiles(new ArrayList<>(attachments.files()));
        var saved = comments.createOffline(request, user, operation.operationId()).getFirst();
        return new MobileOfflineResult(operation.operationId(), APPLIED, "APPLIED", null, now,
                null, saved.getId(), MobileOfflineResult.Visibility.VISIBLE);
    }

    private MobileOfflineResult result(MobileOfflineOperation operation, MobileOfflineResult.Status status,
                                       String code, Task task, Instant now) {
        return new MobileOfflineResult(operation.operationId(), status, code,
                task == null ? null : taskMapper.toShowDto(task), now,
                status == APPLIED && task != null ? operation.baseVersion() : null, null, null);
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
        MobileOfflineResult result = decode(receipt);
        boolean workOrderVisible = access.canView(receipt.workOrderId(), user);
        if (result.statusResult() != null) return workOrderVisible ? result : result.withoutStatusDetails();
        if (result.timeResult() != null || result.startReceipt() != null) {
            if (!workOrderVisible) return result.withoutTimeDetails();
            boolean exists = result.timeResult() == null || laborRepository.existsByIdAndCompany_IdAndWorkOrder_Id(
                    result.timeResult().laborId(), user.getCompany().getId(), receipt.workOrderId());
            var visible = result.withVisibility(exists ? MobileOfflineResult.Visibility.VISIBLE : MobileOfflineResult.Visibility.LABOR_DELETED);
            if (result.startReceipt() != null) visible = visible.withStartReceipt(result.startReceipt().withVisibility(visible.visibility()));
            return visible;
        }
        if (result.transfer() != null || result.fileIds() != null) {
            return result.withVisibility(!workOrderVisible ? MobileOfflineResult.Visibility.WORK_ORDER_UNAVAILABLE
                    : files.visibility(result, receipt.workOrderId(), user));
        }
        if (result.commentId() != null) {
            var visibility = !workOrderVisible ? MobileOfflineResult.Visibility.WORK_ORDER_UNAVAILABLE
                    : commentRepository.existsByIdAndCompany_IdAndWorkOrder_Id(result.commentId(), user.getCompany().getId(), receipt.workOrderId())
                    ? MobileOfflineResult.Visibility.VISIBLE : MobileOfflineResult.Visibility.COMMENT_DELETED;
            return result.withVisibility(visibility);
        }
        boolean visible = workOrderVisible && (result.task() == null || taskRepository.existsByIdAndCompany_IdAndWorkOrder_Id(
                result.task().getId(), user.getCompany().getId(), receipt.workOrderId()));
        return visible ? result : result.withoutTask();
    }

    private MobileOfflineResult decode(MobileOfflineRepository.Receipt receipt) {
        try {
            return json.readValue(receipt.resultJson(), MobileOfflineResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid stored offline receipt", e);
        }
    }

    private String hash(Object operation) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Bound Jackson's exact UTF-8 representation before reserving an operation; never truncate content.
            json.writeValue(new OutputStream() {
                private long bytes;
                private void count(int length) throws IOException {
                    bytes += length;
                    if (bytes > MobileOfflineLimits.PILOT.snapshotBytes()) throw new OperationSizeException();
                }
                @Override public void write(int b) throws IOException { count(1); digest.update((byte) b); }
                @Override public void write(byte[] b, int off, int len) throws IOException { count(len); digest.update(b, off, len); }
            }, operation);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        catch (IOException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof OperationSizeException)
                    throw new CustomException("OPERATION_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE);
            }
            throw new IllegalStateException("Could not encode offline operation", e);
        }
    }

    private static class OperationSizeException extends IOException {}

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not encode offline operation", e); }
    }

    private void requireEligible(User user) {
        if (!properties.isEligible(user)) throw new CustomException("OFFLINE_NOT_AUTHORIZED", HttpStatus.FORBIDDEN);
    }
}
