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
import { getPartConsumptionsByDate } from '../../../../../slices/analytics/part';

interface DowntimesByMonthProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function PartConsumptionsByDate({ handleOpenModal, start, end }: DowntimesByMonthProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { partConsumptionsByDate, loading } = useSelector(
    (state) => state.partAnalytics
  );

  useEffect(() => {
    dispatch(getPartConsumptionsByDate(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    cost: number;
    color: string;
    filters: Filter[];
  }[] = partConsumptionsByDate.map((month) => {
    return {
      label: getDayAndMonth(month.date),
      cost: Number(month.cost.toFixed(2)),
      color: getRandomColor(),
      filters: [{ key: 'month', value: month.date }]
    };
  });
  const title = t('consumed_parts_costs');
  return (
    <AnalyticsCard title={title}>
      {loading.partConsumptionsByDate ? (
        <Loading />
      ) : (
        <BarChart width={900} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey="cost" fill="#8884d8" name={t('cost')}>
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
        </BarChart>
      )}
    </AnalyticsCard>
  );
}

export default PartConsumptionsByDate;
