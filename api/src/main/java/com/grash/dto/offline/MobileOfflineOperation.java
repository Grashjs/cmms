package com.grash.dto.offline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"operationId", "companyId", "userId", "workOrderId", "taskId", "type", "baseVersion", "value", "occurredAt", "notes", "content", "predecessorOperationId", "fileTransferIds", "addTransferIds", "imageChange"})
public record MobileOfflineOperation(@NotNull UUID operationId, @NotNull @Positive Long companyId,
                                     @NotNull @Positive Long userId, @NotNull @Positive Long workOrderId,
                                     @Positive Long taskId, @NotNull Type type,
                                     @Min(0) Long baseVersion, String value,
                                     @NotNull Instant occurredAt, String notes, String content,
                                     UUID predecessorOperationId, List<UUID> fileTransferIds, List<UUID> addTransferIds, @jakarta.validation.Valid ImageChange imageChange) {
    public enum Type { SET_TASK_VALUE, SET_TASK_NOTES, CREATE_COMMENT, ADD_TASK_IMAGES, UPDATE_WORK_ORDER_ATTACHMENTS }

    public record ImageChange(@NotNull UUID transferId, @Positive Long baseImageId) {}

    public MobileOfflineOperation(UUID operationId, Long companyId, Long userId, Long workOrderId,
                                  Long taskId, Type type, Long baseVersion, String value, Instant occurredAt,
                                  String notes, String content, UUID predecessorOperationId) {
        this(operationId, companyId, userId, workOrderId, taskId, type, baseVersion, value, occurredAt,
                notes, content, predecessorOperationId, null, null, null);
    }

    public MobileOfflineOperation(UUID operationId, Long companyId, Long userId, Long workOrderId,
                                  Long taskId, Type type, Long baseVersion, String value, Instant occurredAt) {
        this(operationId, companyId, userId, workOrderId, taskId, type, baseVersion, value, occurredAt, null, null, null);
    }

    /** Structural errors never reach the ledger; business validation produces a durable receipt. */
    @JsonIgnore
    @AssertTrue(message = "INVALID_OFFLINE_REQUEST")
    public boolean isShapeValid() {
        if (type == null) return false;
        if ((fileTransferIds != null && fileTransferIds.stream().anyMatch(java.util.Objects::isNull))
                || (addTransferIds != null && addTransferIds.stream().anyMatch(java.util.Objects::isNull))) return false;
        boolean noAttachments = fileTransferIds == null && addTransferIds == null && imageChange == null;
        return switch (type) {
            case SET_TASK_VALUE -> taskId != null && baseVersion != null && value != null && notes == null && content == null && noAttachments;
            case SET_TASK_NOTES -> taskId != null && baseVersion != null && notes != null && value == null && content == null && noAttachments;
            case CREATE_COMMENT -> content != null && taskId == null && baseVersion == null && value == null
                    && notes == null && predecessorOperationId == null && addTransferIds == null && imageChange == null;
            case ADD_TASK_IMAGES -> taskId != null && baseVersion == null && value == null && notes == null
                    && content == null && predecessorOperationId == null && fileTransferIds != null
                    && !fileTransferIds.isEmpty() && addTransferIds == null && imageChange == null;
            case UPDATE_WORK_ORDER_ATTACHMENTS -> taskId == null && baseVersion == null && value == null && notes == null
                    && content == null && predecessorOperationId == null && fileTransferIds == null
                    && ((addTransferIds != null && !addTransferIds.isEmpty()) || imageChange != null)
                    && (imageChange == null || imageChange.transferId() != null);
        };
    }
}
