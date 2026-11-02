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
        if (!operation.isShapeValid()) throw new CustomException("INVALID_OFFLINE_REQUEST", HttpStatus.BAD_REQUEST);
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
        if (!access.canView(operation.workOrderId(), user))
            return result(operation, REJECTED, "ACCESS_REVOKED", null, now);
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
