/**
 * The automation engine as the frontend sees it.
 *
 * <p>Note what is <b>not</b> here: no list of subjects, no list of operators per subject, no list
 * of actions and no list of their parameters. Those arrive from `GET /automation-rules/meta` at
 * runtime. That is the whole difference to `workflow.ts` next door, which mirrors three Java
 * enums by hand — and where the mirror had drifted, so the settings form offered conditions the
 * backend never evaluated and actions no branch carried out.
 *
 * <p>The unions below are the exception, and a deliberate one: they are shapes the editor has to
 * *branch on* (an ENUM parameter needs a dropdown, a TEXT one a text box), not vocabulary. If a
 * new value type appears server-side, the editor falls back to a text box rather than breaking —
 * see `ParameterInput`.
 */

export type RunStatus = 'SUCCESS' | 'SKIPPED' | 'FAILED';

/** The shapes the editor knows how to render. Anything else degrades to a text box. */
export type ValueType =
  | 'TEXT'
  | 'NUMBER'
  | 'ENUM'
  | 'CHOICE'
  | 'TRIGGER_REFERENCE'
  | `ENTITY_${string}`;

export interface OperandDescriptor {
  /** The path a condition stores, e.g. `asset.status` or `asset.cf`. */
  subject: string;
  /** Set exactly when `subject` is the custom-field subject. */
  customFieldId: number | null;
  /** A translation key for a built-in operand; null for a custom field. */
  labelKey: string | null;
  /** The user's own wording for a custom field; null for a built-in operand. */
  label: string | null;
  valueType: ValueType;
  operators: string[];
  /** Permitted values for ENUM and CHOICE. */
  options: string[];
  /**
   * The asset categories a custom field is bound to. Non-empty means an asset of another
   * category has no value for it, so the condition cannot hold — which reads like a broken rule
   * unless the editor says so.
   */
  boundToCategories: string[];
}

export interface ActionParameterDescriptor {
  name: string;
  labelKey: string;
  valueType: ValueType;
  required: boolean;
  options: string[];
  /** Whether `${trigger.…}` may be interpolated here. */
  placeholders: boolean;
}

export interface ActionDescriptor {
  type: string;
  labelKey: string;
  parameters: ActionParameterDescriptor[];
}

export interface TriggerDescriptor {
  entityType: string;
  changeType: string;
  /** False means nothing publishes this event yet: a rule on it would never fire. */
  live: boolean;
  changedFields: string[];
}

export interface AutomationMeta {
  /** False means rules can be saved but no event reaches them (`AUTOMATION_ENABLED`). */
  engineEnabled: boolean;
  triggers: TriggerDescriptor[];
  subjects: OperandDescriptor[];
  actions: ActionDescriptor[];
  placeholders: string[];
}

export interface AutomationCondition {
  id?: number;
  subject: string;
  customFieldId: number | null;
  customFieldLabel?: string | null;
  operator: string;
  expectedValue: string | null;
}

export interface AutomationAction {
  id?: number;
  actionType: string;
  /** A JSON object, as a string. Kept as sent so a round trip cannot lose a key. */
  parameters: string;
  orderIndex?: number;
  abortOnFailure?: boolean;
}

export interface AutomationRule {
  id: number;
  title: string;
  triggerChangeType: string;
  triggerEntityType: string;
  triggerChangedFields: string[];
  enabled: boolean;
  maxDepth: number | null;
  conditions: AutomationCondition[];
  actions: AutomationAction[];
}

/** What POST and PATCH take. Same shape minus the server-assigned ids. */
export interface AutomationRulePayload {
  title: string;
  triggerChangeType: string;
  triggerEntityType: string;
  triggerChangedFields: string[];
  enabled: boolean;
  maxDepth: number | null;
  conditions: Omit<AutomationCondition, 'id' | 'customFieldLabel'>[];
  actions: Omit<AutomationAction, 'id'>[];
}

export interface AutomationRun {
  id: number;
  ruleId: number | null;
  ruleTitle: string | null;
  entityType: string;
  entityId: number;
  status: RunStatus;
  /** Why it was skipped, or what failed. The half that answers "why did nothing happen?". */
  detail: string | null;
  actionsExecuted: number;
  correlationId: string;
  depth: number;
  triggeredAt: string;
}

/** How an operand is labelled: a translation key for built-ins, the user's own text otherwise. */
export const operandLabel = (operand: OperandDescriptor, t: any): string =>
  operand.label ?? (operand.labelKey ? t(operand.labelKey) : operand.subject);

/** Identifies an operand in a select, since a custom field shares its subject with all others. */
export const operandKey = (operand: {
  subject: string;
  customFieldId: number | null;
}): string =>
  operand.customFieldId == null
    ? operand.subject
    : `${operand.subject}:${operand.customFieldId}`;
