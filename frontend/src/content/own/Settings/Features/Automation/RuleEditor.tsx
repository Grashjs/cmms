import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Divider,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ActionDescriptor,
  AutomationMeta,
  AutomationRule,
  AutomationRulePayload,
  OperandDescriptor,
  operandKey,
  operandLabel
} from '../../../../../models/owns/automation';
import ValueInput, { Option, useEntityOptions } from './ValueInput';
import { getErrorMessage } from '../../../../../utils/api';

interface DraftCondition {
  /** `operandKey` of the chosen operand, so a custom field is distinguishable from its peers. */
  operand: string;
  operator: string;
  expectedValue: string;
}

interface DraftAction {
  actionType: string;
  /** Parameter name to value. Empty strings are dropped when the JSON is built. */
  parameters: { [name: string]: string };
}

interface RuleEditorProps {
  meta: AutomationMeta;
  /** The rule being edited, or null to create one. */
  rule: AutomationRule | null;
  onCancel: () => void;
  onSave: (payload: AutomationRulePayload) => Promise<void>;
}

const triggerKey = (entityType: string, changeType: string) =>
  `${entityType}|${changeType}`;

/**
 * The rule editor. Everything it offers comes from `meta`; it contains no list of subjects,
 * operators or actions of its own, which is the property that keeps it from drifting away from
 * what the engine actually implements.
 *
 * <p>It does contain two pieces of judgement that metadata cannot express, and both are marked
 * where they occur: a trigger that nothing publishes is shown as unavailable rather than hidden,
 * and the value of `SET_CUSTOM_FIELD` is paired with the field chosen next to it.
 */
