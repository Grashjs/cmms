package com.grash.dto.offline;

import com.grash.dto.WorkOrderTimerRun;
import com.grash.model.enums.Status;
import java.time.Instant;
import java.util.List;

public record MobileOfflineStatusResult(Status status, long revision, long chainBaseRevision,
                                        Instant occurredAt, Instant completedOn,
                                        List<WorkOrderTimerRun> stoppedTimerRuns) {}
