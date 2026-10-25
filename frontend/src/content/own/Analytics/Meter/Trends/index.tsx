import { Helmet } from 'react-helmet-async';
import {
  Autocomplete,
  Card,
  Chip,
  Grid,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../../contexts/TitleContext';
import useAuth from '../../../../../hooks/useAuth';
import { useDispatch, useSelector } from '../../../../../store';
import CustomDateRangePicker from '../../CustomDateRangePicker';
import Loading from '../../Loading';
import usePreviousPeriod from '../../usePreviousPeriod';
import {
  clearMeterAnalytics,
  fetchMeterAnalytics,
  getAnalyticsMeters,
  getMeterAnalytics
} from '../../../../../slices/analytics/meter';
import { MeterOption } from '../../../../../models/owns/analytics/meter';
import Overview from './Overview';
import TrendChart from './TrendChart';
import ConsumptionChart from './ConsumptionChart';

function MeterTrends() {
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
  const [meterId, setMeterId] = useState<number | null>(null);
  const { meters, analytics, loading } = useSelector(
    (state) => state.meterAnalytics
  );
  const selected: MeterOption | null =
    meters.find((meter) => meter.id === meterId) ?? null;
  const previous = usePreviousPeriod(
    (previousStart, previousEnd, previousCompanyId) =>
      meterId
        ? fetchMeterAnalytics(
            meterId,
            previousStart,
            previousEnd,
            previousCompanyId
          )
        : Promise.reject(),
    start,
    end,
    companyId,
    undefined,
    [meterId]
  );

  useEffect(() => {
    setTitle(t('meter_trends'));
  }, []);

  useEffect(() => {
    dispatch(getAnalyticsMeters(companyId));
  }, [companyId]);

  // keeps the selected meter while the company has it, otherwise the first one
  useEffect(() => {
    if (!meters.some((meter) => meter.id === meterId)) {
      setMeterId(meters[0]?.id ?? null);
    }
  }, [meters]);

  useEffect(() => {
    if (selected) {
      dispatch(getMeterAnalytics(selected.id, start, end, companyId));
    } else {
      dispatch(clearMeterAnalytics());
    }
  }, [selected?.id, start, end, companyId]);

  const showAnalytics = selected && analytics?.meterId === selected.id;

  return (
    <>
      <Helmet>
        <title>{t('meter_trends')}</title>
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
          />
        </Grid>
        <Grid item xs={12}>
          <Card sx={{ p: 2 }}>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              alignItems={{ sm: 'center' }}
              justifyContent="center"
            >
              <Autocomplete<MeterOption, false, false, false>
                sx={{ width: { xs: '100%', sm: 420 } }}
                options={meters}
                loading={loading.meters}
                value={selected}
                onChange={(_event, value) => setMeterId(value?.id ?? null)}
                getOptionLabel={(option) => option.name}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                noOptionsText={t('no_meters_analytics')}
                renderOption={(props, option) => (
                  <li {...props} key={option.id}>
                    <Stack>
                      <Typography>{option.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {[option.assetName, option.unit]
                          .filter(Boolean)
                          .join(' · ')}
                      </Typography>
                    </Stack>
                  </li>
                )}
                renderInput={(params) => (
                  <TextField {...params} label={t('meter')} />
                )}
              />
              {selected && (
                <Stack direction="row" spacing={1} flexWrap="wrap">
                  {selected.assetName && <Chip label={selected.assetName} />}
                  {selected.unit && (
                    <Chip variant="outlined" label={selected.unit} />
                  )}
                  {selected.cumulative && (
                    <Chip
                      color="primary"
                      variant="outlined"
                      label={t('cumulative_meter')}
                    />
                  )}
                </Stack>
              )}
            </Stack>
            {!loading.meters && !meters.length && (
              <Typography color="text.secondary" textAlign="center" mt={2}>
                {t('no_meters_analytics')}
              </Typography>
            )}
          </Card>
        </Grid>
        {selected && !showAnalytics && loading.analytics && (
          <Grid item xs={12}>
            <Card sx={{ p: 2 }}>
              <Loading />
            </Card>
          </Grid>
        )}
        {showAnalytics && (
          <>
            <Grid item xs={12}>
              <Overview
                analytics={analytics}
                loading={loading.analytics}
                previous={previous}
              />
            </Grid>
            <Grid item xs={12}>
              <TrendChart analytics={analytics} loading={loading.analytics} />
            </Grid>
            {analytics.cumulative && (
              <Grid item xs={12}>
                <ConsumptionChart
                  analytics={analytics}
                  loading={loading.analytics}
                />
              </Grid>
            )}
          </>
        )}
      </Grid>
    </>
  );
}

export default MeterTrends;
