import { useState, useRef } from 'react';
import { TextField, Popover, useTheme, Box, Typography } from '@mui/material';
import 'react-date-range/dist/styles.css';
import 'react-date-range/dist/theme/default.css';
import { useTranslation } from 'react-i18next';
import { TimePicker } from '@mui/lab';

interface OwnProps {
  value?: [number, number];
  onChange: (range: [number, number]) => void;
  fullWidth?: boolean;
  placeholder?: string;
  required?: boolean;
  error?: boolean;
  helperText?: string;
  label?: string;
}

export default function HourRangePicker({
  value,
  onChange,
  fullWidth = false,
  placeholder,
  required,
  error,
  helperText,
  label
}: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const anchorRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const theme = useTheme();

  const handleOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleStartTimeChange = (newValue: Date | null) => {
    const startHour = newValue?.getHours() || 0;
    const endHour = value?.[1] ?? 23;
    onChange([startHour, endHour]);
  };

  const handleEndTimeChange = (newValue: Date | null) => {
    const endHour = newValue?.getHours() || 23;
    const startHour = value?.[0] ?? 0;
    onChange([startHour, endHour]);
  };

  const formatHour = (hour: number): string => {
    const h = Math.floor(hour);
    return `${h.toString().padStart(2, '0')}:00`;
  };

  return (
    <>
      <div ref={anchorRef}>
        <TextField
          value={
            value?.[0] !== undefined && value?.[1] !== undefined
              ? `${formatHour(value[0])} ${t('to')} ${formatHour(value[1])}`
              : ''
          }
          onClick={handleOpen}
          InputProps={{
            readOnly: true
          }}
          fullWidth={fullWidth}
          placeholder={placeholder || t('select_hour_range')}
          required={required}
          error={error}
          helperText={helperText}
          label={label}
        />
      </div>
      <Popover
        open={open}
        anchorEl={anchorRef.current}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left'
        }}
      >
        <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="subtitle2">{t('start_time')}</Typography>
          <TimePicker
            value={
              value?.[0] !== undefined
                ? new Date(2000, 0, 1, value[0], 0)
                : null
            }
            views={['hours']}
            onChange={handleStartTimeChange}
            renderInput={(params) => <TextField {...params} fullWidth />}
          />
          <Typography variant="subtitle2">{t('end_time')}</Typography>
          <TimePicker
            value={
              value?.[1] !== undefined
                ? new Date(2000, 0, 1, value[1], 0)
                : null
            }
            views={['hours']}
            onChange={handleEndTimeChange}
            renderInput={(params) => <TextField {...params} fullWidth />}
          />
        </Box>
      </Popover>
    </>
  );
}
