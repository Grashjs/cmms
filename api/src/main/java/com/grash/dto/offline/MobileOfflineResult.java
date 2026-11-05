package com.grash.dto.offline;

import com.grash.dto.TaskShowDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task,
                                  Instant receivedAt, @JsonInclude(JsonInclude.Include.NON_NULL) Long chainBaseVersion,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Long commentId,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Visibility visibility,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Long fileId,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) List<Long> fileIds,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) Long imageId,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) MobileOfflineFileUpload transfer,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) MobileOfflineTimeResult timeResult,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) MobileOfflineResult startReceipt,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) MobileOfflineStatusResult statusResult) {
    public enum Status { APPLIED, CONFLICT, REJECTED }
    public enum Visibility { VISIBLE, COMMENT_DELETED, WORK_ORDER_UNAVAILABLE, FILE_DELETED, TARGET_UNAVAILABLE, LABOR_DELETED }

    public MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task, Instant receivedAt,
                               Long chainBaseVersion, Long commentId, Visibility visibility, Long fileId,
                               List<Long> fileIds, Long imageId, MobileOfflineFileUpload transfer,
                               MobileOfflineTimeResult timeResult, MobileOfflineResult startReceipt) {
        this(operationId, status, code, task, receivedAt, chainBaseVersion, commentId, visibility,
                fileId, fileIds, imageId, transfer, timeResult, startReceipt, null);
    }

    public MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task, Instant receivedAt,
                               Long chainBaseVersion, Long commentId, Visibility visibility, Long fileId,
                               List<Long> fileIds, Long imageId, MobileOfflineFileUpload transfer) {
        this(operationId, status, code, task, receivedAt, chainBaseVersion, commentId, visibility,
                fileId, fileIds, imageId, transfer, null, null);
    }

    public MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task, Instant receivedAt,
                               Long chainBaseVersion, Long commentId, Visibility visibility) {
        this(operationId, status, code, task, receivedAt, chainBaseVersion, commentId, visibility, null, null, null, null);
    }

    public MobileOfflineResult(UUID operationId, Status status, String code, TaskShowDTO task, Instant receivedAt) {
        this(operationId, status, code, task, receivedAt, null, null, null);
    }

    public MobileOfflineResult withoutStatusDetails() {
        return new MobileOfflineResult(operationId, status, code, null, receivedAt, chainBaseVersion, commentId,
                Visibility.WORK_ORDER_UNAVAILABLE, fileId, fileIds, imageId, transfer, timeResult, startReceipt, null);
    }

    public MobileOfflineResult withoutTask() {
        return new MobileOfflineResult(operationId, status, code, null, receivedAt, chainBaseVersion, commentId, visibility, fileId, fileIds, imageId, transfer, timeResult, startReceipt, statusResult);
    }

    public MobileOfflineResult withVisibility(Visibility current) {
        return new MobileOfflineResult(operationId, status, code, task, receivedAt, chainBaseVersion, commentId, current, fileId, fileIds, imageId, transfer, timeResult, startReceipt, statusResult);
    }

    public MobileOfflineResult withStartReceipt(MobileOfflineResult start) {
        return new MobileOfflineResult(operationId, status, code, task, receivedAt, chainBaseVersion, commentId,
                visibility, fileId, fileIds, imageId, transfer, timeResult, start, statusResult);
    }

    public MobileOfflineResult withoutTimeDetails() {
        return new MobileOfflineResult(operationId, status, code, task, receivedAt, chainBaseVersion, commentId,
                Visibility.WORK_ORDER_UNAVAILABLE, fileId, fileIds, imageId, transfer, null,
                startReceipt == null ? null : startReceipt.withoutTimeDetails(), null);
    }
}
