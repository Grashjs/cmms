import { Helmet } from 'react-helmet-async';
import { Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import useAuth from '../../../../../hooks/useAuth';
import Overview from './Overview';
import RepairTimeByAsset from './RepairTimeByAsset';
import DowntimesByAsset from './DowntimesByAsset';
import MeantimesTrends from './MeantimesTrends';
import { Filter } from '../WOModal';
import CustomDateRangePicker from '../../CustomDateRangePicker';
import { useDispatch, useSelector } from '../../../../../store';
import { getAssetsMini } from '../../../../../slices/asset';
import { getRandomColor } from '../../../../../utils/overall';
import MTBFByAsset from './MTBFByAsset';

interface WOStatusStatsProps {
  handleOpenWOModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
}

function WOStatusStats({ handleOpenWOModal }: WOStatusStatsProps) {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [end, setEnd] = useState(new Date());
  const nowMinusMonth = new Date();
  nowMinusMonth.setMonth(nowMinusMonth.getMonth() - 1);
  const [start, setStart] = useState(nowMinusMonth);
  const { user } = useAuth();
  const [companyId, setCompanyId] = useState<number | undefined>(
    user?.superAccountRelations?.[0]?.childCompanyId ?? undefined
  );
  const dispatch = useDispatch();
  const { assetsMini } = useSelector((state) => state.assets);
  const [assetColors, setAssetColors] = useState<{ color: string; id: number }[]>([]);

  useEffect(() => {
    setTitle(t('reliability_dashboard'));
    dispatch(getAssetsMini(companyId));
  }, [companyId]);

  useEffect(() => {
    setAssetColors(assetsMini.map((assetMini) => ({ color: getRandomColor(), id: assetMini.id })));
  }, [assetsMini]);
  return (
    <>
      <Helmet>
        <title>{t('reliability_dashboard')}</title>
      </Helmet>
      <Grid
        container
        justifyContent="center"
        alignItems="stretch"
        spacing={1}
        my={2}
        paddingX={1}
      >
        <Grid item xs={12}>
          <CustomDateRangePicker start={start} end={end} setStart={setStart} setEnd={setEnd} companyId={companyId} onCompanyChange={setCompanyId} />
        </Grid>
        <Grid item xs={12}>
          <Overview handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12}>
          <DowntimesByAsset handleOpenModal={handleOpenWOModal} start={start} end={end} assetColors={assetColors} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <RepairTimeByAsset handleOpenModal={handleOpenWOModal} start={start}
                             end={end}
                             assetColors={assetColors} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <MTBFByAsset handleOpenModal={handleOpenWOModal} start={start} end={end} assetColors={assetColors} companyId={companyId} />
        </Grid>

        <Grid item xs={12} md={12}>
          <MeantimesTrends handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
      </Grid>
    </>
  );
}

export default WOStatusStats;