export default function RuleEditor({
  meta,
  rule,
  onCancel,
  onSave
}: RuleEditorProps) {
  const { t }: { t: any } = useTranslation();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState(rule?.title ?? '');
  const [trigger, setTrigger] = useState(
    rule
      ? triggerKey(rule.triggerEntityType, rule.triggerChangeType)
      : // The first live trigger, so a new rule starts on something that can actually fire.
        (() => {
          const live = meta.triggers.find((candidate) => candidate.live);
          return live ? triggerKey(live.entityType, live.changeType) : '';
        })()
  );
  const [changedFields, setChangedFields] = useState<string[]>(
    rule?.triggerChangedFields ?? []
  );
  const [conditions, setConditions] = useState<DraftCondition[]>(
    (rule?.conditions ?? []).map((condition) => ({
      operand: operandKey(condition),
      operator: condition.operator,
      expectedValue: condition.expectedValue ?? ''
    }))
  );
  const [actions, setActions] = useState<DraftAction[]>(
    (rule?.actions ?? []).map((action) => ({
      actionType: action.actionType,
      parameters: parseParameters(action.parameters)
    }))
  );

  const customFieldOptions: Option[] = meta.subjects
    .filter((subject) => subject.customFieldId != null)
    .map((subject) => ({
      value: String(subject.customFieldId),
      label: subject.label ?? String(subject.customFieldId)
    }));
  const { optionsFor } = useEntityOptions(customFieldOptions);

  const selectedTrigger = meta.triggers.find(
    (candidate) => triggerKey(candidate.entityType, candidate.changeType) === trigger
  );
  const operandByKey = (key: string): OperandDescriptor | undefined =>
    meta.subjects.find((subject) => operandKey(subject) === key);
  const descriptorFor = (actionType: string): ActionDescriptor | undefined =>
    meta.actions.find((action) => action.type === actionType);

  const placeholderHint = t('automation_placeholder_hint', {
    placeholders: meta.placeholders.join(', ')
  });

  const setCondition = (index: number, patch: Partial<DraftCondition>) =>
    setConditions(
      conditions.map((condition, position) =>
        position === index ? { ...condition, ...patch } : condition
      )
    );
  const setAction = (index: number, patch: Partial<DraftAction>) =>
    setActions(
      actions.map((action, position) =>
        position === index ? { ...action, ...patch } : action
      )
    );

  const handleSave = async () => {
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const [entityType, changeType] = trigger.split('|');
      await onSave({
        title,
        triggerEntityType: entityType,
        triggerChangeType: changeType,
        triggerChangedFields: changedFields,
        enabled: rule?.enabled ?? true,
        maxDepth: rule?.maxDepth ?? null,
        conditions: conditions.map((condition) => {
          const operand = operandByKey(condition.operand)!;
          return {
            subject: operand.subject,
            customFieldId: operand.customFieldId,
            operator: condition.operator,
            expectedValue: condition.expectedValue || null
          };
        }),
        actions: actions.map((action, index) => ({
          actionType: action.actionType,
          parameters: buildParameters(action),
          orderIndex: index,
          abortOnFailure: true
        }))
      });
    } catch (exception: any) {
      // The server refuses a rule it could not carry out — a value a choice field can never
      // hold, a subject nothing reads. Its message says which, so it is shown as it is rather
      // than replaced by a generic failure.
      // getErrorMessage, not exception.message: the raw message is the JSON response body.
      setError(getErrorMessage(exception, t('automation_save_failed')));
    } finally {
      setSaving(false);
    }
  };

  const validate = (): string | null => {
    if (!title.trim()) return t('automation_title_required');
    if (!trigger) return t('automation_trigger_required');
    if (!actions.length) return t('automation_action_required');
    for (const condition of conditions) {
      if (!condition.operand || !condition.operator) {
        return t('automation_condition_incomplete');
      }
    }
    for (const action of actions) {
      const descriptor = descriptorFor(action.actionType);
      const missing = (descriptor?.parameters ?? []).find(
        (parameter) => parameter.required && !action.parameters[parameter.name]
      );
      if (missing) {
        return t('automation_parameter_required', {
          parameter: t(missing.labelKey)
        });
      }
    }
    return null;
  };

  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={1} alignItems="center">
        <Button onClick={onCancel}>{t('cancel')}</Button>
        <Box flex={1} />
        <Button variant="contained" onClick={handleSave} disabled={saving}>
          {t('save')}
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      <TextField
        label={t('title')}
        value={title}
        required
        onChange={(event) => setTitle(event.target.value)}
      />

      <Card sx={{ p: 2 }}>
        <Typography variant="h5" gutterBottom>
          {t('automation_when')}
        </Typography>
        <Stack spacing={2}>
          <TextField
            select
            label={t('automation_trigger')}
            value={trigger}
            onChange={(event) => {
              setTrigger(event.target.value);
              // The field filter belongs to the trigger's own diff, so it cannot survive a
              // change of trigger — a leftover field name would silently never match.
              setChangedFields([]);
            }}
          >
            {meta.triggers.map((candidate) => {
              const key = triggerKey(candidate.entityType, candidate.changeType);
              return (
                <MenuItem
                  key={key}
                  value={key}
                  // Not hidden: a trigger nothing publishes would produce a rule that saves,
                  // looks right and never fires. Saying "not available yet" is the difference
                  // between a limitation and a bug report.
                  disabled={!candidate.live}
                >
                  {t(`automation_entity_${candidate.entityType.toLowerCase()}`)}
                  {' · '}
                  {t(`automation_change_${candidate.changeType.toLowerCase()}`)}
                  {!candidate.live && ` — ${t('automation_not_wired')}`}
                </MenuItem>
              );
            })}
          </TextField>

          {!!selectedTrigger?.changedFields.length && (
            <TextField
              select
              SelectProps={{
                multiple: true,
                renderValue: (selected: any) => (
                  <Stack direction="row" spacing={0.5}>
                    {(selected as string[]).map((field) => (
                      <Chip key={field} size="small" label={field} />
                    ))}
                  </Stack>
                )
              }}
              label={t('automation_only_when_changed')}
              helperText={t('automation_only_when_changed_hint')}
              value={changedFields}
              onChange={(event) =>
                setChangedFields(event.target.value as unknown as string[])
              }
            >
              {selectedTrigger.changedFields.map((field) => (
                <MenuItem key={field} value={field}>
                  {field}
                </MenuItem>
              ))}
            </TextField>
          )}
        </Stack>
      </Card>

      <Card sx={{ p: 2 }}>
        <Typography variant="h5">{t('automation_and_if')}</Typography>
        <Typography variant="subtitle2" gutterBottom>
          {t('automation_conditions_all_hint')}
        </Typography>
        <Stack spacing={2} sx={{ mt: 2 }}>
          {conditions.map((condition, index) => {
            const operand = operandByKey(condition.operand);
            return (
              <Stack
                key={index}
                direction={{ xs: 'column', md: 'row' }}
                spacing={1}
                alignItems="flex-start"
              >
                <TextField
                  select
                  fullWidth
                  size="small"
                  label={t('automation_subject')}
                  value={condition.operand}
                  onChange={(event) => {
                    const next = operandByKey(event.target.value);
                    // Operators and values are properties of the operand, so both are reset —
                    // keeping them is how a rule ends up comparing a status to an asset id.
                    setCondition(index, {
                      operand: event.target.value,
                      operator: next?.operators[0] ?? '',
                      expectedValue: ''
                    });
                  }}
                  helperText={
                    operand?.boundToCategories.length
                      ? t('automation_bound_to_categories', {
                          categories: operand.boundToCategories.join(', ')
                        })
                      : undefined
                  }
                >
                  {meta.subjects.map((subject) => (
                    <MenuItem key={operandKey(subject)} value={operandKey(subject)}>
                      {operandLabel(subject, t)}
                    </MenuItem>
                  ))}
                </TextField>

                <TextField
                  select
                  fullWidth
                  size="small"
                  label={t('automation_operator')}
                  value={condition.operator}
                  onChange={(event) =>
                    setCondition(index, { operator: event.target.value })
                  }
                >
                  {(operand?.operators ?? []).map((operator) => (
                    <MenuItem key={operator} value={operator}>
                      {t(`automation_operator_${operator.toLowerCase()}`)}
                    </MenuItem>
                  ))}
                </TextField>

                <Box sx={{ width: '100%' }}>
                  <ValueInput
                    valueType={operand?.valueType ?? 'TEXT'}
                    options={operand?.options ?? []}
                    value={condition.expectedValue}
                    label={t('automation_value')}
                    optionsFor={optionsFor}
                    onChange={(value) =>
                      setCondition(index, { expectedValue: value })
                    }
                  />
                </Box>

                <IconButton
                  onClick={() =>
                    setConditions(
                      conditions.filter((_, position) => position !== index)
                    )
                  }
                >
                  <DeleteTwoToneIcon color="error" />
                </IconButton>
              </Stack>
            );
          })}
          <Box>
            <Button
              startIcon={<AddTwoToneIcon />}
              disabled={!meta.subjects.length}
              onClick={() => {
                const first = meta.subjects[0];
                setConditions([
                  ...conditions,
                  {
                    operand: operandKey(first),
                    operator: first.operators[0],
                    expectedValue: ''
                  }
                ]);
              }}
            >
              {t('automation_add_condition')}
            </Button>
          </Box>
        </Stack>
      </Card>

      <Card sx={{ p: 2 }}>
        <Typography variant="h5" gutterBottom>
          {t('automation_then')}
        </Typography>
        <Stack spacing={2} divider={<Divider />}>
          {actions.map((action, index) => {
            const descriptor = descriptorFor(action.actionType);
            return (
              <Stack key={index} spacing={2}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <TextField
                    select
                    fullWidth
                    size="small"
                    label={t('automation_action')}
                    value={action.actionType}
                    onChange={(event) =>
                      // Parameters belong to the action type, so switching discards them.
                      setAction(index, {
                        actionType: event.target.value,
                        parameters: {}
                      })
                    }
                  >
                    {meta.actions.map((candidate) => (
                      <MenuItem key={candidate.type} value={candidate.type}>
                        {t(candidate.labelKey)}
                      </MenuItem>
                    ))}
                  </TextField>
                  <IconButton
                    onClick={() =>
                      setActions(
                        actions.filter((_, position) => position !== index)
                      )
                    }
                  >
                    <DeleteTwoToneIcon color="error" />
                  </IconButton>
                </Stack>

                {(descriptor?.parameters ?? []).map((parameter) => {
                  const paired = pairedChoiceOptions(
                    parameter.name,
                    descriptor!,
                    action,
                    meta
                  );
                  return (
                    <ValueInput
                      key={parameter.name}
                      valueType={paired ? 'CHOICE' : parameter.valueType}
                      options={paired ?? parameter.options}
                      required={parameter.required}
                      value={action.parameters[parameter.name] ?? ''}
                      label={t(parameter.labelKey)}
                      helperText={
                        parameter.placeholders ? placeholderHint : undefined
                      }
                      optionsFor={optionsFor}
                      onChange={(value) =>
                        setAction(index, {
                          parameters: {
                            ...action.parameters,
                            [parameter.name]: value
                          }
                        })
                      }
                    />
                  );
                })}
              </Stack>
            );
          })}
          <Box>
            <Button
              startIcon={<AddTwoToneIcon />}
              disabled={!meta.actions.length}
              onClick={() =>
                setActions([
                  ...actions,
                  { actionType: meta.actions[0].type, parameters: {} }
                ])
              }
            >
              {t('automation_add_action')}
            </Button>
          </Box>
        </Stack>
      </Card>
    </Stack>
  );
}

