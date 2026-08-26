import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useDispatch } from '../../../../store';
import { getPDFReport, ReportConfig } from '../../../../slices/workOrder';
import ReportConfigFields from './ReportConfigFields';

interface ReportConfigModalProps {
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

export default function ReportConfigModal({
  open,
  onClose,
  workOrderId
}: ReportConfigModalProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const [config, setConfig] = useState<ReportConfig>(defaultConfig);
  const [generating, setGenerating] = useState<boolean>(false);

  const handleToggle = (key: string) => {
    setConfig((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleGenerate = () => {
    setGenerating(true);
    dispatch(getPDFReport(workOrderId, config))
      .then(() => {
        onClose();
      })
      .finally(() => setGenerating(false));
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          {t('pdf_report')}
        </Typography>
        <Typography variant="subtitle2">
          {t('customize_report_description')}
        </Typography>
      </DialogTitle>
      <DialogContent dividers sx={{ p: 3 }}>
        <ReportConfigFields config={config} onToggle={handleToggle} />
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" onClick={onClose}>
          {t('cancel')}
        </Button>
        <Button
          variant="contained"
          onClick={handleGenerate}
          disabled={generating}
          startIcon={generating ? <CircularProgress size="1rem" /> : null}
        >
          {t('to_export')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
