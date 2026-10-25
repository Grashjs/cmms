import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { useSelector } from '../../../../../store';
import { roundDecimals } from '../../../../../utils/formatters';

const percent = (value: number | null) =>
  value === null || value === undefined ? '—' : `${Math.round(value)}%`;

function Overview() {
  const { t }: { t: any } = useTranslation();
  const { kpis, loading } = useSelector((state) => state.kpiAnalytics);

  const items: { label: string; value: string | number; detail: string }[] = [
    {
      label: t('pm_compliance'),
      value: percent(kpis.pmCompliance),
      detail: t('on_time_of_due', {
        onTime: kpis.pmCompletedOnTime,
        due: kpis.pmDue
      })
    },
    {
      label: t('planned_work'),
      value: percent(kpis.plannedRatio),
      detail: t('planned_of_created', {
        planned: kpis.planned,
        created: kpis.planned + kpis.reactive
      })
    },
    {
      label: t('backlog'),
      value: kpis.backlog,
      detail: t('estimated_hours_value', {
        hours: roundDecimals(kpis.backlogHours)
      })
    },
    {
      label: t('overdue_work_orders'),
      value: kpis.overdue,
      detail: t('of_backlog', { backlog: kpis.backlog })
    }
  ];

  return (
    <AnalyticsCard
      title={t('maintenance_kpis')}
      description="maintenance_kpis_description"
    >
      {loading.kpis ? (
        <Loading />
      ) : (
        <Stack
          direction="row"
          justifyContent="center"
          flexWrap="wrap"
          sx={{ gap: 4 }}
        >
          {items.map((item) => (
            <Stack key={item.label} alignItems="center">
              <Typography variant="h2" fontWeight="bold">
                {item.value}
              </Typography>
              <Typography>{item.label}</Typography>
              <Typography variant="body2" color="text.secondary">
                {item.detail}
              </Typography>
            </Stack>
          ))}
        </Stack>
      )}
    </AnalyticsCard>
  );
}

export default Overview;
