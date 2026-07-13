import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
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
import { getDowntimesByDate } from '../../../../../slices/analytics/asset';
import { getDayAndMonth } from '../../../../../utils/dates';

interface DowntimesByDateProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function DowntimesByDate({ handleOpenModal, start, end }: DowntimesByDateProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { downtimesByDate, loading } = useSelector(
    (state) => state.assetAnalytics
  );

  useEffect(() => {
    dispatch(getDowntimesByDate(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    duration: number;
    cost: string;
    color: string;
    filters: Filter[];
  }[] = downtimesByDate.map((month) => {
    return {
      label: getDayAndMonth(month.date),
      duration: Number((month.duration / 3600).toFixed(2)),
      cost: month.workOrdersCosts.toFixed(2),
      color: getRandomColor(),
      filters: [{ key: 'month', value: month.date }]
    };
  });
  const title = t('downtime_and_costs_trends');
  return (
    <AnalyticsCard title={title}>
      {loading.downtimesByDate ? (
        <Loading />
      ) : (
        <ComposedChart width={508} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis yAxisId="left-axis" />
          <YAxis yAxisId="right-axis"
                 orientation="right" unit={t('hours')} />
          <Tooltip />
          <Legend />
          <Bar dataKey="cost" fill="#8884d8" name={t('total_maintenance_cost')} yAxisId="left-axis">
            {formattedData.map((entry, index) => (
              <Cell
                key={index}
                fill={entry.color}
                onClick={() => {
                  handleOpenModal(columns, entry.filters, t(title));
                }}
              />
            ))}
          </Bar>
          <Line
            name={t('total_downtime_in_hours')}
            type="monotone"
            dataKey="duration"
            stroke="#ff7300"
            yAxisId="right-axis"
          />
        </ComposedChart>
      )}
    </AnalyticsCard>
  );
}

export default DowntimesByDate;
