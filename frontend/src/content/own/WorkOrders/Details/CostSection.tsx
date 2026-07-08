import {
  Box,
  Button,
  CircularProgress,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { Fragment, useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../../store';
import { deleteAdditionalCost } from '../../../../slices/additionalCost';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import useAuth from '../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../models/owns/role';
import { getErrorMessage } from '../../../../utils/api';
import WorkOrder from '../../../../models/owns/workOrder';
import AdditionalCost from '../../../../models/owns/additionalCost';
import AddCostModal from './AddCostModal';
import ConfirmDialog from '../../components/ConfirmDialog';
interface CostSectionProps {
  workOrder: WorkOrder;
  additionalCosts: AdditionalCost[];
  loading: boolean;
}

export default function CostSection({
  workOrder,
  additionalCosts,
  loading
}: CostSectionProps) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { getFormattedCurrency, getFormattedDate } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { hasEditPermission } = useAuth();
  const [openAddCostModal, setOpenAddCostModal] = useState<boolean>(false);
  const [selectedCost, setSelectedCost] = useState<AdditionalCost | null>(null);
  const [openDeleteConfirm, setOpenDeleteConfirm] = useState<boolean>(false);
  const [costToDelete, setCostToDelete] = useState<AdditionalCost | null>(null);

  const handleEdit = (cost: AdditionalCost) => {
    setSelectedCost(cost);
    setOpenAddCostModal(true);
  };

  const handleDelete = (cost: AdditionalCost) => {
    setCostToDelete(cost);
    setOpenDeleteConfirm(true);
  };

  const confirmDelete = () => {
    if (costToDelete) {
      dispatch(deleteAdditionalCost(workOrder.id, costToDelete.id)).catch(
        (err) => showSnackBar(getErrorMessage(err), 'error')
      );
    }
    setOpenDeleteConfirm(false);
    setCostToDelete(null);
  };

  const canEdit = hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder);

  return (
    <Box>
      <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
        {t('additional_costs')}
      </Typography>
      {loading ? (
        <Stack width={'100%'} alignItems={'center'}>
          <CircularProgress />
        </Stack>
      ) : (
        <Fragment>
          {!additionalCosts.length ? (
            <Typography sx={{ color: theme.colors.alpha.black[70] }}>
              {t('no_additional_cost')}
            </Typography>
          ) : (
            <List>
              {additionalCosts.map((additionalCost) => (
                <ListItem
                  key={additionalCost.id}
                  secondaryAction={
                    <Box
                      sx={{
                        display: 'flex',
                        flexDirection: 'row',
                        alignItems: 'center',
                        justifyContent: 'flex-end',
                        gap: 1
                      }}
                    >
                      <Typography variant="h6">
                        {getFormattedCurrency(additionalCost.cost)}
                      </Typography>
                      {canEdit && (
                        <>
                          <IconButton
                            onClick={() => handleEdit(additionalCost)}
                          >
                            <EditTwoToneIcon fontSize="small" color="primary" />
                          </IconButton>
                          <IconButton
                            onClick={() => handleDelete(additionalCost)}
                          >
                            <DeleteTwoToneIcon fontSize="small" color="error" />
                          </IconButton>
                        </>
                      )}
                    </Box>
                  }
                >
                  <ListItemText
                    primary={
                      <Typography variant="h6">
                        {additionalCost.description}
                      </Typography>
                    }
                    secondary={getFormattedDate(additionalCost.createdAt)}
                  />
                </ListItem>
              ))}
              <ListItem
                secondaryAction={
                  <Typography variant="h6" fontWeight="bold">
                    {getFormattedCurrency(
                      additionalCosts.reduce(
                        (acc, additionalCost) =>
                          additionalCost.includeToTotalCost
                            ? acc + additionalCost.cost
                            : acc,
                        0
                      )
                    )}
                  </Typography>
                }
              >
                <ListItemText
                  primary={
                    <Typography variant="h6" fontWeight="bold">
                      {t('total')}
                    </Typography>
                  }
                />
              </ListItem>
            </List>
          )}
        </Fragment>
      )}
      {canEdit && (
        <Button
          onClick={() => {
            setSelectedCost(null);
            setOpenAddCostModal(true);
          }}
          variant="outlined"
          sx={{ mt: 1 }}
        >
          {t('add_additional_cost')}
        </Button>
      )}
      <AddCostModal
        open={openAddCostModal}
        onClose={() => {
          setOpenAddCostModal(false);
          setSelectedCost(null);
        }}
        workOrderId={workOrder.id}
        additionalCost={selectedCost}
      />
      <ConfirmDialog
        open={openDeleteConfirm}
        onCancel={() => {
          setOpenDeleteConfirm(false);
          setCostToDelete(null);
        }}
        onConfirm={confirmDelete}
        confirmText={t('delete')}
        question={t('confirm_delete_additional_cost')}
      />
    </Box>
  );
}
