import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getRandomColor } from '../../../../../utils/overall';
import Loading from '../../Loading';
import { getDayAndMonth } from '../../../../../utils/dates';
import { getStatusesByDate } from '../../../../../slices/analytics/workOrder';

interface DowntimesByMonthProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function StatusByDate({ handleOpenModal, start, end }: DowntimesByMonthProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { statusesByDate, loading } = useSelector(
    (state) => state.woAnalytics
  );

  useEffect(() => {
    const localStart = new Date(end);
    localStart.setDate(localStart.getDate() - 7);
    dispatch(getStatusesByDate(start > localStart ? start : localStart, end));
  }, [end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    open: number;
    onHold: number;
    inProgress: number;
    complete: number;
    filters: Filter[];
  }[] = statusesByDate.map((stat) => {
    return {
      label: getDayAndMonth(stat.date),
      open: stat.open,
      onHold: stat.onHold,
      inProgress: stat.inProgress,
      complete: stat.complete,
      filters: [{ key: 'month', value: stat.date }]
    };
  });
  const title = t('wo_status');
  return (
    <AnalyticsCard title={title}>
      {loading.statusesByDate ? (
        <Loading />
      ) : (
        <BarChart width={900} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey="complete" fill={theme.colors.primary.main} name={t('COMPLETE')} stackId={'a'} />
          <Bar dataKey="onHold" fill={theme.colors.warning.main} name={t('ON_HOLD')} stackId={'a'} />
          <Bar dataKey="inProgress" fill={theme.colors.success.main} name={t('IN_PROGRESS')} stackId={'a'} />
          <Bar dataKey="open" fill={theme.colors.alpha.black[70]} name={t('OPEN')} stackId={'a'} />
        </BarChart>
      )}
    </AnalyticsCard>
  );
}

export default StatusByDate;
