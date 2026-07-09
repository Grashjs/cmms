import {
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
import { useContext, useState } from 'react';
import { useDispatch } from '../../../../store';
import {
  ReportConfig,
  sendWorkOrderReport
} from '../../../../slices/workOrder';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { getErrorMessage } from '../../../../utils/api';
import ReportConfigFields from './ReportConfigFields';

interface SendReportModalProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
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
  workOrderId
}: SendReportModalProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [config, setConfig] = useState<ReportConfig>(defaultConfig);
  const [message, setMessage] = useState<string>('');
  const [sending, setSending] = useState<boolean>(false);

  const handleToggle = (key: string) => {
    setConfig((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleSend = () => {
    setSending(true);
    dispatch(
      sendWorkOrderReport(workOrderId, {
        config,
        message: message || undefined
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
        <TextField
          autoFocus
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
