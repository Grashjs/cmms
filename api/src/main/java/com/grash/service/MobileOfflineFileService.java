package com.grash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.File;
import com.grash.model.User;
import com.grash.model.enums.FileType;
import com.grash.repository.FileRepository;
import com.grash.repository.MobileOfflineRepository;
import com.grash.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

/** Binary transfer and target binding; the existing operation ledger owns its transaction and identity. */
@Service
@RequiredArgsConstructor
public class MobileOfflineFileService {
    public static final long MAX_FILE_BYTES = 35L * 1024 * 1024;
    private final FileService files;
    private final FileRepository fileRepository;
    private final StorageServiceFactory storage;
    private final MobileOfflineAccessService access;
    private final TaskRepository tasks;
    private final TaskService taskService;
    private final MobileOfflineRepository ledger;
    private final ObjectMapper json;
    private final EntityManager em;

    public String validationError(MobileOfflineFileUpload transfer, byte[] bytes, String digest, User user) {
        if (transfer.target() == null || transfer.fileType() == null || transfer.name() == null
                || transfer.name().isBlank() || transfer.name().length() > 255 || transfer.mimeType() == null
                || transfer.mimeType().isBlank() || transfer.mimeType().length() > 255
                || transfer.size() == null || transfer.size() <= 0 || transfer.size() > MAX_FILE_BYTES
                || bytes.length == 0 || bytes.length > MAX_FILE_BYTES || transfer.size() != bytes.length
                || transfer.sha256() == null || !transfer.sha256().matches("[0-9a-f]{64}")
                || !digest.equals(transfer.sha256())
                || (transfer.fileType() == FileType.IMAGE && !transfer.mimeType().startsWith("image/"))) return "INVALID_FILE_CONTENT";
        if (transfer.target() == MobileOfflineFileUpload.Target.TASK
                ? transfer.taskId() == null || transfer.taskId() <= 0 || transfer.fileType() != FileType.IMAGE
                    || !transfer.mimeType().startsWith("image/")
                : transfer.taskId() != null) return "INVALID_FILE_TARGET";
        if (!access.existsInCompany(transfer.workOrderId(), user)) return "TARGET_NOT_FOUND";
        if (!access.canView(transfer.workOrderId(), user)
                || (transfer.target() != MobileOfflineFileUpload.Target.COMMENT
                && !access.canEdit(transfer.workOrderId(), user))) return "ACCESS_REVOKED";
        try {
            files.checkUploadAccess(user, false);
            if (transfer.target() == MobileOfflineFileUpload.Target.TASK) {
                var task = tasks.findForUpdate(transfer.taskId(), user.getCompany().getId());
                if (task.isEmpty() || task.get().getWorkOrder() == null
                        || !Objects.equals(task.get().getWorkOrder().getId(), transfer.workOrderId())) return "TARGET_NOT_FOUND";
                taskService.checkEditAccess(task.get(), user, false);
            }
        } catch (CustomException denied) { return "FILE_UPLOAD_FORBIDDEN"; }
        return null;
    }

    public String consumeUpload(User user) {
        try { files.consumeUpload(user, false); return null; }
        catch (CustomException limited) { return "FILE_UPLOAD_RATE_LIMIT"; }
    }

    public File store(MobileOfflineFileUpload transfer, byte[] bytes, User user) {
        // A rolled-back SQL transaction may leave an object. Its content-addressed key cannot overwrite
        // a different payload subsequently using the same UUID. Object storage is not part of the SQL transaction.
        String path = "company " + user.getCompany().getId() + "/mobile-offline/" + user.getId() + "/"
                + transfer.operationId() + "/" + transfer.sha256();
        storage.getStorageService().uploadAt(bytes, path, transfer.mimeType());
        File file = new File(transfer.name(), path, transfer.fileType(), null, true);
        file.setCompany(user.getCompany());
        return fileRepository.saveAndFlush(file);
    }

    public record Resolution(List<File> files, String error) {}

