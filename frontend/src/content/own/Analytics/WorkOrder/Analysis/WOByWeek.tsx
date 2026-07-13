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
import { getCompleteByWeek } from '../../../../../slices/analytics/workOrder';
import { getDayAndMonth } from '../../../../../utils/dates';

interface WOStatusIncompleteProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
}
function WOByWeek({ handleOpenModal }: WOStatusIncompleteProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { completeByWeek, loading } = useSelector((state) => state.woAnalytics);
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getCompleteByWeek());
  }, []);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    count: number;
    filters: Filter[];
  }[] = completeByWeek.map((data) => {
    return {
      ...data,
      recurring: data.count - data.reactive,
      label: getDayAndMonth(data.date),
      filters: [{ key: 'range', value: data.date }]
    };
  });
  const title = t('completion_comparison');
  const lines: { label: string; dataKey: string; color: string }[] = [
    {
      label: t('complete'),
      dataKey: 'count',
      color: theme.colors.primary.main
    },
    {
      label: t('compliant'),
      dataKey: 'compliant',
      color: theme.colors.success.main
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
    <AnalyticsCard
      title={title}
      description="completion_comparison_description"
    >
      {loading.completeByWeek ? (
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

export default WOByWeek;
