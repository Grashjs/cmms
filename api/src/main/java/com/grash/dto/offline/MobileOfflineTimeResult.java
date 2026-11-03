package com.grash.dto.offline;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileOfflineTimeResult(UUID periodId, Long laborId, Long laborVersion,
                                      UUID timerRunId, Instant startedAt, Instant endedAt,
                                      long durationAdded, boolean closedWithoutActivation) {}
