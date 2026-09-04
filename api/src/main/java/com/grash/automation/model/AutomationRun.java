package com.grash.automation.model;

import com.grash.automation.event.EntityType;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One evaluation of one rule against one entity, kept whatever the outcome.
 *
 * <p>The point is the {@link RunStatus#SKIPPED} rows. Without them the engine can only be
 * observed by its effects, and the most common support question — "why did my rule not fire?" —
 * has no answer at all. That was defect D5 of the old engine, which logs nothing.
 *
 * <p>Inherits from {@code CompanyAudit}, so the tenant check on load applies. Its company must
 * be set by hand: {@code CompanyAudit.beforePersist} reads the security context, and these rows
 * are written on the executor thread where there is none. {@code createdAt} from
 * {@code DateAudit} is the run's timestamp; there is no second one.
 */
@Entity
@Table(name = "automation_run")
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Audit record of one rule evaluation")
public class AutomationRun extends CompanyAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AutomationRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 32)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private RunStatus status;

    /**
     * Why a run was skipped, or what went wrong. Free text on purpose: the useful part is
     * "condition asset.cf(42) IS A did not hold", not a code.
     */
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "actions_executed")
    private int actionsExecuted;

    /** The cascade this run belongs to, so a loop is recognisable after the fact. */
    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    private int depth;
}
