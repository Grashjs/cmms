import { useTheme } from '@mui/material';
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
import Loading from '../../Loading';
import { getRequestsByDate } from '../../../../../slices/analytics/request';
import { getDayAndMonth } from '../../../../../utils/dates';

interface OwnProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function RequestsByDate({ handleOpenModal, start, end }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { requestsByDate, loading } = useSelector(
    (state) => state.requestAnalytics
  );

  useEffect(() => {
    dispatch(getRequestsByDate(start, end));
  }, [start, end]);

  const formattedData: {
    label: string;
    cycleTime: string;
    filters: Filter[];
  }[] = requestsByDate.map((data) => {
    return {
      cycleTime: data.cycleTime.toFixed(2),
      label: getDayAndMonth(data.date),
      filters: [{ key: 'range', value: data.date }]
    };
  });
  const lines: { label: string; dataKey: string; color: string }[] = [
    {
      label: t('average_cycle_time_in_days'),
      dataKey: 'cycleTime',
      color: theme.colors.info.main
    }
  ];
  const title = t('requests_trends');
  return (
    <AnalyticsCard title={title}>
      {loading.requestsByDate ? (
        <Loading />
      ) : (
        <LineChart width={900} height={508} data={formattedData}>
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

export default RequestsByDate;
