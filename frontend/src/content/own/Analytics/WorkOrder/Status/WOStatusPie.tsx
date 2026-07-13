import { useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Cell, Legend, Pie, PieChart, Tooltip } from 'recharts';
import AnalyticsCard from '../../AnalyticsCard';
import { Filter } from '../WOModal';
import { useDispatch, useSelector } from '../../../../../store';
import { useCallback, useEffect } from 'react';
import { getIncompleteByStatus } from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';

interface WOStatusPieProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function WOStatusPie({ handleOpenModal, start, end }: WOStatusPieProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { incompleteByStatus, loading } = useSelector(
    (state) => state.woAnalytics
  );
  const RADIAN = Math.PI / 180;

  useEffect(() => {
    dispatch(getIncompleteByStatus(start, end));
  }, [start, end]);

  const columns = ['id'];
  const formattedData = [
    // {
    //   label: 'Complete',
    //   value: data.complete,
    //   color: theme.colors.error.main,
    //   filters: [{ key: 'status', value: 'COMPLETE' }]
    // },ne
    {
      label: t('ON_HOLD'),
      value: incompleteByStatus.onHold,
      color: theme.colors.warning.main,
      filters: [{ key: 'status', value: 'ON_HOLD' }]
    },
    {
      label: t('IN_PROGRESS'),
      value: incompleteByStatus.inProgress,
      color: theme.colors.success.main,
      filters: [{ key: 'status', value: 'IN_PROGRESS' }]
    },
    {
      label: t('OPEN'),
      value: incompleteByStatus.open,
      color: theme.colors.alpha.black[70],
      filters: [{ key: 'status', value: 'OPEN' }]
    }
  ];
  const title = t('wo_status');
  const renderCustomizedLabel = useCallback(({ cx, cy, midAngle, innerRadius, outerRadius, percent, index }) => {
    const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
      <text x={x} y={y} fill="white" textAnchor={x > cx ? 'start' : 'end'} dominantBaseline="central">
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    );
  }, [formattedData]);
  return (
    <AnalyticsCard title={title}>
      {loading.incompleteByStatus ? (
        <Loading />
      ) : (
        <PieChart width={200} height={300}>
          <Pie
            data={formattedData}
            dataKey="value"
            nameKey="label"
            label={renderCustomizedLabel}
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

export default WOStatusPie;
