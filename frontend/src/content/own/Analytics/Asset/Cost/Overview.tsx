import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useContext, useEffect } from 'react';
import Loading from '../../Loading';
import { getAssetsCosts } from '../../../../../slices/analytics/asset';
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
  const { assetsCosts, loading } = useSelector((state) => state.assetAnalytics);
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  useEffect(() => {
    dispatch(getAssetsCosts(start, end));
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
      label: t('total_cost_as_pct_rav'),
      value: `${assetsCosts.rav}%`,
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('total_maintenance_cost'),
      value: getFormattedCurrency(assetsCosts.totalWOCosts),
      config: {
        columns: ['id'],
        filters: [{ key: 'fs', value: false }]
      }
    },
    {
      label: t('total_purchase_price'),
      value: getFormattedCurrency(assetsCosts.totalAcquisitionCost)
    }
  ];
  return (
    <AnalyticsCard
      title={t('cost_center')}
      height={200}
      description="rav_description"
    >
      <Stack sx={{ height: '100%', justifyContent: 'center' }}>
        {loading.assetsCosts ? (
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
                      t('The numbers')
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
