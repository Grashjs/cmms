import Asset from '../../../../models/owns/asset';
import { Box, Card, CircularProgress, Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../store';
import { useContext, useEffect } from 'react';
import { getAssetWorkOrders } from '../../../../slices/asset';
import { useNavigate } from 'react-router-dom';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import Loading from '../../Analytics/Loading';

interface PropsType {
  asset: Asset;
}

const AssetWorkOrders = ({ asset }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { assetInfos, loadingGet } = useSelector((state) => state.assets);
  const workOrders = assetInfos[asset?.id]?.workOrders;
  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    if (asset) dispatch(getAssetWorkOrders(asset.id));
  }, [asset]);

  if (loadingGet)
    return (
      <Box
        sx={{
          height: '50vh',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center'
        }}
      >
        <Loading />
      </Box>
    );
  return (
    <Box sx={{ px: 4 }}>
      <Grid container spacing={2}>
        {workOrders?.length ? (
          workOrders.map((workOrder) => (
            <Grid key={workOrder.id} item xs={12}>
              <Card
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(`/app/work-orders/${workOrder.id}`)}
              >
                <Box
                  sx={{
                    p: 2,
                    flexDirection: 'row',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <Box>
                    <Typography variant="h4" gutterBottom>
                      {workOrder.title}
                    </Typography>
                    <Typography variant="subtitle1">{`#${workOrder.id}`}</Typography>
                  </Box>
                  <Typography
                    variant="h6"
                    color={workOrder.dueDate ? 'error' : 'primary'}
                  >
                    {workOrder.dueDate
                      ? t('due_at_date', {
                          date: getFormattedDate(workOrder.dueDate)
                        })
                      : t('no_due_date')}
                  </Typography>
                  <Typography variant="h6">
                    {workOrder.primaryUser
                      ? `${workOrder.primaryUser.firstName} ${workOrder.primaryUser.lastName}`
                      : t('no_primary_worker')}
                  </Typography>
                  <Typography variant="h6">{t(workOrder.status)}</Typography>
                </Box>
              </Card>
            </Grid>
          ))
        ) : (
          <Grid item xs={12}>
            <Card>
              <Box
                sx={{
                  height: 500,
                  p: 2,
                  flexDirection: 'row',
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center'
                }}
              >
                <Typography variant="h4">{t('no_wo_linked_asset')}</Typography>
              </Box>
            </Card>
          </Grid>
        )}
      </Grid>
    </Box>
  );
};

export default AssetWorkOrders;
