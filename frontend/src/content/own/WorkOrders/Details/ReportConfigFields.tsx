import { FormControlLabel, Grid, Switch } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { ReportConfig } from '../../../../slices/workOrder';

interface ReportConfigFieldsProps {
  config: ReportConfig;
  onToggle: (key: string) => void;
}

const FIELDS: { key: keyof ReportConfig; label: string }[] = [
  { key: 'cost', label: 'cost' },
  { key: 'comments', label: 'comments' },
  { key: 'workOrderHistory', label: 'history' },
  { key: 'estimatedTime', label: 'estimated_duration' },
  { key: 'locationAddress', label: 'location' },
  { key: 'priority', label: 'priority' },
  { key: 'workOrderInformation', label: 'work_order_information' },
  { key: 'relations', label: 'links' },
  { key: 'files', label: 'files' },
  { key: 'tasks', label: 'tasks' },
  { key: 'signature', label: 'signature' }
];

export default function ReportConfigFields({
  config,
  onToggle
}: ReportConfigFieldsProps) {
  const { t }: { t: any } = useTranslation();

  return (
    <Grid container spacing={1}>
      {FIELDS.map((field) => (
        <Grid item xs={12} key={field.key}>
          <FormControlLabel
            control={
              <Switch
                checked={config[field.key]}
                onChange={() => onToggle(field.key)}
              />
            }
            label={t(field.label)}
          />
        </Grid>
      ))}
    </Grid>
  );
}
