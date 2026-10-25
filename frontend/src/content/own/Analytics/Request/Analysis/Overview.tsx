import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import Loading from '../../Loading';
import { roundDecimals } from '../../../../../utils/formatters';
import { getRequestOverview } from '../../../../../slices/analytics/request';
import { FilterField } from '../../../../../models/owns/page';

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
  const { overview, loading } = useSelector((state) => state.requestAnalytics);
  useEffect(() => {
    dispatch(getRequestOverview(start, end, companyId, filterFields));
  }, [start, end, companyId, filterFields]);

  const totalCount = overview.approved + overview.pending + overview.cancelled;

  const formattedData: {
    label: string;
    value: number | string;
    config?: {
      columns: string[];
      filters: Filter[];
    };
  }[] = [
    {
      label: t('approved_requests') + ` (${(overview.approved * 100 / totalCount).toFixed(0)}%)`,
      value: overview.approved,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('pending_requests') + ` (${(overview.pending * 100 / totalCount).toFixed(0)}%)`,
      value: overview.pending,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('cancelled_requests') + ` (${(overview.cancelled * 100 / totalCount).toFixed(0)}%)`,
      value: overview.cancelled,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('average_cycle_time'),
      value: roundDecimals(overview.cycleTime),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('total'),
      value: totalCount,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    }
  ];
  return (
    <AnalyticsCard title={t('the_numbers')} height={200}>
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
