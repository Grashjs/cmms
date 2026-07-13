import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getIncompleteStats } from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';

interface WOStatusNumbersProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void,
  start: Date,
  end: Date
}

function Overview({ handleOpenModal, start, end }: WOStatusNumbersProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { incompleteOverview, loading } = useSelector(
    (state) => state.woAnalytics
  );

  useEffect(() => {
    dispatch(getIncompleteStats(start, end));
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
      value: incompleteOverview.total,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('average_age'),
      value: incompleteOverview.averageAge,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    }
  ];
  return (
    <AnalyticsCard
      title={t('incomplete_wo')}
      height={200}
      description="wo_age_description"
    >
      <Stack sx={{ height: '100%', justifyContent: 'center' }}>
        {loading.incompleteOverview ? (
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
                      t('incomplete_wo')
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
