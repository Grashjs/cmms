import { Box, Card, CardContent, TextField } from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers-pro';
import { AdapterDayjs } from '@mui/x-date-pickers-pro/AdapterDayjs';
import { DateRangePicker } from '@mui/x-date-pickers-pro/DateRangePicker';
import { useTranslation } from 'react-i18next';

interface OwnProps {
  start: Date;
  end: Date;
  setStart: (date: Date) => void;
  setEnd: (date: Date) => void;
}

export default function({ start, end, setEnd, setStart }: OwnProps) {
  const { t }: { t: any } = useTranslation();

  return (
    <Card>
      <CardContent sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <LocalizationProvider
          localeText={{ start: t('start'), end: t('end') }}
          dateAdapter={AdapterDayjs}
        >
          <DateRangePicker
            value={[start, end]}
            onChange={(newValue) => {
              setStart(newValue[0]);
              setEnd(newValue[1]);
            }}
            renderInput={(startProps, endProps) => (
              <>
                <TextField {...startProps} />
                <Box sx={{ mx: 2 }}> {t('to')} </Box>
                <TextField {...endProps} />
              </>
            )}
          />
        </LocalizationProvider>
      </CardContent>
    </Card>
  );
}