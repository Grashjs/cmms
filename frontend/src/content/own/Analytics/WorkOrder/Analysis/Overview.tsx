import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getOverviewStats } from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';

interface WOStatusNumbersProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function Overview({ handleOpenModal, start, end }: WOStatusNumbersProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { overview, loading } = useSelector((state) => state.woAnalytics);

  useEffect(() => {
    dispatch(getOverviewStats(start, end));
  }, [start, end]);

  const formattedData: {
    label: string;
    value: number | string;
    config?: {
      columns: string[];
      filters: Filter[];
    };
  }[] = [
    {
      label: t('count'),
      value: overview.complete,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('compliant'),
      value: overview.compliant,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('average_cycle_time_in_days'),
      value: overview.avgCycleTime
    },
    {
      label: t('compliance_rate'),
      value: `${
        overview.complete
          ? ((overview.compliant * 100) / overview.complete).toFixed(0)
          : 0
      }%`
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
              </Stack>
            ))}
          </Stack>
        )}
      </Stack>
    </AnalyticsCard>
  );
}

export default Overview;
