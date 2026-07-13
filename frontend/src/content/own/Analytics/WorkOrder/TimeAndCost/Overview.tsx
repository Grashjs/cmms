import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useContext, useEffect } from 'react';
import { getCompleteCosts } from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';
import { CompanySettingsContext } from '../../../../../contexts/CompanySettingsContext';

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
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const { completeCosts, loading } = useSelector((state) => state.woAnalytics);

  useEffect(() => {
    dispatch(getCompleteCosts(start, end));
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
      label: t('total_cost'),
      value: getFormattedCurrency(completeCosts.total.toFixed(2)),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('part_costs'),
      value: getFormattedCurrency(completeCosts.partCost.toFixed(2)),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('labor_costs'),
      value: getFormattedCurrency(completeCosts.laborCost.toFixed(2)),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('additional_costs'),
      value: getFormattedCurrency(completeCosts.additionalCost.toFixed(2)),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('hours_worked'),
      value: (completeCosts.laborTime / 3600).toFixed(2),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    }
  ];
  return (
    <AnalyticsCard
      title={t('costs')}
      height={200}
      description="Associated costs and time relate to work orders that were completed within the time frame specified in the filters."
    >
      <Stack sx={{ height: '100%', justifyContent: 'center' }}>
        {loading.completeCosts ? (
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
