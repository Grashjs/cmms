import { Box, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
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

function BacklogTrend() {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dateLocale = useDateLocale();
  const { trend, loading } = useSelector((state) => state.kpiAnalytics);

  const data = trend.map((point) => ({
    label: format(new Date(point.date), 'dd MMM', { locale: dateLocale }),
    backlog: point.backlog,
    overdue: point.overdue
  }));

  return (
    <AnalyticsCard
      title={t('backlog_trend')}
      description="backlog_trend_description"
    >
      {loading.trend ? (
        <Loading />
      ) : (
        <Box sx={{ width: '100%' }}>
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={data}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="backlog"
                name={t('backlog')}
                stroke={theme.colors.primary.main}
                strokeWidth={2}
              />
              <Line
                type="monotone"
                dataKey="overdue"
                name={t('overdue_work_orders')}
                stroke={theme.colors.error.main}
                strokeWidth={2}
              />
            </LineChart>
          </ResponsiveContainer>
        </Box>
      )}
    </AnalyticsCard>
  );
}

export default BacklogTrend;
