import {
  Box,
  Button,
  CircularProgress,
  debounce,
  Divider,
  Grid,
  IconButton,
  Link,
  Tab,
  Tabs,
  Typography,
  useTheme
} from '@mui/material';
import { ChangeEvent, useContext, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import PurchaseOrder from '../../../models/owns/purchaseOrder';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import { editPartQuantity } from '../../../slices/partQuantity';
import { useDispatch } from '../../../store';
import PartQuantitiesList from '../components/PartQuantitiesList';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import PartQuantity from 'src/models/owns/partQuantity';
import ClearTwoToneIcon from '@mui/icons-material/ClearTwoTone';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import { respondPurchaseOrder } from '../../../slices/purchaseOrder';

interface PurchaseOrderDetailsProps {
  purchaseOrder: PurchaseOrder;
  handleOpenUpdate: () => void;
  handleDelete: () => void;
  partQuantities: PartQuantity[];
}
export default function PurchaseOrderDetails(props: PurchaseOrderDetailsProps) {
  const { purchaseOrder, handleOpenUpdate, handleDelete, partQuantities } =
    props;
  const { t }: { t: any } = useTranslation();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const [approving, setApproving] = useState<boolean>(false);
  const [cancelling, setCancelling] = useState<boolean>(false);
  const { hasEditPermission, hasDeletePermission, hasViewPermission } =
    useAuth();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const theme = useTheme();
  const [currentTab, setCurrentTab] = useState<string>('details');
  const tabs = [
    { value: 'details', label: t('details') },
    { value: 'parts', label: t('parts') },
    { value: 'shipping', label: t('shipping_information') },
    { value: 'additionalInfos', label: t('additional_information') }
  ];

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const onPartQuantityChange = (value: string, partQuantity) => {
    dispatch(
      editPartQuantity(purchaseOrder.id, partQuantity.id, Number(value), true)
    )
      .then(() => showSnackBar(t('quantity_edit_success'), 'success'))
      .catch((err) => showSnackBar(t('quantity_edit_failure'), 'error'));
  };
  const debouncedPartQuantityChange = useMemo(
    () => debounce(onPartQuantityChange, 1500),
    []
  );
  const onApprove = () => {
    setApproving(true);
    dispatch(respondPurchaseOrder(purchaseOrder.id, true)).finally(() =>
      setApproving(false)
    );
  };

  const onCancel = () => {
    setCancelling(true);
    dispatch(respondPurchaseOrder(purchaseOrder.id, false)).finally(() =>
      setCancelling(false)
    );
  };
  const BasicField = ({
    label,
    value,
    id,
    type
  }: {
    label: string | number;
    value: string | number;
    type?: string;
    id?: number;
  }) => {
    return value ? (
      <Grid item xs={12} lg={6}>
        <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
          {label}
        </Typography>
        {type === 'vendor' ? (
          <Link
            href={`/app/vendors-customers/vendors/${id}`}
            variant="h6"
            fontWeight="bold"
          >
            {value}
          </Link>
        ) : (
          <Typography variant="h6">{value}</Typography>
        )}
      </Grid>
    ) : null;
  };
  const detailsFieldsToRender = (
    purchaseOrder1: PurchaseOrder
  ): {
    label: string;
    value: string | number;
    type?: 'vendor';
    id?: number;
  }[] => [
    {
      label: t('name'),
      value: purchaseOrder1.name
    },
    {
      label: t('id'),
      value: purchaseOrder1.id
    },
    {
      label: t('due_date'),
      value: getFormattedDate(purchaseOrder1.shippingDueDate)
    },
    {
      label: t('category'),
      value: purchaseOrder1.category?.name
    },
    {
      label: t('created_at'),
      value: getFormattedDate(purchaseOrder1.createdAt)
    },
    {
      label: t('vendor'),
      type: 'vendor',
      value: purchaseOrder1.vendor?.companyName,
      id: purchaseOrder1.vendor?.id
    }
  ];
  const shippingFieldsToRender = (
    purchaseOrder1: PurchaseOrder
  ): { label: string; value: any }[] => [
    {
      label: t('Company Name'),
      value: purchaseOrder1.shippingCompanyName
    },
    {
      label: t('address'),
      value: purchaseOrder1.shippingAddress
    },
    {
      label: t('city'),
      value: purchaseOrder1.shippingCity
    },
    {
      label: t('state'),
      value: purchaseOrder1.shippingState
    },
    {
      label: t('phone'),
      value: purchaseOrder1.shippingPhone
    },
    {
      label: t('fax_number'),
      value: purchaseOrder1.shippingFax
    },
    {
      label: t('ship_to'),
      value: purchaseOrder1.shippingShipToName
    },
    {
      label: t('additional_details'),
      value: purchaseOrder1.shippingAdditionalDetail
    }
  ];
  const additionalInfosFieldsToRender = (
    purchaseOrder1: PurchaseOrder
  ): { label: string; value: any }[] => [
    {
      label: t('requisitioner'),
      value: purchaseOrder1.additionalInfoRequisitionedName
    },
    {
      label: t('shipping_method'),
      value: purchaseOrder1.additionalInfoShippingMethod
    },
    {
      label: t('notes'),
      value: purchaseOrder1.additionalInfoNotes
    },
    {
      label: t('terms'),
      value: purchaseOrder1.additionalInfoTerm
    }
  ];
  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={2}
      padding={4}
    >
      <Grid
        item
        xs={12}
        display="flex"
        flexDirection="row"
        justifyContent="space-between"
      >
        <Box>
          <Typography variant="h2">{purchaseOrder?.name}</Typography>
          <Typography variant="h6">{purchaseOrder?.shippingAddress}</Typography>
          <Typography variant="h6">{t(purchaseOrder?.status)}</Typography>
        </Box>
        <Box>
          {hasEditPermission(
            PermissionEntity.PURCHASE_ORDERS,
            purchaseOrder
          ) && (
            <IconButton onClick={handleOpenUpdate} style={{ marginRight: 10 }}>
              <EditTwoToneIcon color="primary" />
            </IconButton>
          )}
          {hasDeletePermission(
            PermissionEntity.PURCHASE_ORDERS,
            purchaseOrder
          ) && (
            <IconButton onClick={handleDelete}>
              <DeleteTwoToneIcon color="error" />
            </IconButton>
          )}
        </Box>
      </Grid>
      <Divider />
      {purchaseOrder.status === 'PENDING' &&
        hasViewPermission(PermissionEntity.SETTINGS) && (
          <>
            <Grid
              item
              xs={12}
              sx={{
                display: 'flex',
                flexDirection: 'row',
                justifyContent: 'space-around'
              }}
            >
              <Button
                startIcon={
                  cancelling ? (
                    <CircularProgress size="1rem" />
                  ) : (
                    <ClearTwoToneIcon />
                  )
                }
                onClick={onCancel}
                variant="outlined"
              >
                {t('reject')}
              </Button>
              <Button
                startIcon={
                  approving ? (
                    <CircularProgress size="1rem" sx={{ color: 'white' }} />
                  ) : (
                    <CheckTwoToneIcon />
                  )
                }
                onClick={onApprove}
                variant="contained"
              >
                {t('approve')}
              </Button>
            </Grid>
            <Divider />
          </>
        )}
      <Grid item xs={12}>
        <Tabs
          onChange={handleTabsChange}
          value={currentTab}
          variant="scrollable"
          scrollButtons="auto"
          textColor="primary"
          indicatorColor="primary"
        >
          {tabs.map((tab) => (
            <Tab key={tab.value} label={tab.label} value={tab.value} />
          ))}
        </Tabs>
      </Grid>
      <Grid item xs={12}>
        {currentTab === 'details' && (
          <Grid container spacing={2}>
            {detailsFieldsToRender(purchaseOrder).map((field) => (
              <BasicField
                key={field.id}
                label={field.label}
                value={field.value}
                type={field.type}
                id={field.id}
              />
            ))}
          </Grid>
        )}
        {currentTab === 'parts' && (
          <PartQuantitiesList
            partQuantities={partQuantities}
            disabled={
              !hasEditPermission(
                PermissionEntity.PURCHASE_ORDERS,
                purchaseOrder
              ) || purchaseOrder.status !== 'PENDING'
            }
            onChange={debouncedPartQuantityChange}
          />
        )}
        {currentTab === 'shipping' && (
          <Grid container spacing={2}>
            {shippingFieldsToRender(purchaseOrder).map((field) => (
              <BasicField
                key={field.label}
                label={field.label}
                value={field.value}
              />
            ))}
          </Grid>
        )}
        {currentTab === 'additionalInfos' && (
          <Grid container spacing={2}>
            {additionalInfosFieldsToRender(purchaseOrder).map((field) => (
              <BasicField
                key={field.label}
                label={field.label}
                value={field.value}
              />
            ))}
          </Grid>
        )}
      </Grid>
    </Grid>
  );
}
