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

  const datas: {
    label: string;
    value: number;
    config?: {
      columns: string[];
      filters: Filter[];
    };
  }[] = [
    {
      label: t('count'),
      value: overview.total,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('complete'),
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
      label: t('MTTA'),
      value: overview.mtta
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
            {datas.map((data) => (
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
