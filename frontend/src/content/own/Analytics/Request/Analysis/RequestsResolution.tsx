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
import { getIncompleteByAsset } from '../../../../../slices/analytics/workOrder';
import Loading from '../../Loading';
import { getRandomColor } from '../../../../../utils/overall';
import { getRequestsResolvedByDate } from '../../../../../slices/analytics/request';
import { getDayAndMonth } from '../../../../../utils/dates';

interface OwnProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void,
  start: Date,
  end: Date
}

function RequestsResolution({ handleOpenModal, start, end }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { requestsResolvedByDate, loading } = useSelector(
    (state) => state.requestAnalytics
  );
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getRequestsResolvedByDate(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    received: number;
    resolved: number;
    resolutionRate: number;
    filters: Filter[];
  }[] = requestsResolvedByDate.map((stat) => {
    return {
      label: getDayAndMonth(stat.date),
      received: stat.received,
      resolved: stat.resolved,
      resolutionRate: Number(stat.received ? (stat.resolved / stat.received * 100).toFixed(2) : '0'),
      filters: []
    };
  });
  const title = t('resolution_rate');
  return (
    <AnalyticsCard title={title}>
      {loading.requestsResolvedByDate ? (
        <Loading />
      ) : (
        <ComposedChart width={508} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis yAxisId="left-axis" />
          <YAxis yAxisId="right-axis"
                 orientation="right" unit={'%'} />
          <Tooltip />
          <Legend />
          <Bar dataKey="received" fill="red" name={t('count')} yAxisId="left-axis">
            {formattedData.map((entry, index) => (
              <Cell
                key={index}
                fill={'red'}
                onClick={() => {
                  handleOpenModal(columns, entry.filters, t(title));
                }}
              />
            ))}
          </Bar>
          <Bar dataKey="resolved" fill="blue" name={t('resolved')} yAxisId="left-axis">
            {formattedData.map((entry, index) => (
              <Cell
                key={index}
                fill={'blue'}
                onClick={() => {
                  handleOpenModal(columns, entry.filters, t(title));
                }}
              />
            ))}
          </Bar>
          <Line
            name={t('resolution_rate')}
            type="monotone"
            dataKey="resolutionRate"
            stroke="#ff7300"
            yAxisId="right-axis"
          />
        </ComposedChart>
      )}
    </AnalyticsCard>
  );
}

export default RequestsResolution;
