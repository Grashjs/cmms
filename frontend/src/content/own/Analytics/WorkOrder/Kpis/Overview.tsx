import { ReactNode } from 'react';
import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { useSelector } from '../../../../../store';
import { roundDecimals } from '../../../../../utils/formatters';
import { FilterField } from '../../../../../models/owns/page';
import { fetchMaintenanceKpis } from '../../../../../slices/analytics/kpi';
import DeltaBadge from '../../DeltaBadge';
import usePreviousPeriod from '../../usePreviousPeriod';

const percent = (value: number | null) =>
  value === null || value === undefined ? '—' : `${Math.round(value)}%`;

interface OverviewProps {
  start: Date;
  end: Date;
  companyId?: number;
  filterFields?: FilterField[];
}

function Overview({ start, end, companyId, filterFields }: OverviewProps) {
  const { t }: { t: any } = useTranslation();
  const { kpis, loading } = useSelector((state) => state.kpiAnalytics);
  const previous = usePreviousPeriod(
    fetchMaintenanceKpis,
    start,
    end,
    companyId,
    filterFields
  );
  const formatPercent = (value: number) => `${Math.round(value)}%`;

  const items: {
    label: string;
    value: string | number;
    detail: string;
    delta: ReactNode;
  }[] = [
    {
      label: t('pm_compliance'),
      value: percent(kpis.pmCompliance),
      detail: t('on_time_of_due', {
        onTime: kpis.pmCompletedOnTime,
        due: kpis.pmDue
      }),
      delta: (
        <DeltaBadge
          current={kpis.pmCompliance}
          previous={previous.data?.pmCompliance}
          previousPeriod={previous}
          higherIsBetter
          points
          formatValue={formatPercent}
        />
      )
    },
    {
      label: t('planned_work'),
      value: percent(kpis.plannedRatio),
      detail: t('planned_of_created', {
        planned: kpis.planned,
        created: kpis.planned + kpis.reactive
      }),
      delta: (
        <DeltaBadge
          current={kpis.plannedRatio}
          previous={previous.data?.plannedRatio}
          previousPeriod={previous}
          higherIsBetter
          points
          formatValue={formatPercent}
        />
      )
    },
    {
      label: t('backlog'),
      value: kpis.backlog,
      detail: t('estimated_hours_value', {
        hours: roundDecimals(kpis.backlogHours)
      }),
      delta: (
        <DeltaBadge
          current={kpis.backlog}
          previous={previous.data?.backlog}
          previousPeriod={previous}
          higherIsBetter={false}
        />
      )
    },
    {
      label: t('overdue_work_orders'),
      value: kpis.overdue,
      detail: t('of_backlog', { backlog: kpis.backlog }),
      delta: (
        <DeltaBadge
          current={kpis.overdue}
          previous={previous.data?.overdue}
          previousPeriod={previous}
          higherIsBetter={false}
        />
      )
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
              {item.delta}
            </Stack>
          ))}
        </Stack>
      )}
    </AnalyticsCard>
  );
}

export default Overview;
