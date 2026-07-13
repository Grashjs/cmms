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
import { getCompleteCostsByDate } from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';
import { getDayAndMonth } from '../../../../../utils/dates';

interface WOStatusIncompleteProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function CompleteCostsByDate({ handleOpenModal, start, end }: WOStatusIncompleteProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { completeCostsByDate, loading } = useSelector(
    (state) => state.woAnalytics
  );
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getCompleteCostsByDate(start, end));
  }, [start, end]);

  const formattedData: {
    label: string;
    partCost: number;
    additionalCost: number;
    laborCost: number;
    filters: Filter[];
  }[] = completeCostsByDate.map((data) => {
    return {
      partCost: Number(data.partCost.toFixed(2)),
      additionalCost: Number(data.additionalCost.toFixed(2)),
      laborCost: Number(data.laborCost.toFixed(2)),
      label: getDayAndMonth(data.date),
      filters: [{ key: 'range', value: data.date }]
    };
  });
  const lines: { label: string; dataKey: string; color: string }[] = [
    {
      label: t('part_costs'),
      dataKey: 'partCost',
      color: theme.colors.primary.main
    },
    {
      label: t('additional_costs'),
      dataKey: 'additionalCost',
      color: theme.colors.warning.main
    },
    {
      label: t('labor_costs'),
      dataKey: 'laborCost',
      color: theme.colors.error.main
    }
  ];
  const title = t('cost_trends');
  return (
    <AnalyticsCard title={title}>
      {loading.completeCostsByDate ? (
        <Loading />
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

export default CompleteCostsByDate;
