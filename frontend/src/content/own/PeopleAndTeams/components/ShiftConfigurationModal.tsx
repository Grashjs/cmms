import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  InputAdornment,
  Stack,
  Switch,
  Tab,
  Tabs,
  TextField,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import {
  ShiftConfigurationShowDTO,
  ShiftDayConfiguration,
  ShiftException,
  UserResponseDTO
} from '../../../../models/user';
import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew';
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';
import { addWeeks, format, startOfWeek, subWeeks } from 'date-fns';
import { useDispatch } from '../../../../store';
import { patchShiftConfiguration } from '../../../../slices/shiftConfiguration';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import userSlice from '../../../../slices/user';

const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
];

interface DayRowProps {
  dayOfWeek: string;
  date?: string;
  enabled: boolean;
  hours: number;
  minutes: number;
  onToggle: (enabled: boolean) => void;
  onHoursChange: (hours: number) => void;
  onMinutesChange: (minutes: number) => void;
}

function DayRow({
  dayOfWeek,
  date,
  enabled,
  hours,
  minutes,
  onToggle,
  onHoursChange,
  onMinutesChange
}: DayRowProps) {
  const { t }: { t: any } = useTranslation();
  const { getFormattedDate } = useContext(CompanySettingsContext);

  const handleHoursChange = (value: string) => {
    const num = parseInt(value, 10);
    if (!isNaN(num) && num >= 0 && num <= 24) {
      onHoursChange(num);
    } else if (value === '') {
      onHoursChange(0);
    }
  };

  const handleMinutesChange = (value: string) => {
    const num = parseInt(value, 10);
    if (!isNaN(num) && num >= 0 && num <= 59) {
      onMinutesChange(num);
    } else if (value === '') {
      onMinutesChange(0);
    }
  };

  return (
    <Stack
      direction="row"
      alignItems="center"
      justifyContent={'space-between'}
      spacing={2}
      sx={{ py: 1, px: 2 }}
    >
      <Stack direction={'row'}>
        <Switch
          checked={enabled}
          onChange={(e) => onToggle(e.target.checked)}
        />
        <Typography sx={{ minWidth: date ? 100 : 90, fontWeight: 500 }}>
          {t(dayOfWeek.toLowerCase())}
        </Typography>
        {date && (
          <Typography sx={{ minWidth: 90, color: 'text.secondary' }}>
            {getFormattedDate(date, true)}
          </Typography>
        )}
      </Stack>
      <Stack direction={'row'} spacing={2}>
        <TextField
          type="number"
          value={hours}
          onChange={(e) => handleHoursChange(e.target.value)}
          disabled={!enabled}
          size="small"
          sx={{ width: 90 }}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                {t('hours_abbrev')}
              </InputAdornment>
            )
          }}
          inputProps={{ min: 0, max: 24, style: { textAlign: 'right' } }}
        />
        <TextField
          type="number"
          value={minutes}
          onChange={(e) => handleMinutesChange(e.target.value)}
          disabled={!enabled}
          size="small"
          sx={{ width: 90 }}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                {t('minutes_abbrev')}
              </InputAdornment>
            )
          }}
          inputProps={{ min: 0, max: 59, style: { textAlign: 'right' } }}
        />
      </Stack>
    </Stack>
  );
}

interface PropsType {
  user: UserResponseDTO;
  open: boolean;
  onClose: () => void;
}

