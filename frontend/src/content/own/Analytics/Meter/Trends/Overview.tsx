import { ReactNode } from 'react';
import { Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import DeltaBadge from '../../DeltaBadge';
import { PreviousPeriod } from '../../usePreviousPeriod';
import { MeterAnalytics } from '../../../../../models/owns/analytics/meter';
import useMeterFormat from './format';

interface OverviewProps {
  analytics: MeterAnalytics;
  loading: boolean;
  previous: PreviousPeriod<MeterAnalytics>;
}

function Overview({ analytics, loading, previous }: OverviewProps) {
  const { t }: { t: any } = useTranslation();
  const { formatNumber, formatValue, formatDate } = useMeterFormat(
    analytics.unit
  );

  const readings = {
    label: t('readings'),
    value: formatNumber(analytics.readings),
    detail: analytics.lastReadingAt
      ? t('last_reading_value', {
          value: formatValue(analytics.lastValue),
          date: formatDate(analytics.lastReadingAt)
        })
      : t('no_readings_period'),
    delta: (
      <DeltaBadge
        current={analytics.readings}
        previous={previous.data?.readings}
        previousPeriod={previous}
      />
    )
  };
  const outOfRange = {
    label: t('out_of_range_readings'),
    value: analytics.thresholds.length
      ? formatNumber(analytics.outOfRange)
      : '—',
    detail: analytics.thresholds.length
      ? t('of_readings', { count: analytics.readings })
      : t('no_meter_thresholds'),
    delta: analytics.thresholds.length ? (
      <DeltaBadge
        current={analytics.outOfRange}
        previous={previous.data?.outOfRange}
        previousPeriod={previous}
        higherIsBetter={false}
      />
    ) : null
  };
  const items: {
    label: string;
    value: string;
    detail: string;
    delta: ReactNode;
  }[] = analytics.cumulative
    ? [
        readings,
        {
          label: t('consumption'),
          value: formatValue(analytics.consumption),
          detail: analytics.resets
            ? t('counter_resets', { count: analytics.resets })
            : '',
          delta: (
            <DeltaBadge
              current={analytics.consumption}
              previous={previous.data?.consumption}
              previousPeriod={previous}
              formatValue={formatValue}
            />
          )
        },
        {
          label: t('consumption_between_failures'),
          value: formatValue(analytics.consumptionBetweenFailures),
          detail: analytics.failures
            ? t('asset_downtimes_count', {
                count: analytics.failures,
                asset: analytics.assetName
              })
            : t('no_asset_downtimes'),
          delta: null
        },
        outOfRange
      ]
    : [
        readings,
        {
          label: t('average'),
          value: formatValue(analytics.average),
          detail:
            analytics.min === null
              ? ''
              : t('min_max_values', {
                  min: formatValue(analytics.min),
                  max: formatValue(analytics.max)
                }),
          delta: (
            <DeltaBadge
              current={analytics.average}
              previous={previous.data?.average}
              previousPeriod={previous}
              formatValue={formatValue}
            />
          )
        },
        outOfRange
      ];

  return (
    <AnalyticsCard
      title={t('meter_readings_overview')}
      description="meter_readings_overview_description"
    >
      {loading ? (
        <Loading />
      ) : (
        <Stack
          direction="row"
          justifyContent="center"
          flexWrap="wrap"
          sx={{ gap: 4 }}
        >
          {items.map((item) => (
            <Stack key={item.label} alignItems="center" maxWidth={260}>
              <Typography variant="h2" fontWeight="bold">
                {item.value}
              </Typography>
              <Typography>{item.label}</Typography>
              <Typography
                variant="body2"
                color="text.secondary"
                textAlign="center"
              >
                {item.detail}
              </Typography>
              {item.delta}
            </Stack>
          ))}
        </Stack>
      )}
    </AnalyticsCard>
  );
}

export default Overview;
