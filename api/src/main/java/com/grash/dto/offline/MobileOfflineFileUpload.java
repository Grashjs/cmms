package com.grash.dto.offline;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grash.model.enums.FileType;
import java.util.UUID;

/** Metadata is immutable for a transfer UUID. Actual bytes are checked before any object write. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileOfflineFileUpload(UUID operationId, Long companyId, Long userId, Long workOrderId,
                                     Target target, Long taskId, String name, String mimeType, Long size,
                                     String sha256, FileType fileType) {
    public enum Target { WORK_ORDER, TASK, COMMENT }
}
