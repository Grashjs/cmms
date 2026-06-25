import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  Switch,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useDispatch } from '../../../../store';
import { getPDFReport, ReportConfig } from '../../../../slices/workOrder';

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
      .then((url: string) => {
        window.open(url);
        onClose();
      })
      .finally(() => setGenerating(false));
  };

  const fields: { key: keyof ReportConfig; label: string }[] = [
    { key: 'cost', label: t('cost') },
    { key: 'comments', label: t('comments') },
    { key: 'workOrderHistory', label: t('history') },
    { key: 'estimatedTime', label: t('estimated_duration') },
    { key: 'locationAddress', label: t('location') },
    { key: 'priority', label: t('priority') },
    { key: 'workOrderInformation', label: t('work_order_information') },
    { key: 'relations', label: t('links') },
    { key: 'files', label: t('files') },
    { key: 'tasks', label: t('tasks') },
    { key: 'signature', label: t('signature') }
  ];

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
        <Grid container spacing={1}>
          {fields.map((field) => (
            <Grid item xs={12} key={field.key}>
              <FormControlLabel
                control={
                  <Switch
                    checked={config[field.key]}
                    onChange={() => handleToggle(field.key)}
                  />
                }
                label={field.label}
              />
            </Grid>
          ))}
        </Grid>
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
