import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  FormControlLabel,
  Grid,
  Stack,
  Switch,
  Typography
} from '@mui/material';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import HistoryTwoToneIcon from '@mui/icons-material/HistoryTwoTone';
import { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../../store';
import { CustomSnackBarContext } from '../../../../../contexts/CustomSnackBarContext';
import { TitleContext } from '../../../../../contexts/TitleContext';
import ConfirmDialog from '../../../components/ConfirmDialog';
import { getErrorMessage } from '../../../../../utils/api';
import {
  addAutomationRule,
  deleteAutomationRule,
  editAutomationRule,
  getAutomationMeta,
  getAutomationRules,
  getAutomationRuns,
  setAutomationRuleEnabled
} from '../../../../../slices/automation';
import {
  AutomationRule,
  AutomationRulePayload,
  operandKey,
  operandLabel
} from '../../../../../models/owns/automation';
import RuleEditor from './RuleEditor';
import RunLog from './RunLog';

/**
 * The rule list. A deliberately separate page from `Settings/Features/Workflows`, which keeps
 * running the old engine untouched — the two coexist, so nothing that depends on the old one
 * (including the unmodified upstream mobile app) is affected by anything here.
 */
function Automation() {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { setTitle } = useContext(TitleContext);
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const { rules, meta, loadingRules, loadingMeta } = useSelector(
    (state) => state.automation
  );
  const [view, setView] = useState<'list' | 'edit' | 'runs'>('list');
  const [editing, setEditing] = useState<AutomationRule | null>(null);
  const [runsOf, setRunsOf] = useState<AutomationRule | null>(null);
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);

  useEffect(() => {
    setTitle(t('automation_rules'));
    dispatch(getAutomationRules());
    dispatch(getAutomationMeta());
  }, []);

  const handleSave = async (payload: AutomationRulePayload) => {
    // Deliberately not caught here: the editor shows the server's own message, which names the
    // condition or value it refused, and a snackbar would hide it behind "an error occurred".
    if (editing) {
      await dispatch(editAutomationRule(editing.id, payload));
    } else {
      await dispatch(addAutomationRule(payload));
    }
    showSnackBar(t('automation_rule_saved'), 'success');
    setView('list');
    setEditing(null);
  };

  const handleSetEnabled = (rule: AutomationRule, enabled: boolean) =>
    dispatch(setAutomationRuleEnabled(rule.id, enabled)).catch((error) =>
      showSnackBar(getErrorMessage(error, t('automation_save_failed')), 'error')
    );

  const handleDelete = (id: number) => {
    setPendingDelete(null);
    dispatch(deleteAutomationRule(id))
      .then(() => showSnackBar(t('automation_rule_deleted'), 'success'))
      .catch((error) =>
        showSnackBar(getErrorMessage(error, t('automation_save_failed')), 'error')
      );
  };

  const describeTrigger = (rule: AutomationRule) =>
    `${t(`automation_entity_${rule.triggerEntityType.toLowerCase()}`)} · ${t(
      `automation_change_${rule.triggerChangeType.toLowerCase()}`
    )}`;

  /** A rule's conditions in words, so the list answers "what does this do?" without opening it. */
  const describeConditions = (rule: AutomationRule): string => {
    if (!rule.conditions.length) return t('automation_no_conditions');
    return rule.conditions
      .map((condition) => {
        const operand = meta?.subjects.find(
          (subject) => operandKey(subject) === operandKey(condition)
        );
        const label = operand
          ? operandLabel(operand, t)
          : // A condition whose operand the metadata no longer offers — a deleted custom field,
            // say. Shown as stored rather than hidden, because it still decides whether the rule
            // fires.
            condition.customFieldLabel ?? condition.subject;
        return `${label} ${t(
          `automation_operator_${condition.operator.toLowerCase()}`
        )} ${condition.expectedValue ?? ''}`.trim();
      })
      .join(` ${t('automation_and')} `);
  };

  if (loadingMeta && !meta) {
    return (
      <Box p={4} display="flex" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }

  if (view === 'runs') {
    return (
      <Box p={4}>
        <RunLog
          rule={runsOf}
          onBack={() => {
            setRunsOf(null);
            setView('list');
          }}
        />
      </Box>
    );
  }

  if (view === 'edit' && meta) {
    return (
      <Box p={4}>
        <RuleEditor
          meta={meta}
          rule={editing}
          onCancel={() => {
            setEditing(null);
            setView('list');
          }}
          onSave={handleSave}
        />
      </Box>
    );
  }

  return (
    <Box p={4}>
      <Stack spacing={2}>
        {meta && !meta.engineEnabled && (
          // Without this the page is a trap: rules save, look correct, and never run, and
          // nothing on screen would say why. AUTOMATION_ENABLED defaults to false.
          <Alert severity="warning">{t('automation_engine_disabled')}</Alert>
        )}

        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="h4" flex={1}>
            {t('automation_rules')}
          </Typography>
          <Button
            startIcon={<HistoryTwoToneIcon />}
            onClick={() => {
              setRunsOf(null);
              dispatch(getAutomationRuns(null));
              setView('runs');
            }}
          >
            {t('automation_all_runs')}
          </Button>
          <Button
            variant="contained"
            startIcon={<AddTwoToneIcon />}
            onClick={() => {
              setEditing(null);
              setView('edit');
            }}
          >
            {t('automation_add_rule')}
          </Button>
        </Stack>

        <Typography variant="subtitle2">
          {t('automation_rules_description')}
        </Typography>

        {loadingRules && <CircularProgress />}

        {!loadingRules && !rules.length && (
          <Typography>{t('automation_no_rules')}</Typography>
        )}

        <Grid container spacing={2}>
          {rules.map((rule) => (
            <Grid item xs={12} key={rule.id}>
              <Card sx={{ p: 2 }}>
                <Stack
                  direction={{ xs: 'column', md: 'row' }}
                  spacing={2}
                  alignItems={{ md: 'center' }}
                >
                  <Box flex={1}>
                    <Typography variant="h5">{rule.title}</Typography>
                    <Stack
                      direction="row"
                      spacing={1}
                      sx={{ mt: 1, flexWrap: 'wrap' }}
                    >
                      <Chip size="small" label={describeTrigger(rule)} />
                      {rule.triggerChangedFields.map((field) => (
                        <Chip
                          key={field}
                          size="small"
                          variant="outlined"
                          label={field}
                        />
                      ))}
                      {rule.actions.map((action, index) => (
                        <Chip
                          key={index}
                          size="small"
                          color="primary"
                          label={t(
                            meta?.actions.find(
                              (candidate) => candidate.type === action.actionType
                            )?.labelKey ?? action.actionType
                          )}
                        />
                      ))}
                    </Stack>
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {describeConditions(rule)}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <FormControlLabel
                      control={
                        <Switch
                          checked={rule.enabled}
                          onChange={(event) =>
                            handleSetEnabled(rule, event.target.checked)
                          }
                        />
                      }
                      label={rule.enabled ? t('enabled') : t('disabled')}
                    />
                    <Button
                      onClick={() => {
                        setRunsOf(rule);
                        dispatch(getAutomationRuns(rule.id));
                        setView('runs');
                      }}
                    >
                      {t('automation_runs')}
                    </Button>
                    <Button
                      onClick={() => {
                        setEditing(rule);
                        setView('edit');
                      }}
                    >
                      {t('edit')}
                    </Button>
                    <Button color="error" onClick={() => setPendingDelete(rule.id)}>
                      {t('to_delete')}
                    </Button>
                  </Stack>
                </Stack>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Stack>

      <ConfirmDialog
        open={pendingDelete != null}
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => handleDelete(pendingDelete!)}
        confirmText={t('to_delete')}
        question={t('automation_confirm_delete_rule')}
      />
    </Box>
  );
}

export default Automation;
