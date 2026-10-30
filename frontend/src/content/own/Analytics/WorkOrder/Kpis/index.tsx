import { Helmet } from 'react-helmet-async';
import { Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import useAuth from '../../../../../hooks/useAuth';
import { useDispatch } from '../../../../../store';
import CustomDateRangePicker from '../../CustomDateRangePicker';
import { FilterField } from '../../../../../models/owns/page';
import {
  getAvailabilityByAsset,
  getMaintenanceKpis,
  getMaintenanceKpisTrend
} from '../../../../../slices/analytics/kpi';
import Overview from './Overview';
import BacklogTrend from './BacklogTrend';
import PlannedVsReactive from './PlannedVsReactive';
import ResponseTimes from './ResponseTimes';
import AssetAvailability from './AssetAvailability';

function MaintenanceKpis() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const dispatch = useDispatch();
  const [end, setEnd] = useState(new Date());
  const nowMinusMonth = new Date();
  nowMinusMonth.setMonth(nowMinusMonth.getMonth() - 1);
  const [start, setStart] = useState(nowMinusMonth);
  const { user } = useAuth();
  const [companyId, setCompanyId] = useState<number | undefined>(
    user?.superAccountRelations?.[0]?.childCompanyId ?? undefined
  );
  const [filterFields, setFilterFields] = useState<FilterField[]>([]);

  useEffect(() => {
    setTitle(t('maintenance_kpis'));
  }, []);

  // one request per endpoint: several cards share the trend
  useEffect(() => {
    dispatch(getMaintenanceKpis(start, end, companyId, filterFields));
    dispatch(getMaintenanceKpisTrend(start, end, companyId, filterFields));
    dispatch(getAvailabilityByAsset(start, end, companyId, filterFields));
  }, [start, end, companyId, filterFields]);

  return (
    <>
      <Helmet>
        <title>{t('maintenance_kpis')}</title>
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
          <CustomDateRangePicker
            start={start}
            end={end}
            setStart={setStart}
            setEnd={setEnd}
            companyId={companyId}
            onCompanyChange={setCompanyId}
            filterFields={filterFields}
            onFilterFieldsChange={setFilterFields}
          />
        </Grid>
        <Grid item xs={12}>
          <Overview
            start={start}
            end={end}
            companyId={companyId}
            filterFields={filterFields}
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <BacklogTrend />
        </Grid>
        <Grid item xs={12} md={6}>
          <PlannedVsReactive />
        </Grid>
        <Grid item xs={12}>
          <ResponseTimes />
        </Grid>
        <Grid item xs={12}>
          <AssetAvailability />
        </Grid>
      </Grid>
    </>
  );
}

export default MaintenanceKpis;
