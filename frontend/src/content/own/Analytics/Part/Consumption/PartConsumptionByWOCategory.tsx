import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Cell, Legend, Pie, PieChart, Tooltip } from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import { getCompleteByCategory } from '../../../../../slices/analytics/workOrder';
import { getRandomColor } from '../../../../../utils/overall';
import Loading from '../../Loading';
import {
  getPartConsumptionsByPartCategory,
  getPartConsumptionsByWOCategory
} from '../../../../../slices/analytics/part';

interface OwnProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function PartConsumptionByWOCategory({ handleOpenModal, start, end }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { partConsumptionByWOCategory, loading } = useSelector(
    (state) => state.partAnalytics
  );

  useEffect(() => {
    dispatch(getPartConsumptionsByWOCategory(start, end));
  }, [start, end]);

  const columns = ['id'];

  const formattedData = partConsumptionByWOCategory.map((category) => {
    return {
      label: category.name,
      value: category.cost,
      color: getRandomColor(),
      filters: [{ key: 'category', value: category.id }]
    };
  });
  const title = t('grouped_by_wo_category');
  return (
    <AnalyticsCard title={title}>
      {loading.partConsumptionByWOCategory ? (
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

export default PartConsumptionByWOCategory;
