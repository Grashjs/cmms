package com.grash.dto.offline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"operationId", "companyId", "userId", "workOrderId", "taskId", "type", "baseVersion", "value", "occurredAt", "notes", "content", "predecessorOperationId"})
public record MobileOfflineOperation(@NotNull UUID operationId, @NotNull @Positive Long companyId,
                                     @NotNull @Positive Long userId, @NotNull @Positive Long workOrderId,
                                     @Positive Long taskId, @NotNull Type type,
                                     @Min(0) Long baseVersion, String value,
                                     @NotNull Instant occurredAt, String notes, String content,
                                     UUID predecessorOperationId) {
    public enum Type { SET_TASK_VALUE, SET_TASK_NOTES, CREATE_COMMENT }

    public MobileOfflineOperation(UUID operationId, Long companyId, Long userId, Long workOrderId,
                                  Long taskId, Type type, Long baseVersion, String value, Instant occurredAt) {
        this(operationId, companyId, userId, workOrderId, taskId, type, baseVersion, value, occurredAt, null, null, null);
    }

    /** Structural errors never reach the ledger; business validation produces a durable receipt. */
    @JsonIgnore
    @AssertTrue(message = "INVALID_OFFLINE_REQUEST")
    public boolean isShapeValid() {
        if (type == null) return false;
        return switch (type) {
            case SET_TASK_VALUE -> taskId != null && baseVersion != null && value != null && notes == null && content == null;
            case SET_TASK_NOTES -> taskId != null && baseVersion != null && notes != null && value == null && content == null;
            case CREATE_COMMENT -> content != null && taskId == null && baseVersion == null && value == null
                    && notes == null && predecessorOperationId == null;
        };
    }
}
