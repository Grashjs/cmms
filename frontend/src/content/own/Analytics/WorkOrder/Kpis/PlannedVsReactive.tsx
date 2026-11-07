import { Box, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { format } from 'date-fns';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { useSelector } from '../../../../../store';
import useDateLocale from '../../../../../hooks/useDateLocale';

function PlannedVsReactive() {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dateLocale = useDateLocale();
  const { trend, loading } = useSelector((state) => state.kpiAnalytics);

  const data = trend.map((point) => ({
    label: format(new Date(point.date), 'dd MMM', { locale: dateLocale }),
    planned: point.planned,
    reactive: point.reactive
  }));

  return (
    <AnalyticsCard
      title={t('planned_vs_reactive')}
      description="planned_vs_reactive_description"
    >
      {loading.trend ? (
        <Loading />
      ) : (
        <Box sx={{ width: '100%' }}>
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={data}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar
                dataKey="planned"
                name={t('planned')}
                stackId="created"
                fill={theme.colors.primary.main}
              />
              <Bar
                dataKey="reactive"
                name={t('reactive_plural')}
                stackId="created"
                fill={theme.colors.warning.main}
              />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      )}
    </AnalyticsCard>
  );
}

export default PlannedVsReactive;
