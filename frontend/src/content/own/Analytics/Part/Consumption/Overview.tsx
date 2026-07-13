import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useContext, useEffect } from 'react';
import Loading from '../../Loading';
import { CompanySettingsContext } from '../../../../../contexts/CompanySettingsContext';
import { getPartConsumptionOverview } from '../../../../../slices/analytics/part';

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
  const { consumptionsOverview, loading } = useSelector(
    (state) => state.partAnalytics
  );
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  useEffect(() => {
    dispatch(getPartConsumptionOverview(start, end));
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
      label: t('total_consumption_cost'),
      value: getFormattedCurrency(consumptionsOverview.totalConsumptionCost),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('parts_consumed'),
      value: consumptionsOverview.consumedCount,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    }
  ];
  return (
    <AnalyticsCard title={t('the_numbers')} height={200}>
      <Stack sx={{ height: '100%', justifyContent: 'center' }}>
        {loading.consumptionsOverview ? (
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