    /** Resolve only this user's successful uploads for the exact intervention and attachment target. */
    public Resolution resolve(List<UUID> ids, MobileOfflineFileUpload.Target target, long workOrderId,
                              Long taskId, User user) {
        if (ids == null || ids.isEmpty()) return new Resolution(List.of(), null);
        try { files.checkUploadAccess(user, false); }
        catch (CustomException denied) { return new Resolution(List.of(), "FILE_UPLOAD_FORBIDDEN"); }
        List<File> resolved = new ArrayList<>();
        for (UUID id : new LinkedHashSet<>(ids)) {
            var receipt = ledger.receipt(user.getCompany().getId(), user.getId(), id);
            if (receipt.isEmpty() || receipt.get().resultJson() == null) return new Resolution(List.of(), "TRANSFER_NOT_CONFIRMED");
            MobileOfflineResult result;
            try { result = json.readValue(receipt.get().resultJson(), MobileOfflineResult.class); }
            catch (JsonProcessingException malformed) { throw new IllegalStateException("Invalid stored offline receipt", malformed); }
            var transfer = result.transfer();
            if (result.status() != MobileOfflineResult.Status.APPLIED || result.fileId() == null || transfer == null)
                return new Resolution(List.of(), "TRANSFER_REJECTED");
            if (!Objects.equals(transfer.operationId(), id) || !Objects.equals(transfer.companyId(), user.getCompany().getId())
                    || !Objects.equals(transfer.userId(), user.getId()) || !Objects.equals(transfer.workOrderId(), workOrderId)
                    || transfer.target() != target || !Objects.equals(transfer.taskId(), taskId))
                return new Resolution(List.of(), "INVALID_FILE_TARGET");
            var file = fileRepository.findByIdAndCompany_Id(result.fileId(), user.getCompany().getId());
            if (file.isEmpty()) return new Resolution(List.of(), "FILE_NOT_FOUND");
            if (target == MobileOfflineFileUpload.Target.TASK && file.get().getType() != FileType.IMAGE)
                return new Resolution(List.of(), "INVALID_FILE_TARGET");
            resolved.add(file.get());
        }
        // Lock in stable order: two comments must not race to acquire the same one-to-many attachment.
        resolved.stream().sorted(Comparator.comparing(File::getId)).forEach(file -> em.lock(file, LockModeType.PESSIMISTIC_WRITE));
        if (target == MobileOfflineFileUpload.Target.COMMENT) {
            for (File file : resolved) {
                if (em.createQuery("select count(c) from Comment c join c.files f where f.id = :id", Long.class)
                        .setParameter("id", file.getId()).getSingleResult() > 0)
                    return new Resolution(List.of(), "FILE_ALREADY_ATTACHED");
            }
        }
        return new Resolution(resolved, null);
    }

    public void attachTaskImages(com.grash.model.Task task, List<File> images) {
        images.forEach(file -> file.setTask(task));
        fileRepository.saveAll(images);
        fileRepository.flush();
        em.refresh(task);
    }

    public MobileOfflineResult.Visibility visibility(MobileOfflineResult result, long workOrderId, User user) {
        if (result.fileId() != null && fileRepository.findByIdAndCompany_Id(result.fileId(), user.getCompany().getId()).isEmpty())
            return MobileOfflineResult.Visibility.FILE_DELETED;
        if (result.transfer() != null && result.transfer().target() == MobileOfflineFileUpload.Target.TASK
                && !tasks.existsByIdAndCompany_IdAndWorkOrder_Id(result.transfer().taskId(), user.getCompany().getId(),
                result.transfer().workOrderId())) return MobileOfflineResult.Visibility.TARGET_UNAVAILABLE;
        if (result.fileIds() != null) {
            Set<Long> expected = new HashSet<>(result.fileIds());
            if (result.imageId() != null) expected.add(result.imageId());
            if (!expected.isEmpty()) {
                long existing = em.createQuery("select count(f) from File f where f.id in :ids and f.company.id = :company", Long.class)
                        .setParameter("ids", expected).setParameter("company", user.getCompany().getId()).getSingleResult();
                if (existing != expected.size()) return MobileOfflineResult.Visibility.FILE_DELETED;
                Set<Long> attached = new HashSet<>();
                String[] associations = {
                        "select w.image.id from WorkOrder w where w.id = :id and w.image.id in :files",
                        "select f.id from WorkOrder w join w.files f where w.id = :id and f.id in :files",
                        "select f.id from Task t join t.images f where t.workOrder.id = :id and f.id in :files"
                };
                for (String query : associations) attached.addAll(em.createQuery(query, Long.class)
                        .setParameter("id", workOrderId).setParameter("files", expected).getResultList());
                if (!attached.containsAll(expected)) return MobileOfflineResult.Visibility.TARGET_UNAVAILABLE;
            }
        }
        return MobileOfflineResult.Visibility.VISIBLE;
    }

    public MobileOfflineFileDownload download(long workOrderId, long fileId, User user) {
        if (!access.canView(workOrderId, user)) throw new CustomException("ACCESS_REVOKED", HttpStatus.FORBIDDEN);
        var file = fileRepository.findByIdAndCompany_Id(fileId, user.getCompany().getId())
                .orElseThrow(() -> new CustomException("FILE_NOT_FOUND", HttpStatus.NOT_FOUND));
        String[] associations = {
                "select count(w) from WorkOrder w where w.id = :workOrder and w.image.id = :file",
                "select count(w) from WorkOrder w join w.files f where w.id = :workOrder and f.id = :file",
                "select count(t) from Task t join t.images f where t.workOrder.id = :workOrder and f.id = :file",
                "select count(c) from Comment c join c.files f where c.workOrder.id = :workOrder and f.id = :file"
        };
        boolean attached = Arrays.stream(associations).anyMatch(query -> em.createQuery(query, Long.class)
                .setParameter("workOrder", workOrderId).setParameter("file", fileId).getSingleResult() > 0);
        if (!attached) throw new CustomException("FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
        StorageService backend = storage.getStorageService();
        var metadata = backend.stat(file.getPath());
        if (metadata.size() > MAX_FILE_BYTES) throw new CustomException("FILE_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE);
        return new MobileOfflineFileDownload(fileId, file.getName(), metadata.contentType() == null ? "application/octet-stream" : metadata.contentType(), metadata.size(),
                backend.generateSignedUrl(file, 10));
    }
}
