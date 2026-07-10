import {
  Autocomplete,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../../../store';
import {
  ReportConfig,
  sendWorkOrderReport
} from '../../../../slices/workOrder';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { getErrorMessage } from '../../../../utils/api';
import { CustomerMiniDTO } from '../../../../models/owns/customer';
import { getCustomersMini } from '../../../../slices/customer';
import ReportConfigFields from './ReportConfigFields';

interface SendReportModalProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
  defaultCustomers: CustomerMiniDTO[];
}

const defaultConfig: ReportConfig = {
  cost: true,
  comments: true,
  workOrderHistory: true,
  estimatedTime: true,
  locationAddress: true,
  priority: true,
  workOrderInformation: true,
  relations: true,
  files: true,
  signature: true,
  tasks: true
};

export default function SendReportModal({
  open,
  onClose,
  workOrderId,
  defaultCustomers
}: SendReportModalProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { customersMini } = useSelector((state) => state.customers);
  const [config, setConfig] = useState<ReportConfig>(defaultConfig);
  const [message, setMessage] = useState<string>('');
  const [customers, setCustomers] =
    useState<CustomerMiniDTO[]>(defaultCustomers);
  const [customerError, setCustomerError] = useState<boolean>(false);
  const [sending, setSending] = useState<boolean>(false);

  useEffect(() => {
    if (open) {
      setCustomers(defaultCustomers);
      setCustomerError(false);
      if (!customersMini.length) {
        dispatch(getCustomersMini());
      }
    }
  }, [open]);

  const handleToggle = (key: string) => {
    setConfig((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleSend = () => {
    if (!customers.length) {
      setCustomerError(true);
      return;
    }
    setSending(true);
    dispatch(
      sendWorkOrderReport(workOrderId, {
        config,
        message: message || undefined,
        customers
      })
    )
      .then(() => {
        showSnackBar(t('report_sent_success'), 'success');
        onClose();
      })
      .catch((err) => showSnackBar(getErrorMessage(err), 'error'))
      .finally(() => setSending(false));
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          {t('email_contractors')}
        </Typography>
        <Typography variant="subtitle2">
          {t('email_contractors_description')}
        </Typography>
      </DialogTitle>
      <DialogContent dividers sx={{ p: 3 }}>
        <Autocomplete<CustomerMiniDTO, true, false, false>
          multiple
          fullWidth
          sx={{ mb: 3 }}
          options={customersMini}
          value={customers}
          onChange={(_event, value) => {
            setCustomers(value);
            setCustomerError(!value.length);
          }}
          getOptionLabel={(option) => option.name}
          isOptionEqualToValue={(option, value) => option.id === value.id}
          filterSelectedOptions
          limitTags={5}
          renderInput={(params) => (
            <TextField
              {...params}
              fullWidth
              variant="outlined"
              label={`${t('customers')} *`}
              placeholder={t('customers')}
              autoFocus
              error={customerError}
              helperText={customerError ? t('required_field') : ''}
            />
          )}
        />
        <TextField
          fullWidth
          multiline
          minRows={3}
          sx={{ mb: 3 }}
          label={t('custom_message_optional')}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
        />
        <Typography variant="h6" gutterBottom>
          {t('customize_report_description')}
        </Typography>
        <ReportConfigFields config={config} onToggle={handleToggle} />
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" onClick={onClose}>
          {t('cancel')}
        </Button>
        <Button
          variant="contained"
          onClick={handleSend}
          disabled={sending}
          startIcon={sending ? <CircularProgress size="1rem" /> : null}
        >
          {t('send_work_order_as_pdf')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
