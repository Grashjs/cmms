import Schedule from '../models/schedule';

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * The next date this schedule will raise a work order, or null when it never
 * will again — because it is disabled, has no start date or frequency, or has
 * already passed its end date.
 *
 * `frequency` is a whole number of days. Rather than stepping one interval at
 * a time from the start date, which for a daily schedule running since last
 * year means hundreds of iterations, the number of elapsed intervals is
 * computed directly.
 */
export const getNextOccurrence = (schedule?: Schedule): Date | null => {
  if (!schedule || schedule.disabled) return null;
  if (!schedule.startsOn || !schedule.frequency || schedule.frequency <= 0) {
    return null;
  }

  const start = new Date(schedule.startsOn);
  if (Number.isNaN(start.getTime())) return null;

  const now = Date.now();
  const intervalMs = schedule.frequency * DAY_MS;

  let next: Date;
  if (start.getTime() >= now) {
    next = start;
  } else {
    const elapsed = Math.ceil((now - start.getTime()) / intervalMs);
    next = new Date(start.getTime() + elapsed * intervalMs);
  }

  if (schedule.endsOn) {
    const end = new Date(schedule.endsOn);
    if (!Number.isNaN(end.getTime()) && next > end) return null;
  }
  return next;
};

/** Whole days from today until `date`; negative once it is in the past. */
export const daysUntil = (date: Date): number => {
  const startOfToday = new Date();
  startOfToday.setHours(0, 0, 0, 0);
  const startOfTarget = new Date(date);
  startOfTarget.setHours(0, 0, 0, 0);
  return Math.round(
    (startOfTarget.getTime() - startOfToday.getTime()) / DAY_MS
  );
};

/**
 * "Every 30 days" and the like. Common intervals get their own wording, since
 * "every 7 days" is how a computer describes a weekly schedule.
 */
export const describeFrequency = (
  schedule: Schedule | undefined,
  t: (key: string, options?: Record<string, unknown>) => string
): string => {
  const frequency = schedule?.frequency;
  if (!frequency || frequency <= 0) return t('no_schedule');
  if (frequency === 1) return t('every_day');
  if (frequency === 7) return t('every_week');
  if (frequency === 14) return t('every_two_weeks');
  if (frequency === 30 || frequency === 31) return t('every_month');
  if (frequency === 365) return t('every_year');
  return t('every_n_days', { count: frequency });
};
