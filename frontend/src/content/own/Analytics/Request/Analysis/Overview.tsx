import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { ReactNode, useEffect } from 'react';
import Loading from '../../Loading';
import { roundDecimals } from '../../../../../utils/formatters';
import {
  fetchRequestOverview,
  getRequestOverview
} from '../../../../../slices/analytics/request';
import { FilterField } from '../../../../../models/owns/page';
import DeltaBadge from '../../DeltaBadge';
import usePreviousPeriod from '../../usePreviousPeriod';

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
  const previous = usePreviousPeriod(fetchRequestOverview, start, end, companyId, filterFields);

  const totalCount = overview.approved + overview.pending + overview.cancelled;

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
      label: t('approved_requests') + ` (${(overview.approved * 100 / totalCount).toFixed(0)}%)`,
      value: overview.approved,
      delta: (
        <DeltaBadge
          current={overview.approved}
          previous={previous.data?.approved}
          previousPeriod={previous}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('pending_requests') + ` (${(overview.pending * 100 / totalCount).toFixed(0)}%)`,
      value: overview.pending,
      delta: (
        <DeltaBadge
          current={overview.pending}
          previous={previous.data?.pending}
          previousPeriod={previous}
          higherIsBetter={false}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('cancelled_requests') + ` (${(overview.cancelled * 100 / totalCount).toFixed(0)}%)`,
      value: overview.cancelled,
      delta: (
        <DeltaBadge
          current={overview.cancelled}
          previous={previous.data?.cancelled}
          previousPeriod={previous}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('average_cycle_time'),
      value: roundDecimals(overview.cycleTime),
      delta: (
        <DeltaBadge
          current={overview.cycleTime}
          previous={previous.data?.cycleTime}
          previousPeriod={previous}
          higherIsBetter={false}
          formatValue={roundDecimals}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('total'),
      value: totalCount,
      delta: (
        <DeltaBadge
          current={totalCount}
          previous={previous.data && previous.data.approved + previous.data.pending + previous.data.cancelled}
          previousPeriod={previous}
        />
      ),
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
