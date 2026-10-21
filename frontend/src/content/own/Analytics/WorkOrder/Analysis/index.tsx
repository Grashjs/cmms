import { Helmet } from 'react-helmet-async';
import { Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import useAuth from '../../../../../hooks/useAuth';
import Overview from './Overview';
import WOByPrimaryUser from './WOByPrimaryUser';
import Costs from './Costs';
import { Filter } from '../WOModal';
import WOByPriority from './WOByPriority';
import WOByCompletedBy from './WOByCompletedBy';
import WOByCategory from './WOByCategory';
import TimeByWeek from './TimeByWeek';
import WOByWeek from './WOByWeek';
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
    setTitle(t('wo_analysis'));
  }, []);

  return (
    <>
      <Helmet>
        <title>{t('wo_analysis')}</title>
      </Helmet>
      <Grid
        container
        justifyContent="center"
        alignItems="stretch"
        spacing={2}
        // Container spacing adds a left negative margin; offset it so both
        // page gutters match the rest of the app.
        sx={{
          width: 'auto',
          ml: { xs: 0, md: 1, lg: 2 },
          mr: { xs: 2, md: 3, lg: 4 },
          mt: 1,
          mb: 3
        }}
      >
        <Grid item xs={6}>
          <CustomDateRangePicker start={start} end={end} setStart={setStart} setEnd={setEnd} companyId={companyId} onCompanyChange={setCompanyId} />
        </Grid>
        <Grid item xs={12} md={12}>
          <Overview handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <WOByPrimaryUser handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <WOByCompletedBy handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <WOByPriority handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <WOByCategory handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <WOByWeek handleOpenModal={handleOpenWOModal} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={6}>
          <TimeByWeek handleOpenModal={handleOpenWOModal} companyId={companyId} />
        </Grid>
        <Grid item xs={12} md={12}>
          <Costs handleOpenModal={handleOpenWOModal} start={start} end={end} companyId={companyId} />
        </Grid>
      </Grid>
    </>
  );
}

export default WOStatusStats;
