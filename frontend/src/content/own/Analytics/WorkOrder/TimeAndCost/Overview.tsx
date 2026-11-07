import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { ReactNode, useContext, useEffect } from 'react';
import {
  fetchCompleteCosts,
  getCompleteCosts
} from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';
import { CompanySettingsContext } from '../../../../../contexts/CompanySettingsContext';
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
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const { completeCosts, loading } = useSelector((state) => state.woAnalytics);

  useEffect(() => {
    dispatch(getCompleteCosts(start, end, companyId, filterFields));
  }, [start, end, companyId, filterFields]);
  const previous = usePreviousPeriod(fetchCompleteCosts, start, end, companyId, filterFields);

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
      label: t('total_cost'),
      value: getFormattedCurrency(completeCosts.total.toFixed(2)),
      delta: (
        <DeltaBadge
          current={completeCosts.total}
          previous={previous.data?.total}
          previousPeriod={previous}
          higherIsBetter={false}
          formatValue={(value) => getFormattedCurrency(value.toFixed(2))}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('part_costs'),
      value: getFormattedCurrency(completeCosts.partCost.toFixed(2)),
      delta: (
        <DeltaBadge
          current={completeCosts.partCost}
          previous={previous.data?.partCost}
          previousPeriod={previous}
          higherIsBetter={false}
          formatValue={(value) => getFormattedCurrency(value.toFixed(2))}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('labor_costs'),
      value: getFormattedCurrency(completeCosts.laborCost.toFixed(2)),
      delta: (
        <DeltaBadge
          current={completeCosts.laborCost}
          previous={previous.data?.laborCost}
          previousPeriod={previous}
          higherIsBetter={false}
          formatValue={(value) => getFormattedCurrency(value.toFixed(2))}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('additional_costs'),
      value: getFormattedCurrency(completeCosts.additionalCost.toFixed(2)),
      delta: (
        <DeltaBadge
          current={completeCosts.additionalCost}
          previous={previous.data?.additionalCost}
          previousPeriod={previous}
          higherIsBetter={false}
          formatValue={(value) => getFormattedCurrency(value.toFixed(2))}
        />
      ),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('hours_worked'),
      value: (completeCosts.laborTime / 3600).toFixed(2),
      delta: (
        <DeltaBadge
          current={completeCosts.laborTime}
          previous={previous.data?.laborTime}
          previousPeriod={previous}
          formatValue={(value) => (value / 3600).toFixed(2)}
        />
      ),
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
