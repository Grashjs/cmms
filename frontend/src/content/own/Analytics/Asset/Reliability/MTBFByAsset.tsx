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
import { getDowntimesByAsset, getMTBFByAsset } from '../../../../../slices/analytics/asset';
import { getRandomColor } from '../../../../../utils/overall';

interface WOStatusIncompleteProps {
  handleOpenModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
  start: Date;
  end: Date;
  assetColors: { color: string; id: number }[];
}

function MTBFByAsset({ handleOpenModal, start, end, assetColors }: WOStatusIncompleteProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { mtbfByAsset, loading } = useSelector(
    (state) => state.assetAnalytics
  );
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getMTBFByAsset(start, end));
  }, [start, end]);

  const columns: string[] = ['id'];

  const formattedData: {
    label: string;
    mtbf: number;
    color: string;
    filters: Filter[];
  }[] = mtbfByAsset.map((asset) => {
    return {
      label: asset.name,
      mtbf: asset.mtbf,
      color: assetColors.find(assetColor => assetColor.id === asset.id)?.color,
      filters: [{ key: 'asset', value: asset.id }]
    };
  });

  const title = t('MTBF');
  return (
    <AnalyticsCard title={title}>
      {loading.mtbfByAsset ? (
        <Loading />
      ) : (
        <ComposedChart width={508} height={508} data={formattedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" hide />
          <YAxis yAxisId="left-axis" />
          <Tooltip />
          <Bar dataKey="mtbf" fill="#8884d8" name={t('MTBF')} yAxisId="left-axis">
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
        </ComposedChart>
      )}
    </AnalyticsCard>
  );
}

export default MTBFByAsset;