function ShiftConfigurationModal({ user, open, onClose }: PropsType) {
  const { t }: { t: any } = useTranslation();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const [currentTab, setCurrentTab] = useState<string>('default');
  const [enabled, setEnabled] = useState<boolean>(
    user.shiftConfiguration?.enabled ?? true
  );
  const [days, setDays] = useState<ShiftDayConfiguration[]>([]);
  const [exceptions, setExceptions] = useState<ShiftException[]>([]);
  const [weekStart, setWeekStart] = useState<Date>(
    startOfWeek(new Date(), { weekStartsOn: 1 })
  );

  useEffect(() => {
    if (user.shiftConfiguration) {
      setDays(
        DAYS_OF_WEEK.map((day) => {
          const existing = user.shiftConfiguration.days.find(
            (d) => d.dayOfWeek === day
          );
          return (
            existing ?? {
              dayOfWeek: day,
              availabilityMinutes: 8 * 60,
              enabled: true
            }
          );
        })
      );
      setExceptions(user.shiftConfiguration.exceptions ?? []);
    } else {
      setDays(
        DAYS_OF_WEEK.map((day) => ({
          dayOfWeek: day,
          availabilityMinutes: 8 * 60,
          enabled: true
        }))
      );
      setExceptions([]);
    }
  }, [user.shiftConfiguration]);

  const handleTabsChange = (_event: React.ChangeEvent<{}>, value: string) => {
    setCurrentTab(value);
  };

  const handleDayToggle = (dayOfWeek: string, enabled: boolean) => {
    setDays((prev) =>
      prev.map((d) => (d.dayOfWeek === dayOfWeek ? { ...d, enabled } : d))
    );
  };

  const handleDayHoursChange = (dayOfWeek: string, hours: number) => {
    setDays((prev) =>
      prev.map((d) =>
        d.dayOfWeek === dayOfWeek
          ? {
              ...d,
              availabilityMinutes: hours * 60 + (d.availabilityMinutes % 60)
            }
          : d
      )
    );
  };

  const handleDayMinutesChange = (dayOfWeek: string, minutes: number) => {
    setDays((prev) =>
      prev.map((d) =>
        d.dayOfWeek === dayOfWeek
          ? {
              ...d,
              availabilityMinutes:
                Math.floor(d.availabilityMinutes / 60) * 60 + minutes
            }
          : d
      )
    );
  };

  const handleExceptionToggle = (date: string, enabled: boolean) => {
    setExceptions((prev) => {
      const existing = prev.find((e) => e.exceptionDate === date);
      if (existing) {
        return prev.map((e) =>
          e.exceptionDate === date
            ? {
                ...e,
                enabled
              }
            : e
        );
      }
      return [
        ...prev,
        { exceptionDate: date, enabled, availabilityMinutes: 8 * 60 }
      ];
    });
  };

  const handleExceptionHoursChange = (date: string, hours: number) => {
    setExceptions((prev) => {
      const existing = prev.find((e) => e.exceptionDate === date);
      if (existing) {
        return prev.map((e) =>
          e.exceptionDate === date
            ? {
                ...e,
                availabilityMinutes: hours * 60 + (e.availabilityMinutes % 60)
              }
            : e
        );
      }
      return [
        ...prev,
        { exceptionDate: date, availabilityMinutes: hours * 60, enabled: true }
      ];
    });
  };

  const handleExceptionMinutesChange = (date: string, minutes: number) => {
    setExceptions((prev) => {
      const existing = prev.find((e) => e.exceptionDate === date);
      if (existing) {
        return prev.map((e) =>
          e.exceptionDate === date
            ? {
                ...e,
                availabilityMinutes:
                  Math.floor(e.availabilityMinutes / 60) * 60 + minutes
              }
            : e
        );
      }
      return [
        ...prev,
        { exceptionDate: date, availabilityMinutes: minutes, enabled: true }
      ];
    });
  };

  const weekDays = useMemo(() => {
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(weekStart);
      d.setDate(d.getDate() + i);
      return d;
    });
  }, [weekStart]);

  const getExceptionForDate = (dateStr: string) => {
    return exceptions.find((e) => e.exceptionDate === dateStr);
  };

  const totalWeeklyMinutes = days
    .filter((d) => d.enabled)
    .reduce((sum, d) => sum + d.availabilityMinutes, 0);
  const totalHours = Math.floor(totalWeeklyMinutes / 60);
  const totalMins = totalWeeklyMinutes % 60;

  const handleSave = async () => {
    try {
      await dispatch(
        patchShiftConfiguration(user.id, { days, exceptions, enabled })
      );
      dispatch(
        userSlice.actions.editUser({
          user: {
            ...user,
            shiftConfiguration: {
              ...user.shiftConfiguration,
              days,
              exceptions
            } as ShiftConfigurationShowDTO
          }
        })
      );
      showSnackBar(t('changes_saved_success'), 'success');
      onClose();
    } catch {
      showSnackBar(t("The Shift Configuration couldn't be saved"), 'error');
    }
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle sx={{ p: 3 }}>
        <Stack
          direction="row"
          alignItems="center"
          justifyContent="space-between"
        >
          <Typography variant="h4">
            {t('customize_shift')} ({user.firstName + ' ' + user.lastName})
          </Typography>
          <Stack direction="row" alignItems="center" spacing={1}>
            <Typography>{t('scheduled')}</Typography>
            <Switch
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
            />
          </Stack>
        </Stack>
      </DialogTitle>
      <Tabs
        value={currentTab}
        onChange={handleTabsChange}
        sx={{ px: 3 }}
        textColor="primary"
        indicatorColor="primary"
      >
        <Tab label={t('default_schedule')} value="default" />
        <Tab label={t('custom_capacity_by_week')} value="custom" />
      </Tabs>
      <DialogContent sx={{ p: 1 }}>
        {currentTab === 'default' && (
          <Box>
            {days.map((day) => (
              <DayRow
                key={day.dayOfWeek}
                dayOfWeek={day.dayOfWeek}
                enabled={day.enabled}
                hours={Math.floor(day.availabilityMinutes / 60)}
                minutes={day.availabilityMinutes % 60}
                onToggle={(enabled) => handleDayToggle(day.dayOfWeek, enabled)}
                onHoursChange={(hours) =>
                  handleDayHoursChange(day.dayOfWeek, hours)
                }
                onMinutesChange={(minutes) =>
                  handleDayMinutesChange(day.dayOfWeek, minutes)
                }
              />
            ))}
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle1" sx={{ textAlign: 'right', px: 2 }}>
              {t('total_weekly_capacity', {
                hours: totalHours,
                minutes: totalMins
              })}
            </Typography>
          </Box>
        )}
        {currentTab === 'custom' && (
          <Box>
            <Stack
              direction="row"
              alignItems="center"
              justifyContent="center"
              spacing={2}
              sx={{ mb: 2 }}
            >
              <IconButton
                onClick={() => setWeekStart((prev) => subWeeks(prev, 1))}
              >
                <ArrowBackIosNewIcon />
              </IconButton>
              <Typography
                variant="h6"
                sx={{ minWidth: 200, textAlign: 'center' }}
              >
                {getFormattedDate(weekDays[0].toISOString(), true)} -{' '}
                {getFormattedDate(weekDays[6].toISOString(), true)}
              </Typography>
              <IconButton
                onClick={() => setWeekStart((prev) => addWeeks(prev, 1))}
              >
                <ArrowForwardIosIcon />
              </IconButton>
            </Stack>
            {weekDays.map((weekDay) => {
              const dateStr = format(weekDay, 'yyyy-MM-dd');
              const equivalentDayOfWeek = days.find(
                (day) =>
                  day.dayOfWeek ===
                  DAYS_OF_WEEK[
                    weekDay.getDay() === 0 ? 6 : weekDay.getDay() - 1
                  ]
              );
              const exception = getExceptionForDate(dateStr);
              const isEnabled = exception
                ? exception.enabled
                : equivalentDayOfWeek!.enabled;
              const minutes =
                exception?.availabilityMinutes ??
                equivalentDayOfWeek!.availabilityMinutes;
              return (
                <DayRow
                  key={dateStr}
                  dayOfWeek={
                    DAYS_OF_WEEK[
                      weekDay.getDay() === 0 ? 6 : weekDay.getDay() - 1
                    ]
                  }
                  date={dateStr}
                  enabled={isEnabled}
                  hours={Math.floor(minutes / 60)}
                  minutes={minutes % 60}
                  onToggle={(enabled) =>
                    handleExceptionToggle(dateStr, enabled)
                  }
                  onHoursChange={(hours) =>
                    handleExceptionHoursChange(dateStr, hours)
                  }
                  onMinutesChange={(minutes) =>
                    handleExceptionMinutesChange(dateStr, minutes)
                  }
                />
              );
            })}
          </Box>
        )}
      </DialogContent>
      <Box sx={{ p: 2, display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
        <Button variant="outlined" onClick={onClose}>
          {t('cancel')}
        </Button>
        <Button variant="contained" onClick={handleSave}>
          {t('save')}
        </Button>
      </Box>
    </Dialog>
  );
}

export default ShiftConfigurationModal;
