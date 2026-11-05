package com.grash.dto.offline;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/** Only the three concrete time operations use this payload; no client-provided total or rate. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileOfflineTime(UUID periodId, Instant startedAt, Instant endedAt,
                                Long laborId, Long laborVersion, UUID timerRunId,
                                Long timerStatusRevision, Boolean preserveStatus,
                                MobileOfflineOperation startOperation, UUID statusPredecessorOperationId) {
    public MobileOfflineTime(UUID periodId, Instant startedAt, Instant endedAt,
                             Long laborId, Long laborVersion, UUID timerRunId, Long timerStatusRevision,
                             Boolean preserveStatus, MobileOfflineOperation startOperation) {
        this(periodId, startedAt, endedAt, laborId, laborVersion, timerRunId, timerStatusRevision,
                preserveStatus, startOperation, null);
    }

    public boolean hasValidIdentity() {
        return periodId != null && startedAt != null
                && (laborId == null || laborId > 0) && (laborVersion == null || laborVersion >= 0)
                && (timerStatusRevision == null || timerStatusRevision >= 0);
    }
}
