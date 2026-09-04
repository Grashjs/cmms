package com.grash.automation.model;

import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One automation rule: a trigger, conditions that all have to hold, and actions in order.
 *
 * <p>Lives beside the old {@code Workflow} rather than replacing it. That is a deliberate
 * trade for a fork that merges upstream monthly — see {@code docs/workflow-engine-konzept.md}
 * section 4.1. Nothing here touches the old tables.
 */
@Entity
@Table(name = "automation_rule")
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "An automation rule: trigger, conditions and ordered actions")
public class AutomationRule extends CompanyAudit {

    @NotNull
    @Schema(description = "Name of the rule, shown in the rule list and in the run log")
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_change_type", length = 32)
    private ChangeType triggerChangeType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_entity_type", length = 32)
    private EntityType triggerEntityType;

    /**
     * Optional narrowing of an UPDATED trigger to specific fields. Empty means "any change".
     * It is an efficiency measure and the first loop guard at once: a rule that only reacts to
     * {@code status} cannot be woken by an action that writes something else.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "automation_rule_changed_fields",
            joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "field_name", length = 64)
    private Set<String> triggerChangedFields = new HashSet<>();

    @Schema(description = "Whether the rule runs at all")
    private boolean enabled = true;

    /**
     * How deep a cascade this rule may still take part in. Null uses the engine default. The
     * old engine had no such limit, and it also had no action able to create anything — the
     * combination is what makes the limit necessary now.
     */
    @Column(name = "max_depth")
    private Integer maxDepth;

    /**
     * Ordered, and not for presentation. The evaluator reports the <em>first</em> condition that
     * did not hold, and that sentence goes into the run log as the answer to "why did my rule not
     * fire?". Without a stable order the same rule can blame a different condition from one run
     * to the next, which makes the log misleading exactly where it is meant to be trusted.
     */
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<AutomationCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<AutomationActionStep> actions = new ArrayList<>();

    public void addCondition(AutomationCondition condition) {
        condition.setRule(this);
        this.conditions.add(condition);
    }

    public void addAction(AutomationActionStep action) {
        action.setRule(this);
        this.actions.add(action);
    }
}
