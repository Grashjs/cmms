package com.grash.dto.offline;

import com.grash.dto.TaskShowDTO;
import java.time.Instant;
import java.util.UUID;

public record MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task,
                                  Instant receivedAt) {
    public enum Status { APPLIED, CONFLICT, REJECTED }
    public MobileOfflineResult withoutTask() {
        return new MobileOfflineResult(operationId, status, code, null, receivedAt);
    }
}
