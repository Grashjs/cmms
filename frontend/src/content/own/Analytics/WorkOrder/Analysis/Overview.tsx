import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { ReactNode, useEffect } from 'react';
import {
  fetchOverviewStats,
  getOverviewStats
} from '../../../../../slices/analytics/workOrder';
import { WoOverviewStats } from '../../../../../models/owns/analytics/workOrder';
import Loading from '../../Loading';
import { roundDecimals } from '../../../../../utils/formatters';
import { FilterField } from '../../../../../models/owns/page';
import DeltaBadge from '../../DeltaBadge';
import usePreviousPeriod from '../../usePreviousPeriod';

// percent of the completed work orders that were compliant; null without completed work orders
const complianceRate = (stats: WoOverviewStats): number | null =>
  stats.complete ? (stats.compliant * 100) / stats.complete : null;

interface WOStatusNumbersProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
  companyId?: number;
  filterFields?: FilterField[];
}

function Overview({ handleOpenModal, start, end, companyId, filterFields }: WOStatusNumbersProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { overview, loading } = useSelector((state) => state.woAnalytics);

  useEffect(() => {
    dispatch(getOverviewStats(start, end, companyId, filterFields));
  }, [start, end, companyId, filterFields]);
  const previous = usePreviousPeriod(fetchOverviewStats, start, end, companyId, filterFields);

  const formattedData: {
    label: string;
    value: number | string;
    delta?: ReactNode;
    config?: {
      columns: string[];
      filters: Filter[];
    };
  }[] = [
    {
      label: t('count'),
      value: overview.complete,
      delta: (
        <DeltaBadge
          current={overview.complete}
          previous={previous.data?.complete}
          previousPeriod={previous}
          higherIsBetter
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('compliant'),
      value: overview.compliant,
      delta: (
        <DeltaBadge
          current={overview.compliant}
          previous={previous.data?.compliant}
          previousPeriod={previous}
          higherIsBetter
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('average_cycle_time_in_days'),
      value: roundDecimals(overview.avgCycleTime),
      delta: (
        <DeltaBadge
          current={overview.avgCycleTime}
          previous={previous.data?.avgCycleTime}
          previousPeriod={previous}
          higherIsBetter={false}
          formatValue={roundDecimals}
        />
      )
    },
    {
      label: t('compliance_rate'),
      value: `${
        overview.complete
          ? ((overview.compliant * 100) / overview.complete).toFixed(0)
          : 0
      }%`,
      delta: (
        <DeltaBadge
          current={complianceRate(overview)}
          previous={previous.data && complianceRate(previous.data)}
          previousPeriod={previous}
          higherIsBetter
          points
          formatValue={(value) => `${Math.round(value)}%`}
        />
      )
    }
  ];
  return (
    <AnalyticsCard
      title={t('the_numbers')}
      height={200}
      description="compliant_wo_description"
    >
      <Stack sx={{ height: '100%', justifyContent: 'center' }}>
        {loading.overview ? (
          <Loading />
        ) : (
          <Stack direction="row" spacing={2}>
            {formattedData.map((data) => (
              <Stack key={data.label} alignItems="center">
                <Typography
                  variant="h2"
                  fontWeight="bold"
                  style={{ cursor: data.config ? 'pointer' : 'auto' }}
                  onClick={() =>
                    handleOpenModal(
                      data.config.columns,
                      data.config.filters,
                      t('the_numbers')
                    )
                  }
                >
                  {data.value}
                </Typography>
                <Typography>{data.label}</Typography>
                {data.delta}
              </Stack>
            ))}
          </Stack>
        )}
      </Stack>
    </AnalyticsCard>
  );
}

export default Overview;
