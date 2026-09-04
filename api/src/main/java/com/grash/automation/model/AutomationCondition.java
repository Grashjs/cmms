package com.grash.automation.model;

import com.grash.model.CustomField;
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
 * One condition of a rule, stored as data rather than as an enum value with a switch behind it.
 *
 * <p>A condition names its operand by a dotted path — {@code asset.status}, {@code asset.cf} —
 * and an {@code OperandResolver} knows how to read it. Adding a new source of conditions is
 * therefore a new resolver and nothing else: no enum, no switch, no TypeScript mirrors.
 *
 * <p>No company column: a condition is only reachable through its rule, and the rule carries
 * the company. Same shape as {@code CustomFieldValue}, which hangs off its owner too.
 */
@Entity
@Table(name = "automation_condition")
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "One condition of an automation rule")
public class AutomationCondition extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AutomationRule rule;

    /**
     * Which value to read, as a dotted path. {@code asset.cf} additionally needs
     * {@link #customField} to say <em>which</em> custom field.
     */
    @NotNull
    @Column(length = 128)
    private String subject;

    /**
     * A real foreign key, not the field's id encoded in {@link #subject}. Without it, deleting
     * a custom field would leave a rule pointing at nothing and the database could not object —
     * the rule would simply stop matching, quietly, which is the failure mode this whole
     * document is trying to get rid of.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_field_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CustomField customField;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ConditionOperator operator;

    @Column(name = "expected_value", columnDefinition = "TEXT")
    private String expectedValue;
}
