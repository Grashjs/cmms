import {
  Box,
  Button,
  CircularProgress,
  IconButton,
  Link,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../../store';
import { deleteLabor } from '../../../../slices/labor';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import useAuth from '../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../models/owns/role';
import { getHoursAndMinutesAndSeconds } from '../../../../utils/formatters';
import { getUserUrl } from '../../../../utils/urlPaths';
import { getErrorMessage } from '../../../../utils/api';
import WorkOrder from '../../../../models/owns/workOrder';
import Labor from '../../../../models/owns/labor';
import { useBrand } from '../../../../hooks/useBrand';
import AddTimeModal from './AddTimeModal';
import ConfirmDialog from '../../components/ConfirmDialog';

interface TimeSectionProps {
  workOrder: WorkOrder;
  labors: Labor[];
  loading: boolean;
}

const getLaborCost = (labor: Labor): number => {
  const [hours, minutes] = getHoursAndMinutesAndSeconds(labor.duration);
  return Number((labor.hourlyRate * (hours + minutes / 60)).toFixed(2));
};

export default function TimeSection({
  workOrder,
  labors,
  loading
}: TimeSectionProps) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { hasEditPermission } = useAuth();
  const brandConfig = useBrand();
  const [openAddTimeModal, setOpenAddTimeModal] = useState<boolean>(false);
  const [selectedLabor, setSelectedLabor] = useState<Labor | null>(null);
  const [openDeleteConfirm, setOpenDeleteConfirm] = useState<boolean>(false);
  const [laborToDelete, setLaborToDelete] = useState<Labor | null>(null);

  const handleEdit = (labor: Labor) => {
    setSelectedLabor(labor);
    setOpenAddTimeModal(true);
  };

  const handleDelete = (labor: Labor) => {
    setLaborToDelete(labor);
    setOpenDeleteConfirm(true);
  };

  const confirmDelete = () => {
    if (laborToDelete) {
      dispatch(deleteLabor(workOrder.id, laborToDelete.id)).catch((err) =>
        showSnackBar(getErrorMessage(err), 'error')
      );
    }
    setOpenDeleteConfirm(false);
    setLaborToDelete(null);
  };

  const canEdit = hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder);

  return (
    <Box>
      <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
        {t('labors')}
      </Typography>
      {loading ? (
        <Stack width={'100%'} alignItems="center">
          <CircularProgress />
        </Stack>
      ) : !labors.filter((labor) => !labor.logged).length ? (
        <Typography sx={{ color: theme.colors.alpha.black[70] }}>
          {t('no_labor', { shortBrandName: brandConfig.shortName })}
        </Typography>
      ) : (
        <List>
          {labors
            .filter((labor) => !labor.logged)
            .map((labor) => (
              <ListItem
                key={labor.id}
                secondaryAction={
                  <Box
                    sx={{
                      display: 'flex',
                      flexDirection: 'row',
                      alignItems: 'center',
                      justifyContent: 'flex-end'
                    }}
                  >
                    <Typography variant="h6">
                      {getFormattedCurrency(getLaborCost(labor))}
                    </Typography>
                    {canEdit && (
                      <>
                        <IconButton
                          sx={{ ml: 1 }}
                          onClick={() => handleEdit(labor)}
                        >
                          <EditTwoToneIcon fontSize="small" color="primary" />
                        </IconButton>
                        <IconButton
                          sx={{ ml: 1 }}
                          onClick={() => handleDelete(labor)}
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
                    <>
                      {labor.assignedTo ? (
                        <Link
                          href={getUserUrl(labor.assignedTo.id)}
                          variant="h6"
                        >
                          {`${labor.assignedTo.firstName} ${labor.assignedTo.lastName}`}
                        </Link>
                      ) : (
                        <Typography>{t('not_assigned')}</Typography>
                      )}
                    </>
                  }
                  secondary={`${
                    getHoursAndMinutesAndSeconds(labor.duration)[0]
                  }h ${getHoursAndMinutesAndSeconds(labor.duration)[1]}m`}
                />
              </ListItem>
            ))}
          <ListItem
            secondaryAction={
              <Box>
                <Typography variant="h6" fontWeight="bold">
                  {getFormattedCurrency(
                    labors
                      .filter((labor) => !labor.logged)
                      .reduce(
                        (acc, labor) =>
                          labor.includeToTotalTime
                            ? acc + getLaborCost(labor)
                            : acc,
                        0
                      )
                  )}
                </Typography>
              </Box>
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
      {canEdit && (
        <Button
          onClick={() => {
            setSelectedLabor(null);
            setOpenAddTimeModal(true);
          }}
          variant="outlined"
          sx={{ mt: 1 }}
        >
          {t('add_time')}
        </Button>
      )}
      <AddTimeModal
        open={openAddTimeModal}
        onClose={() => {
          setOpenAddTimeModal(false);
          setSelectedLabor(null);
        }}
        workOrderId={workOrder.id}
        labor={selectedLabor}
      />
      <ConfirmDialog
        open={openDeleteConfirm}
        onCancel={() => {
          setOpenDeleteConfirm(false);
          setLaborToDelete(null);
        }}
        onConfirm={confirmDelete}
        confirmText={t('delete')}
        question={t('confirm_delete_labor')}
      />
    </Box>
  );
}
