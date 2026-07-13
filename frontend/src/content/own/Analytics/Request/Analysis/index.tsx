import { Helmet } from 'react-helmet-async';
import { Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import Overview from './Overview';
import { Filter } from '../WOModal';
import RequestStatsByPriority from './RequestStatsByPriority';
import RequestsByDate from './RequestsByDate';
import CustomDateRangePicker from '../../CustomDateRangePicker';
import RequestsByCategory from './RequestsByCategory';
import RequestsResolution from './RequestsResolution';

interface WOStatusStatsProps {
  handleOpenWOModal: (
    columns: string[],
    filters: Filter[],
    title: string
  ) => void;
}

function RequestAnalysis({ handleOpenWOModal }: WOStatusStatsProps) {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [end, setEnd] = useState(new Date());
  const nowMinusMonth = new Date();
  nowMinusMonth.setMonth(nowMinusMonth.getMonth() - 1);
  const [start, setStart] = useState(nowMinusMonth);

  useEffect(() => {
    setTitle(t('requests_analysis'));
  }, []);

  return (
    <>
      <Helmet>
        <title>{t('requests_analysis')}</title>
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
        <Grid item xs={12}>
          <Overview handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <RequestStatsByPriority handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12} md={6}>
          <RequestsByCategory handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12}>
          <RequestsResolution handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
        <Grid item xs={12}>
          <RequestsByDate handleOpenModal={handleOpenWOModal} start={start} end={end} />
        </Grid>
      </Grid>
    </>
  );
}

export default RequestAnalysis;
