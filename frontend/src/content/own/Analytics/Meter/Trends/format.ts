import { format } from 'date-fns';
import { useTranslation } from 'react-i18next';
import useDateLocale from '../../../../../hooks/useDateLocale';
import { MeterInterval } from '../../../../../models/owns/analytics/meter';

const LABEL_FORMATS: Record<MeterInterval, string> = {
  HOUR: 'dd MMM HH:mm',
  DAY: 'dd MMM',
  WEEK: 'dd MMM',
  MONTH: 'MMM yyyy'
};

/**
 * Readings in the language of the user, with the unit of the meter, and interval labels for the charts.
 */
export default function useMeterFormat(unit?: string) {
  const { i18n }: { i18n: any } = useTranslation();
  const dateLocale = useDateLocale();
  const numberFormat = new Intl.NumberFormat(i18n.language, {
    maximumFractionDigits: 2
  });
  const formatNumber = (value: number | null | undefined): string =>
    value === null || value === undefined ? '—' : numberFormat.format(value);
  const formatValue = (value: number | null | undefined): string =>
    value === null || value === undefined
      ? '—'
      : unit
      ? `${formatNumber(value)} ${unit}`
      : formatNumber(value);
  const formatLabel = (date: string, interval: MeterInterval): string =>
    format(new Date(date), LABEL_FORMATS[interval], { locale: dateLocale });
  const formatDate = (date: string): string =>
    format(new Date(date), 'dd MMM yyyy HH:mm', { locale: dateLocale });
  return { formatNumber, formatValue, formatLabel, formatDate };
}
