import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Cell, Legend, Pie, PieChart, Tooltip } from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getCompleteByPriority } from '../../../../../slices/analytics/workOrder';
import { getRandomColor } from '../../../../../utils/overall';
import { getPriorityLabel } from '../../../../../utils/formatters';
import Loading from '../../Loading';

interface WOByPrimaryUserProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function WOByPriority({ handleOpenModal, start, end }: WOByPrimaryUserProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { completeByPriority, loading } = useSelector(
    (state) => state.woAnalytics
  );

  useEffect(() => {
    dispatch(getCompleteByPriority(start, end));
  }, [start, end]);

  const columns = ['id'];

  const formattedData = Object.entries(completeByPriority).map(
    ([priority, count]) => {
      return {
        label: getPriorityLabel(priority, t),
        value: count,
        color: getRandomColor(),
        filters: [{ key: 'priority', value: priority }]
      };
    }
  );
  const title = t('grouped_by_priority');
  return (
    <AnalyticsCard title={title}>
      {loading.completeByPriority ? (
        <Loading />
      ) : (
        <PieChart width={200} height={300}>
          <Pie
            data={formattedData}
            dataKey="value"
            nameKey="label"
            cx="50%"
            cy="50%"
            outerRadius={100}
            innerRadius={50}
            fill="#8884d8"
          >
            {formattedData.map((entry, index) => (
              <Cell
                key={index}
                fill={entry.color}
                onClick={() => {
                  handleOpenModal(columns, entry.filters, title);
                }}
              />
            ))}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      )}
    </AnalyticsCard>
  );
}

export default WOByPriority;
