import { Box, Typography, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { MeterAnalytics } from '../../../../../models/owns/analytics/meter';
import useMeterFormat from './format';

interface TrendChartProps {
  analytics: MeterAnalytics;
  loading: boolean;
}

function TrendChart({ analytics, loading }: TrendChartProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { formatValue, formatLabel } = useMeterFormat(analytics.unit);

  const data = analytics.points.map((point) => ({
    label: formatLabel(point.start, analytics.interval),
    range: point.min === null ? null : [point.min, point.max],
    average: point.average,
    last: point.last,
    outOfRange: point.outOfRange
  }));
  // cumulative meters: the value of the counter, the consumption is in its own chart
  const valueKey = analytics.cumulative ? 'last' : 'average';

  // marks the intervals with readings out of range
  const renderDot = (props: any) => {
    const { cx, cy, payload, index } = props;
    if (cx === undefined || cy === undefined || !payload?.outOfRange) {
      return <g key={`dot-${index}`} />;
    }
    return (
      <circle
        key={`dot-${index}`}
        cx={cx}
        cy={cy}
        r={4}
        fill={theme.colors.error.main}
        stroke="white"
        strokeWidth={1}
      />
    );
  };

  return (
    <AnalyticsCard
      title={t('meter_trend')}
      description={
        analytics.cumulative
          ? 'meter_counter_trend_description'
          : 'meter_trend_description'
      }
    >
      {loading ? (
        <Loading />
      ) : analytics.readings === 0 ? (
        <Typography color="text.secondary" sx={{ py: 6 }}>
          {t('no_readings_period')}
        </Typography>
      ) : (
        <Box sx={{ width: '100%' }}>
          <ResponsiveContainer width="100%" height={340}>
            <ComposedChart data={data}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" minTickGap={16} />
              <YAxis
                domain={['auto', 'auto']}
                tickFormatter={(value) => formatValue(value)}
                width={90}
              />
              <Tooltip
                formatter={(value: any) =>
                  Array.isArray(value)
                    ? `${formatValue(value[0])} – ${formatValue(value[1])}`
                    : formatValue(value)
                }
              />
              <Legend />
              {!analytics.cumulative && (
                <Area
                  dataKey="range"
                  name={t('min_max')}
                  stroke="none"
                  fill={theme.colors.primary.light}
                  fillOpacity={0.25}
                  legendType="square"
                  isAnimationActive={false}
                />
              )}
              <Line
                type="monotone"
                dataKey={valueKey}
                name={analytics.cumulative ? t('value') : t('average')}
                stroke={theme.colors.primary.main}
                strokeWidth={2}
                dot={renderDot}
                connectNulls
              />
              {analytics.thresholds.map((threshold) => (
                <ReferenceLine
                  key={`${threshold.name}-${threshold.condition}-${threshold.value}`}
                  y={threshold.value}
                  stroke={theme.colors.error.main}
                  strokeDasharray="6 4"
                  ifOverflow="extendDomain"
                  label={{
                    value: `${threshold.name} (${
                      threshold.condition === 'LESS_THAN' ? '<' : '>'
                    } ${formatValue(threshold.value)})`,
                    position:
                      threshold.condition === 'LESS_THAN'
                        ? 'insideBottomRight'
                        : 'insideTopRight',
                    fill: theme.colors.error.main,
                    fontSize: 12
                  }}
                />
              ))}
            </ComposedChart>
          </ResponsiveContainer>
        </Box>
      )}
    </AnalyticsCard>
  );
}

export default TrendChart;
