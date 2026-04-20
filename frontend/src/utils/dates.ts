import { addDays, format, Locale as DateLocale, startOfWeek } from 'date-fns';
import Schedule from '../models/owns/schedule';
export const dayDiff = (date1: Date, date2: Date) => {
  const diffInTime = date2.getTime() - date1.getTime();
  return Math.abs(Math.floor(diffInTime / (1000 * 3600 * 24))) - 1;
};

export const getDayAndMonth = (str: string): string => {
  const date = new Date(str).toDateString();
  const arr = date.split(' ');
  return `${arr[1]} ${arr[2]}`;
};
export const getDayAndMonthAndYear = (str: string): string => {
  const date = new Date(str).toDateString();
  const arr = date.split(' ');
  return `${arr[1]} ${arr[2]} ${arr[3]}`;
};
export const sameDay = (d1: Date, d2: Date) => {
  return (
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
  );
};

export function getWeekdays(locale: DateLocale, formatStr = 'EEEE'): string[] {
  const start = startOfWeek(new Date(), { locale, weekStartsOn: 1 });
  return [...Array(7)].map((_, i) =>
    format(addDays(start, i), formatStr, { locale, weekStartsOn: 1 })
  );
}
export function getScheduleDescription(
  schedule: Schedule,
  locale: DateLocale,
  t: (key: string, options?: any) => string
): string {
  const { frequency, recurrenceType, daysOfWeek, recurrenceBasedOn } = schedule;

  // WEEKLY → translated weekdays
  const weekdayNames = getWeekdays(locale);

  switch (recurrenceType) {
    case 'DAILY':
      return t('schedule.daily', { count: frequency });

    case 'WEEKLY': {
      const days = [
        ...((recurrenceBasedOn === 'SCHEDULED_DATE' ? daysOfWeek : []) || [])
      ] // copy array first
        .sort()
        .map((d) => weekdayNames[d])
        .join(', ');
      return t('schedule.weekly', { count: frequency, days });
    }

    case 'MONTHLY': {
      return t('schedule.monthly', { count: frequency });
    }

    case 'YEARLY': {
      return t('schedule.yearly', { count: frequency });
    }

    default:
      return '';
  }
}
