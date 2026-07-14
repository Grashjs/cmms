import {
  Box,
  CircularProgress,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
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
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchRecentWorkOrders = async () => {
      try {
        const response = await api.get<WorkOrderMini[]>(
          `preventive-maintenances/${pmId}/recent-work-orders`
        );
        setWorkOrders(response);
      } catch (error) {
        console.error('Failed to fetch recent work orders:', error);
      } finally {
        setLoading(false);
      }
    };

    if (pmId) {
      setLoading(true);
      fetchRecentWorkOrders();
    }
  }, [pmId]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" py={4}>
        <CircularProgress />
      </Box>
    );
  }

  return workOrders.length > 0 ? (
    <List sx={{ width: '100%' }}>
      {workOrders.map((workOrder) => (
        <ListItemButton
          key={workOrder.id}
          onClick={() => navigate(getWorkOrderUrl(workOrder.id))}
          sx={{ px: 1 }}
        >
          <ListItemText
            primary={workOrder.title}
            secondary={getFormattedDate(workOrder.createdAt)}
          />
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
            <Typography variant="body2">{t(workOrder.status)}</Typography>
          </Box>
        </ListItemButton>
      ))}
    </List>
  ) : (
    <Box width="100%" textAlign={'center'} mt={2}>
      <Typography>{t('no_recent_work_orders')}</Typography>
    </Box>
  );
};

export default RecentWorkOrders;
