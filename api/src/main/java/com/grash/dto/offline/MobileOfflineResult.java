package com.grash.dto.offline;

import com.grash.dto.TaskShowDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

public record MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task,
                                  Instant receivedAt, @JsonInclude(JsonInclude.Include.NON_NULL) Long chainBaseVersion,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Long commentId,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Visibility visibility) {
    public enum Status { APPLIED, CONFLICT, REJECTED }
    public enum Visibility { VISIBLE, COMMENT_DELETED, WORK_ORDER_UNAVAILABLE }

    public MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task, Instant receivedAt) {
        this(operationId, status, code, task, receivedAt, null, null, null);
    }

    public MobileOfflineResult withoutTask() {
        return new MobileOfflineResult(operationId, status, code, null, receivedAt, chainBaseVersion, commentId, visibility);
    }

    public MobileOfflineResult withVisibility(Visibility current) {
        return new MobileOfflineResult(operationId, status, code, task, receivedAt, chainBaseVersion, commentId, current);
    }
}
