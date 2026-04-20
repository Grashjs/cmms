import { Box, Grid, Link, Stack, Typography, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import React, { useContext, useEffect, useState } from 'react';
import { getWorkOrderUrl } from '../../../utils/urlPaths';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import axios from '../../../utils/axios';
import api from '../../../utils/api';
import { useNavigate } from 'react-router-dom';
import { WorkOrderMini } from '../../../models/owns/workOrder';

const RecentWorkOrders = ({ pmId }: { pmId: number }) => {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const navigate = useNavigate();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const [workOrders, setWorkOrders] = useState<WorkOrderMini[]>([]);

  useEffect(() => {
    const fetchRecentWorkOrders = async () => {
      try {
        const response = await api.get<WorkOrderMini[]>(
          `preventive-maintenances/${pmId}/recent-work-orders`
        );
        setWorkOrders(response);
      } catch (error) {
        console.error('Failed to fetch recent work orders:', error);
      }
    };

    if (pmId) {
      fetchRecentWorkOrders();
    }
  }, [pmId]);

  return (
    <Grid container mt={2}>
      {workOrders.length > 0 ? (
        workOrders.map((workOrder) => (
          <Grid item xs={12} key={workOrder.id}>
            <Box
              sx={{
                p: 1,
                cursor: 'pointer'
              }}
              onClick={() => navigate(getWorkOrderUrl(workOrder.id))}
            >
              <Stack direction={'row'} justifyContent={'space-between'}>
                <Box>
                  <Typography variant="h6">{workOrder.title}</Typography>
                  <Typography variant="body2">
                    {getFormattedDate(workOrder.createdAt)}
                  </Typography>
                </Box>
                <Box
                  sx={{
                    backgroundColor:
                      workOrder.status === 'IN_PROGRESS'
                        ? theme.colors.success.main
                        : workOrder.status === 'ON_HOLD'
                        ? theme.colors.warning.main
                        : theme.colors.alpha.black[30],
                    color: 'white',
                    width: 'fit-content',
                    height: 'fit-content',
                    py: 0.5,
                    px: 1,
                    borderRadius: 1
                  }}
                >
                  {t(workOrder.status)}
                </Box>
              </Stack>
            </Box>
          </Grid>
        ))
      ) : (
        <Grid item xs={12}>
          <Box width="100%" textAlign={'center'}>
            <Typography>{t('no_recent_work_orders')}</Typography>
          </Box>
        </Grid>
      )}
    </Grid>
  );
};

export default RecentWorkOrders;
