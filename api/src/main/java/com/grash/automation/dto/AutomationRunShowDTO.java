package com.grash.automation.dto;

import com.grash.automation.event.EntityType;
import com.grash.automation.model.AutomationRun;
import com.grash.automation.model.RunStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Schema(description = "One recorded rule evaluation")
public record AutomationRunShowDTO(
        Long id,
        Long ruleId,
        String ruleTitle,
        EntityType entityType,
        Long entityId,
        RunStatus status,
        @Schema(description = "Why it was skipped, or what failed") String detail,
        int actionsExecuted,
        String correlationId,
        int depth,
        Date triggeredAt) {

    public static AutomationRunShowDTO of(AutomationRun run) {
        return new AutomationRunShowDTO(
                run.getId(),
                run.getRule() == null ? null : run.getRule().getId(),
                run.getRule() == null ? null : run.getRule().getTitle(),
                run.getEntityType(),
                run.getEntityId(),
                run.getStatus(),
                run.getDetail(),
                run.getActionsExecuted(),
                run.getCorrelationId(),
                run.getDepth(),
                // There is no separate triggered_at column: a run is written once and never
                // updated, so createdAt from DateAudit is the timestamp.
                run.getCreatedAt());
    }
}
