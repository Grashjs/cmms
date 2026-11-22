import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Chip,
  CircularProgress,
  debounce,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'src/store';
import { getWorkOrdersMini } from 'src/slices/workOrder';
import { linkRequestToWorkOrder } from 'src/slices/request';
import { SearchCriteria } from 'src/models/owns/page';
import WorkOrder from 'src/models/owns/workOrder';
import { WorkOrderBaseMiniDTO } from 'src/models/owns/workOrderBase';
import { onSearchQueryChange } from 'src/utils/overall';
import { CustomSnackBarContext } from 'src/contexts/CustomSnackBarContext';
import { getErrorMessage } from 'src/utils/api';

interface RequestLinkWorkOrderModalProps {
  open: boolean;
  requestId: number;
  onClose: () => void;
  onLinked: () => void;
}

export default function RequestLinkWorkOrderModal({
  open,
  requestId,
  onClose,
  onLinked
}: RequestLinkWorkOrderModalProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { workOrdersMini } = useSelector((state) => state.workOrders);
  const [selected, setSelected] = useState<WorkOrderBaseMiniDTO>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });

  useEffect(() => {
    if (open) dispatch(getWorkOrdersMini(criteria));
  }, [criteria, open]);

  useEffect(() => {
    if (!open) setSelected(null);
  }, [open]);

  const onQueryChange = (event) => {
    onSearchQueryChange<WorkOrder>(event, criteria, setCriteria, [
      'title',
      'customId'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 800), []);

  const getOptionLabel = (option: WorkOrderBaseMiniDTO) =>
    option.customId ? `${option.customId} - ${option.title}` : option.title;

  const onSubmit = () => {
    if (!selected) return;
    setSubmitting(true);
    dispatch(linkRequestToWorkOrder(requestId, selected.id))
      .then(() => {
        showSnackBar(t('request_link_wo_success'), 'success');
        onLinked();
      })
      .catch((err) => showSnackBar(getErrorMessage(err), 'error'))
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          {t('request_link_wo')}
        </Typography>
        <Typography variant="subtitle2">
          {t('request_link_wo_description')}
        </Typography>
      </DialogTitle>
      <DialogContent dividers sx={{ p: 3 }}>
        <Autocomplete<WorkOrderBaseMiniDTO, false, false, false>
          fullWidth
          options={workOrdersMini.content}
          filterOptions={(options) => options}
          getOptionLabel={getOptionLabel}
          isOptionEqualToValue={(option, value) => option.id === value.id}
          onInputChange={(event, value, reason) => {
            if (reason === 'input' || reason === 'clear')
              debouncedQueryChange({ target: { value } });
          }}
          onChange={(event, value) => setSelected(value)}
          value={selected}
          renderOption={(props, option) => (
            <li {...props} key={option.id}>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  width: '100%',
                  gap: 1
                }}
              >
                <Typography variant="body2">
                  {getOptionLabel(option)}
                </Typography>
                {option.status && (
                  <Chip size="small" label={t(option.status)} />
                )}
              </Box>
            </li>
          )}
          renderInput={(params) => (
            <TextField
              {...params}
              fullWidth
              variant="outlined"
              label={t('work_order')}
              placeholder={t('search')}
            />
          )}
        />
        {selected && (
          <Alert severity="info" sx={{ mt: 2 }}>
            {t('request_link_wo_confirm', {
              workOrder: getOptionLabel(selected)
            })}
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose} disabled={submitting}>
          {t('cancel')}
        </Button>
        <Button
          variant="contained"
          onClick={onSubmit}
          disabled={!selected || submitting}
          startIcon={submitting ? <CircularProgress size="1rem" /> : null}
        >
          {t('link')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
