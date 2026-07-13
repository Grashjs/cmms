import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import Loading from '../../Loading';
import { getDayAndMonth } from '../../../../../utils/dates';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { getDowntimesMeantimeByDate } from '../../../../../slices/analytics/asset';

interface HoursWorkedProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function MeantimesTrends({ handleOpenModal, start, end }: HoursWorkedProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const theme = useTheme();
  const { downtimesMeantimeByDate, loading } = useSelector(
    (state) => state.assetAnalytics
  );

  useEffect(() => {
    dispatch(getDowntimesMeantimeByDate(start, end));
  }, [start, end]);

  const formattedData: {
    label: string;
    meantime: string;
    filters: Filter[];
  }[] = downtimesMeantimeByDate.map((data) => {
    return {
      meantime: data.meantime.toFixed(2),
      label: getDayAndMonth(data.date),
      filters: [{ key: 'range', value: data.date }]
    };
  });
  const lines: { label: string; dataKey: string; color: string }[] = [
    {
      label: t('meantime_between_failures_in_hours'),
      dataKey: 'meantime',
      color: theme.colors.primary.main
    }
  ];
  const title = t('downtimes_trends');
  return (
    <AnalyticsCard title={title}>
      {loading.downtimesMeantimeByDate ? (
        <Loading />
      ) : (
        <LineChart width={800} height={508} data={formattedData}>
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

export default MeantimesTrends;
