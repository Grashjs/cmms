import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Cell, Legend, Pie, PieChart, Tooltip } from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useEffect } from 'react';
import Loading from '../../Loading';
import { getRequestStatsByPriority } from '../../../../../slices/analytics/request';

interface OwnProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function RequestStatsByPriority({ handleOpenModal, start, end }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { requestStatsByPriority, loading } = useSelector(
    (state) => state.requestAnalytics
  );

  useEffect(() => {
    dispatch(getRequestStatsByPriority(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    count: number;
    color: string;
    filters: Filter[];
  }[] = [
    {
      label: t('high_priority'),
      count: requestStatsByPriority.high.count,
      color: theme.colors.error.main,
      filters: [{ key: 'fs', value: false }]
    },
    {
      label: t('medium_priority'),
      count: requestStatsByPriority.medium.count,
      color: theme.colors.warning.main,
      filters: [{ key: 'fs', value: false }]
    },
    {
      label: t('low_priority'),
      count: requestStatsByPriority.low.count,
      color: theme.colors.success.main,
      filters: [{ key: 'fs', value: false }]
    },
    {
      label: t('none_priority'),
      count: requestStatsByPriority.none.count,
      color: theme.colors.alpha.black[70],
      filters: [{ key: 'fs', value: false }]
    }
  ];
  const title = t('requests_by_priority');
  return (
    <AnalyticsCard title={title}>
      {loading.requestStatsByPriority ? (
        <Loading />
      ) : (
        <PieChart width={200} height={300}>
          <Pie
            data={formattedData}
            dataKey="count"
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

export default RequestStatsByPriority;
