package com.grash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** Identity of a running clock the user has actually seen; never a client-provided duration. */
public record WorkOrderTimerRun(@NotNull @Positive Long laborId, @NotNull UUID timerRunId) {}
