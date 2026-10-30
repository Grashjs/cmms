package com.grash.dto.offline;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record MobileOfflineOperation(@NotNull UUID operationId, @NotNull @Positive Long companyId,
                                     @NotNull @Positive Long userId, @NotNull @Positive Long workOrderId,
                                     @NotNull @Positive Long taskId, @NotNull Type type,
                                     @NotNull @Min(0) Long baseVersion, @NotNull @Size(max = 255) String value,
                                     @NotNull Instant occurredAt) {
    public enum Type { SET_TASK_VALUE }
}
