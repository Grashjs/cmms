import { Helmet } from 'react-helmet-async';
import { Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import useAuth from '../../../../../hooks/useAuth';
import Overview from './Overview';
import { Filter } from '../WOModal';
import IncompleteWOByAsset from './IncompleteByAsset';
import IncompleteWOByUser from './IncompleteByUser';
import CustomDateRangePicker from '../../CustomDateRangePicker';

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

  useEffect(() => {
    setTitle(t('wo_aging'));
  }, []);

  return (
    <>
      <Helmet>
        <title>{t('wo_aging')}</title>
      </Helmet>
      <Grid
        container
        justifyContent="center"
        alignItems="stretch"
        spacing={1}
        my={2}
        paddingX={1}
      >
        <Grid item xs={6}>
          <CustomDateRangePicker start={start} end={end} setStart={setStart} setEnd={setEnd} companyId={companyId} onCompanyChange={setCompanyId} />
        </Grid>
        <Grid item xs={12} md={12}>
          <Overview handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <IncompleteWOByAsset handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <IncompleteWOByUser handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
      </Grid>
    </>
  );
}

export default WOStatusStats;
