import { Helmet } from 'react-helmet-async';
import { Box, Card, CardContent, Grid, TextField } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import Overview from './Overview';
import DowntimesAndCosts from './DowntimesAndCosts';
import { Filter } from '../WOModal';
import DowntimesByDate from './DowntimesByDate';
import { AdapterDayjs } from '@mui/x-date-pickers-pro/AdapterDayjs';
import { DateRangePicker } from '@mui/x-date-pickers-pro/DateRangePicker';
import { LocalizationProvider } from '@mui/x-date-pickers-pro';
import CustomDateRangePicker from '../../CustomDateRangePicker';

interface WOStatusStatsProps {
  handleOpenWOModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
}

function AssetCost({ handleOpenWOModal }: WOStatusStatsProps) {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [end, setEnd] = useState(new Date());
  const nowMinusMonth = new Date();
  nowMinusMonth.setMonth(nowMinusMonth.getMonth() - 1);
  const [start, setStart] = useState(nowMinusMonth);

  useEffect(() => {
    setTitle(t('total_maintenance_cost'));
  }, []);

  return (
    <>
      <Helmet>
        <title>{t('total_maintenance_cost')}</title>
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
          <DowntimesAndCosts handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <DowntimesByDate handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
      </Grid>
    </>
  );
}

export default AssetCost;
