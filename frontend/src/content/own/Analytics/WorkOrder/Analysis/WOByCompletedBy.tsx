import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Cell, Legend, Pie, PieChart, Tooltip } from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getCompleteByCompletedBy } from '../../../../../slices/analytics/workOrder';
import { getRandomColor } from '../../../../../utils/overall';
import Loading from '../../Loading';
import { FilterField } from '../../../../../models/owns/page';

interface WOByPrimaryUserProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
  companyId?: number;
  filterFields?: FilterField[];
}

function WOByCompletedBy({ handleOpenModal, start, end, companyId, filterFields }: WOByPrimaryUserProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { completeByCompletedBy, loading } = useSelector(
    (state) => state.woAnalytics
  );

  useEffect(() => {
    dispatch(getCompleteByCompletedBy(start, end, companyId, filterFields));
  }, [start, end, companyId, filterFields]);

  const columns = ['id'];

  const formattedData = completeByCompletedBy.map((user) => {
    return {
      label: `${user.firstName} ${user.lastName}`,
      value: user.count,
      color: getRandomColor(),
      filters: [{ key: 'completedBy', value: user.id }]
    };
  });
  const title = t('grouped_by_completed_by');
  return (
    <AnalyticsCard title={title}>
      {loading.completeByCompletedBy ? (
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
        </PieChart>
      )}
    </AnalyticsCard>
  );
}

export default WOByCompletedBy;
