import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell, ComposedChart,
  Legend, Line,
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
import { getPartConsumptionsByPart } from '../../../../../slices/analytics/part';

interface DowntimesByMonthProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function PartConsumptionsPareto({ handleOpenModal, start, end }: DowntimesByMonthProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { partConsumptionsByPart, loading } = useSelector(
    (state) => state.partAnalytics
  );

  useEffect(() => {
    dispatch(getPartConsumptionsByPart(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    cost: number;
    cumulativePercent: number;
    color: string;
    filters: Filter[];
  }[] = partConsumptionsByPart.map((part, index, array) => {
    return {
      label: part.name,
      cost: Number(part.cost.toFixed(2)),
      cumulativePercent: 100 * array.slice(0, index).reduce((prev, curr) => prev + curr.cost, 0)
        / array.reduce((prev, curr) => prev + curr.cost, 0),
      color: getRandomColor(),
      filters: [{ key: 'part', value: part.id }]
    };
  });
  const title = t('pareto_by_total_price');
  return (
    <AnalyticsCard title={title}>
      {loading.partConsumptionsByPart ? (
        <Loading />
      ) : (
        <ComposedChart width={508} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis yAxisId={'left-axis'} type={'number'} domain={['dataMin', 'dataMax']} />
          <YAxis yAxisId="right-axis" orientation="right" type={'number'} unit={'%'} />
          <Tooltip />
          <Legend />
          <Bar dataKey="cost" fill="#8884d8" name={t('cost')} yAxisId={'left-axis'}>
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
            name={t('cumulative_percent')}
            type="monotone"
            dataKey="cumulativePercent"
            stroke="#ff7300"
            yAxisId="right-axis"
          />
        </ComposedChart>
      )}
    </AnalyticsCard>
  );
}

export default PartConsumptionsPareto;
