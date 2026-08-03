import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Chip,
  CircularProgress,
  Link,
  Stack,
  Typography
} from '@mui/material';
import api from '../../../../utils/api';
import PurchaseOrder from '../../../../models/owns/purchaseOrder';
import { getPurchaseOrderUrl } from '../../../../utils/urlPaths';

interface PurchaseOrdersSectionProps {
  workOrderId: number;
}

/**
 * Purchase orders raised for this work order — read-only. A work order can trigger
 * several of them (different vendors), which is why this is a list rather than a single
 * field. Fetched here instead of through a redux slice because the section is display
 * only and the data is scoped to one work order; that matches how the other on-demand
 * detail panels in this app load.
 */
export default function PurchaseOrdersSection({
  workOrderId
}: PurchaseOrdersSectionProps) {
  const { t }: { t: any } = useTranslation();
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrder[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api
      .get<PurchaseOrder[]>(`purchase-orders/work-order/${workOrderId}`)
      .then((result) => {
        if (active) setPurchaseOrders(result ?? []);
      })
      // A failure here must not take the work order page down with it; the section
      // simply stays empty.
      .catch(() => {
        if (active) setPurchaseOrders([]);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [workOrderId]);

  const statusColor = (status: PurchaseOrder['status']) =>
    status === 'APPROVED'
      ? 'success'
      : status === 'REJECTED'
      ? 'error'
      : 'warning';

  if (loading) {
    return (
      <Box>
        <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
          {t('purchase_orders')}
        </Typography>
        <Stack width={'100%'} alignItems={'center'}>
          <CircularProgress />
        </Stack>
      </Box>
    );
  }

  return (
    <Box>
      <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
        {t('purchase_orders')}
      </Typography>
      {purchaseOrders.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          {t('no_purchase_orders_for_work_order')}
        </Typography>
      ) : (
        <Stack spacing={1}>
          {purchaseOrders.map((purchaseOrder) => (
            <Stack
              key={purchaseOrder.id}
              direction="row"
              spacing={2}
              alignItems="center"
            >
              <Link
                href={getPurchaseOrderUrl(purchaseOrder.id)}
                variant="h6"
                fontWeight="bold"
              >
                {purchaseOrder.name}
              </Link>
              <Chip
                size="small"
                label={t(purchaseOrder.status)}
                color={statusColor(purchaseOrder.status)}
              />
              {purchaseOrder.vendor && (
                <Typography variant="body2" color="text.secondary">
                  {purchaseOrder.vendor.companyName}
                </Typography>
              )}
            </Stack>
          ))}
        </Stack>
      )}
    </Box>
  );
}
