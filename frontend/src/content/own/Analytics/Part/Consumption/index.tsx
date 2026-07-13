import { Helmet } from 'react-helmet-async';
import { Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import Overview from './Overview';
import { Filter } from '../WOModal';
import PartConsumptionsByDate from './PartConsumptionsByDate';
import CustomDateRangePicker from '../../CustomDateRangePicker';
import PartConsumptionsPareto from './PartConsumptionsPareto';
import PartConsumptionsByAsset from './PartConsumptionsByAsset';
import PartConsumptionByCategory from './PartConsumptionByCategory';
import PartConsumptionByWOCategory from './PartConsumptionByWOCategory';

interface WOStatusStatsProps {
  handleOpenWOModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
}

function PartsConsumption({ handleOpenWOModal }: WOStatusStatsProps) {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [end, setEnd] = useState(new Date());
  const nowMinusMonth = new Date();
  nowMinusMonth.setMonth(nowMinusMonth.getMonth() - 1);
  const [start, setStart] = useState(nowMinusMonth);

  useEffect(() => {
    setTitle(t('parts_consumption'));
  }, []);

  return (
    <>
      <Helmet>
        <title>{t('parts_consumption')}</title>
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
          <CustomDateRangePicker start={start} end={end} setStart={setStart} setEnd={setEnd} />
        </Grid>
        <Grid item xs={12} md={12}>
          <Overview handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <PartConsumptionsPareto handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <PartConsumptionsByAsset handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <PartConsumptionByCategory handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <PartConsumptionByWOCategory handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12}>
          <PartConsumptionsByDate handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
      </Grid>
    </>
  );
}

export default PartsConsumption;
