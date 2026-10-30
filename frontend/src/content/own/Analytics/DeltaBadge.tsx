import { Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { computeDelta } from '../../../utils/periodComparison';

interface DeltaBadgeProps {
  current: number | null | undefined;
  previous: number | null | undefined;
  previousPeriod: { start: Date; end: Date };
  // whether an increase is an improvement; omitted when it is neither (shown in grey)
  higherIsBetter?: boolean;
  // compare percentages by their difference in points instead of their relative change
  points?: boolean;
  // how to show the previous value in the tooltip
  formatValue?: (value: number) => string | number;
}

/**
 * Change of a summary value against the previous period: ▲ 12%, ▼ 3 pts...
 * Renders nothing until the previous period is loaded.
 */
export default function DeltaBadge({
  current,
  previous,
  previousPeriod,
  higherIsBetter,
  points = false,
  formatValue = (value) => value
}: DeltaBadgeProps) {
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
  const delta = computeDelta(current, previous, points);
  if (!delta) return null;

  const rounded = delta.value === null ? null : Math.round(delta.value);
  const unit = points ? ` ${t('percentage_points_short')}` : '%';
  const text =
    rounded === null
      ? '—'
      : rounded === 0
      ? `= 0${unit}`
      : `${rounded > 0 ? '▲' : '▼'} ${Math.abs(rounded)}${unit}`;
  const color =
    rounded === null || rounded === 0 || higherIsBetter === undefined
      ? 'text.secondary'
      : rounded > 0 === higherIsBetter
      ? 'success.main'
      : 'error.main';
  // Intl instead of the date-fns locale: no async loading for each badge
  const formatDate = (date: Date) => {
    try {
      return new Intl.DateTimeFormat(i18n.language?.replace('_', '-'), {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }).format(date);
    } catch {
      return date.toDateString();
    }
  };

  return (
    <Tooltip
      title={t('previous_period_value', {
        start: formatDate(previousPeriod.start),
        end: formatDate(previousPeriod.end),
        value: formatValue(previous as number)
      })}
    >
      <Typography variant="caption" fontWeight="bold" color={color}>
        {text}
      </Typography>
    </Tooltip>
  );
}
