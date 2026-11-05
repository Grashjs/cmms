import { Box, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { MeterAnalytics } from '../../../../../models/owns/analytics/meter';
import useMeterFormat from './format';

interface ConsumptionChartProps {
  analytics: MeterAnalytics;
  loading: boolean;
}

function ConsumptionChart({ analytics, loading }: ConsumptionChartProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { formatValue, formatLabel } = useMeterFormat(analytics.unit);

  const data = analytics.points.map((point) => ({
    label: formatLabel(point.start, analytics.interval),
    consumption: point.consumption ?? 0
  }));

  return (
    <AnalyticsCard
      title={t('consumption_per_period')}
      description="consumption_per_period_description"
    >
      {loading ? (
        <Loading />
      ) : (
        <Box sx={{ width: '100%' }}>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={data}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" minTickGap={16} />
              <YAxis tickFormatter={(value) => formatValue(value)} width={90} />
              <Tooltip formatter={(value: any) => formatValue(value)} />
              <Bar
                dataKey="consumption"
                name={t('consumption')}
                fill={theme.colors.primary.main}
                radius={[4, 4, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      )}
    </AnalyticsCard>
  );
}

export default ConsumptionChart;
