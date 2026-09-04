package com.grash.automation.model;

import com.grash.model.abstracts.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One action of a rule, with its parameters as JSON.
 *
 * <p>JSON rather than typed columns because a generic model with typed parameter columns grows a
 * column per action — the old {@code WorkflowAction} has seventeen, most of them null on any
 * given row, and it still cannot express "a work order for <em>this</em> asset". The trade is
 * that the shape is not enforced by the schema, so it is validated against the handler's
 * descriptor when a rule is saved.
 */
@Entity
@Table(name = "automation_action_step")
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "One action of an automation rule")
public class AutomationActionStep extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AutomationRule rule;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 48)
    private ActionType actionType;

    /**
     * Parameters as a JSON object. Values may contain {@code ${trigger.…}} placeholders, which
     * are resolved against the triggering entity — that is the runtime-value capability the old
     * engine lacks, and the reason the AI triage cannot use it today.
     */
    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "order_index")
    private int orderIndex;

    /**
     * True stops the rule when this step fails, false carries on with the next one. Defaults to
     * stopping: a rule whose first action was meant to create the work order the second one
     * notifies about should not send the notification.
     */
    @Column(name = "abort_on_failure")
    private boolean abortOnFailure = true;
}
