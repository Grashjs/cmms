package com.grash.dto.offline;

import com.grash.dto.WorkOrderTimerRun;
import com.grash.model.enums.Status;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileOfflineStatusChange(Status status, Long baseRevision, String feedback, String signature,
                                        List<WorkOrderTimerRun> activeTimerRuns, List<UUID> dependencyOperationIds) {
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isValid() {
        return status != null && baseRevision != null && baseRevision >= 0 && activeTimerRuns != null
                && activeTimerRuns.stream().allMatch(run -> run != null && run.laborId() != null && run.laborId() > 0 && run.timerRunId() != null)
                && activeTimerRuns.stream().map(WorkOrderTimerRun::laborId).distinct().count() == activeTimerRuns.size()
                && dependencyOperationIds != null && dependencyOperationIds.stream().allMatch(java.util.Objects::nonNull)
                && dependencyOperationIds.stream().distinct().count() == dependencyOperationIds.size();
    }
}