/**
 * The options a `value` parameter may take, when the same action also selects a custom field.
 *
 * <p>The only piece of cross-parameter knowledge in the editor, and it is here rather than in the
 * server's descriptor because it is genuinely dynamic: which options are permitted depends on
 * the field picked in the row above, which the server cannot know when it describes the action.
 * Without it, `SET_CUSTOM_FIELD` writes free text into a single-choice field — a value that
 * field can never legitimately hold, and the mistake that made the first real rule useless.
 */
function pairedChoiceOptions(
  parameterName: string,
  descriptor: ActionDescriptor,
  action: DraftAction,
  meta: AutomationMeta
): string[] | null {
  if (parameterName !== 'value') return null;
  const fieldParameter = descriptor.parameters.find(
    (parameter) => parameter.valueType === 'ENTITY_CUSTOM_FIELD'
  );
  if (!fieldParameter) return null;
  const fieldId = action.parameters[fieldParameter.name];
  if (!fieldId) return null;
  const operand = meta.subjects.find(
    (subject) => String(subject.customFieldId) === fieldId
  );
  return operand?.options.length ? operand.options : null;
}

/** Stored parameters back into form state. A malformed value yields an empty form, not a crash. */
function parseParameters(parameters: string): { [name: string]: string } {
  try {
    const parsed = JSON.parse(parameters || '{}');
    return Object.fromEntries(
      Object.entries(parsed).map(([key, value]) => [key, String(value ?? '')])
    );
  } catch {
    return {};
  }
}

/** Form state back into the JSON the engine reads. Empty values are omitted, not sent as "". */
function buildParameters(action: DraftAction): string {
  const filled = Object.entries(action.parameters).filter(
    ([, value]) => value !== '' && value != null
  );
  return JSON.stringify(Object.fromEntries(filled));
}
