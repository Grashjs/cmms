import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  LinearProgress,
  Link,
  Stack,
  Tooltip,
  Typography,
  useTheme
} from '@mui/material';
import AutoAwesomeTwoToneIcon from '@mui/icons-material/AutoAwesomeTwoTone';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import ClearTwoToneIcon from '@mui/icons-material/ClearTwoTone';
import RefreshTwoToneIcon from '@mui/icons-material/RefreshTwoTone';
import { useTranslation } from 'react-i18next';
import { useEffect } from 'react';
import { useDispatch, useSelector } from '../../../store';
import {
  applyQualification,
  getRequestQualification,
  rejectQualification,
  rerunQualification
} from '../../../slices/requestQualification';
import { QualificationCandidate } from '../../../models/owns/requestQualification';
import Request from '../../../models/owns/request';
import { getAssetUrl } from '../../../utils/urlPaths';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';

interface QualificationCardProps {
  request: Request;
}

/**
 * The triage suggestion for one request: which asset the system thinks it is about, why, and the
 * two buttons that end the question.
 *
 * <p>Three decisions about how this is shown, all of them about trust rather than about layout:
 *
 * - It names the words that produced the match. A percentage on its own is unfalsifiable and gets
 *   ignored within a week; "Heizung, Keller" can be judged in a second.
 * - It says which engine answered. A reader who knows the suggestion came from word matching
 *   reads a miss as a limitation, not as the system being broken.
 * - It calls the number a match, never a confidence. It measures how much of the request text this
 *   asset accounts for, which is not a probability that the asset is right, and labelling it as one
 *   would be a promise the matcher cannot keep.
 */
export default function QualificationCard({ request }: QualificationCardProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { hasEditPermission } = useAuth();
  const { byRequestId, loadingByRequestId, deciding } = useSelector(
    (state) => state.requestQualifications
  );
  const qualification = byRequestId[request.id];
  const loading = loadingByRequestId[request.id];

  /**
   * Only fetched for requests that are still open and that the user could act on. A suggestion
   * shown next to a request nobody can change is a card that can only be dismissed, and the
   * request already carries its asset by then anyway.
   */
  const relevant =
    !request.workOrder &&
    !request.cancelled &&
    hasEditPermission(PermissionEntity.REQUESTS, request);

  useEffect(() => {
    if (relevant) dispatch(getRequestQualification(request.id));
  }, [request.id, relevant]);

  if (!relevant) return null;
  if (loading && qualification === undefined) return null;
  if (!qualification) return null;

  const formatScore = (score: number) => `${Math.round(score * 100)} %`;

  const rerunButton = (
    <Button
      size="small"
      startIcon={<RefreshTwoToneIcon />}
      disabled={deciding}
      onClick={() => dispatch(rerunQualification(request.id))}
    >
      {t('triage_rerun')}
    </Button>
  );

  /**
   * A dismissed suggestion collapses to one line instead of disappearing. Hiding it entirely was
   * the first version and it is a dead end: once the suggestions were wrong, the usual next step
   * is to fix the asset name or location and ask again, and there would have been no way to do
   * that from here.
   */
  if (qualification.status === 'REJECTED') {
    return (
      <Card sx={{ width: '100%', mb: 2 }}>
        <CardContent>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
            spacing={2}
          >
            <Typography
              variant="subtitle2"
              sx={{ color: theme.colors.alpha.black[50] }}
            >
              {t('triage_rejected')}
            </Typography>
            {rerunButton}
          </Stack>
        </CardContent>
      </Card>
    );
  }

  if (qualification.status !== 'PENDING') return null;

  const renderCandidate = (candidate: QualificationCandidate) => (
    <Box key={candidate.asset.id} sx={{ py: 1.5 }}>
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        spacing={2}
      >
        <Box sx={{ minWidth: 0, flexGrow: 1 }}>
          <Link variant="h5" href={getAssetUrl(candidate.asset.id)}>
            {candidate.asset.name}
          </Link>
          {candidate.asset.customId && (
            <Typography
              variant="subtitle2"
              sx={{ color: theme.colors.alpha.black[50] }}
            >
              {candidate.asset.customId}
            </Typography>
          )}
        </Box>
        <Button
          size="small"
          variant={candidate.ordinal === 0 ? 'contained' : 'outlined'}
          startIcon={<CheckTwoToneIcon />}
          disabled={deciding}
          onClick={() =>
            dispatch(
              applyQualification(
                qualification.id,
                request.id,
                candidate.asset.id
              )
            )
          }
        >
          {t('triage_apply')}
        </Button>
      </Stack>

      <Tooltip title={t('triage_match_explanation')}>
        <Box sx={{ mt: 1, maxWidth: 260 }}>
          <Stack direction="row" justifyContent="space-between">
            <Typography
              variant="subtitle2"
              sx={{ color: theme.colors.alpha.black[50] }}
            >
              {t('triage_match')}
            </Typography>
            <Typography variant="subtitle2">
              {formatScore(candidate.score)}
            </Typography>
          </Stack>
          <LinearProgress
            variant="determinate"
            value={Math.round(candidate.score * 100)}
            sx={{ mt: 0.5, height: 6, borderRadius: 3 }}
          />
        </Box>
      </Tooltip>

      {!!candidate.matchedTerms.length && (
        <Stack direction="row" spacing={0.5} sx={{ mt: 1, flexWrap: 'wrap' }}>
          {candidate.matchedTerms.map((term) => (
            <Chip key={term} label={term} size="small" variant="outlined" />
          ))}
        </Stack>
      )}
    </Box>
  );

  return (
    <Card sx={{ width: '100%', mb: 2 }}>
      <CardContent>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="flex-start"
          spacing={1}
        >
          <Box>
            <Stack direction="row" alignItems="center" spacing={1}>
              <AutoAwesomeTwoToneIcon color="primary" />
              <Typography variant="h4">{t('triage_title')}</Typography>
            </Stack>
            <Typography
              variant="subtitle2"
              sx={{ mt: 0.5, color: theme.colors.alpha.black[50] }}
            >
              {t('triage_subtitle', { engine: qualification.engine })}
            </Typography>
          </Box>
          {deciding && <CircularProgress size="1.5rem" />}
        </Stack>

        <Divider sx={{ mt: 2 }} />
        {qualification.candidates.map(renderCandidate)}
        <Divider sx={{ mb: 2 }} />

        <Stack direction="row" spacing={2}>
          <Button
            size="small"
            variant="outlined"
            startIcon={<ClearTwoToneIcon />}
            disabled={deciding}
            onClick={() =>
              dispatch(rejectQualification(qualification.id, request.id))
            }
          >
            {t('triage_reject')}
          </Button>
          {rerunButton}
        </Stack>
      </CardContent>
    </Card>
  );
}
