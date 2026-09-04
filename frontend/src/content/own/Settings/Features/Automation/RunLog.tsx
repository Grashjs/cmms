import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import ArrowBackTwoToneIcon from '@mui/icons-material/ArrowBackTwoTone';
import RefreshTwoToneIcon from '@mui/icons-material/RefreshTwoTone';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../../store';
import { getAutomationRuns } from '../../../../../slices/automation';
import { AutomationRule, RunStatus } from '../../../../../models/owns/automation';

const STATUS_COLOUR: {
  [status in RunStatus]: 'success' | 'warning' | 'error';
} = {
  SUCCESS: 'success',
  SKIPPED: 'warning',
  FAILED: 'error'
};

interface RunLogProps {
  /** The rule whose runs to show, or null for the whole company's log. */
  rule: AutomationRule | null;
  onBack: () => void;
}

/**
 * The execution history, including the runs that decided to do nothing.
 *
 * <p>That is the point of this view, and the reason the engine records a SKIPPED run at all. The
 * old engine kept no history, so "why did my rule not fire?" had no answer anywhere in the
 * product. Here the `detail` column names the condition that did not hold — which has already
 * caught a real mistake: a rule comparing a choice field to a value that field could never hold.
 */
export default function RunLog({ rule, onBack }: RunLogProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { runs, loadingRuns } = useSelector((state) => state.automation);

  const key = rule == null ? 'all' : String(rule.id);
  const rows = runs[key] ?? [];

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <Button startIcon={<ArrowBackTwoToneIcon />} onClick={onBack}>
          {t('go_back')}
        </Button>
        <Typography variant="h4" flex={1}>
          {rule ? rule.title : t('automation_all_runs')}
        </Typography>
        <Button
          startIcon={<RefreshTwoToneIcon />}
          onClick={() => dispatch(getAutomationRuns(rule?.id ?? null))}
        >
          {t('automation_refresh')}
        </Button>
      </Stack>

      {loadingRuns && <CircularProgress />}

      {!loadingRuns && !rows.length && (
        <Box>
          <Typography>{t('automation_no_runs')}</Typography>
          <Typography variant="subtitle2">
            {t('automation_no_runs_hint')}
          </Typography>
        </Box>
      )}

      {!!rows.length && (
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('automation_triggered_at')}</TableCell>
                {!rule && <TableCell>{t('automation_rule')}</TableCell>}
                <TableCell>{t('automation_entity')}</TableCell>
                <TableCell>{t('status')}</TableCell>
                <TableCell>{t('automation_detail')}</TableCell>
                <TableCell>{t('automation_actions_executed')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((run) => (
                <TableRow key={run.id}>
                  <TableCell>
                    {new Date(run.triggeredAt).toLocaleString()}
                  </TableCell>
                  {!rule && <TableCell>{run.ruleTitle}</TableCell>}
                  <TableCell>
                    {t(`automation_entity_${run.entityType.toLowerCase()}`)}{' '}
                    #{run.entityId}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={STATUS_COLOUR[run.status]}
                      label={t(`automation_status_${run.status.toLowerCase()}`)}
                    />
                  </TableCell>
                  <TableCell sx={{ maxWidth: 420 }}>{run.detail}</TableCell>
                  <TableCell>{run.actionsExecuted}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
    </Stack>
  );
}
