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
import Loading from '../../Loading';
import { getRandomColor } from '../../../../../utils/overall';
import { getWOTimeCostByAsset } from '../../../../../slices/analytics/asset';

interface WOStatusIncompleteProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
}

function TimeCostByAsset({ handleOpenModal, start, end }: WOStatusIncompleteProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { completeTimeCostByAsset, loading } = useSelector(
    (state) => state.assetAnalytics
  );
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getWOTimeCostByAsset(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    time: string;
    cost: number;
    color: string;
    filters: Filter[];
  }[] = completeTimeCostByAsset.map((asset) => {
    return {
      label: asset.name,
      time: (asset.time / 3600).toFixed(2),
      cost: Number(asset.cost.toFixed(2)),
      color: getRandomColor(),
      filters: [{ key: 'asset', value: asset.id }]
    };
  });
  const title = t('hours_and_cost_by_asset');
  return (
    <AnalyticsCard title={title}>
      {loading.completeTimeCostByAsset ? (
        <Loading />
      ) : (
        <ComposedChart width={508} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis yAxisId="left-axis" unit={'h'} type={'number'} domain={['dataMin', 'dataMax']} />
          <YAxis yAxisId="right-axis"
                 orientation="right" type={'number'}
                 domain={['dataMin', 'dataMax']} />
          <Tooltip />
          <Legend />
          <Bar
            dataKey="time"
            fill={theme.colors.warning.main}
            name={t('hours')}
            yAxisId="left-axis"
          >
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
            name={t('cost')}
            type="monotone"
            dataKey="cost"
            stroke={theme.colors.primary.main}
            yAxisId="right-axis"
          />
        </ComposedChart>
      )}
    </AnalyticsCard>
  );
}

export default TimeCostByAsset;
