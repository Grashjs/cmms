import { CircularProgress, Stack, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getCompleteTimesByWeek } from '../../../../../slices/analytics/workOrder';
import { getDayAndMonth } from '../../../../../utils/dates';

interface WOStatusIncompleteProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
}
function TimeByWeek({ handleOpenModal }: WOStatusIncompleteProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { completeTimesByWeek, loading } = useSelector(
    (state) => state.woAnalytics
  );
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getCompleteTimesByWeek());
  }, []);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    total: string;
    reactive: string;
    recurring: string;
    filters: Filter[];
  }[] = completeTimesByWeek.map((data) => {
    return {
      total: (data.total / 3600).toFixed(2),
      reactive: (data.reactive / 3600).toFixed(2),
      recurring: ((data.total - data.reactive) / 3600).toFixed(2),
      label: getDayAndMonth(data.date),
      filters: [{ key: 'range', value: data.date }]
    };
  });
  const title = t('time_spent');
  const lines: { label: string; dataKey: string; color: string }[] = [
    {
      label: t('complete'),
      dataKey: 'total',
      color: theme.colors.primary.main
    },
    {
      label: t('reactive'),
      dataKey: 'reactive',
      color: theme.colors.warning.main
    },
    {
      label: t('recurring'),
      dataKey: 'recurring',
      color: theme.colors.error.main
    }
  ];
  return (
    <AnalyticsCard title={title}>
      {loading.completeTimesByWeek ? (
        <Stack
          width="100%"
          height="100%"
          justifyContent="center"
          alignItems="center"
        >
          <CircularProgress />
        </Stack>
      ) : (
        <LineChart width={508} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis />
          <Tooltip />
          <Legend />
          {lines.map((line) => (
            <Line
              key={line.dataKey}
              name={line.label}
              type="monotone"
              dataKey={line.dataKey}
              stroke={line.color}
            />
          ))}
        </LineChart>
      )}
    </AnalyticsCard>
  );
}

export default TimeByWeek;
