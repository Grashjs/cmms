/**
 * The period of the same length that ends right before `start`.
 * The backend includes both ends of a range, so the previous one stops 1 ms before `start`.
 */
export const getPreviousPeriod = (
  start: Date,
  end: Date
): { start: Date; end: Date } => {
  const length = end.getTime() - start.getTime();
  return {
    start: new Date(start.getTime() - length - 1),
    end: new Date(start.getTime() - 1)
  };
};

export interface Delta {
  // relative change in percent, or difference in percentage points; null when the previous value is 0
  value: number | null;
  points: boolean;
}

/**
 * Change of a value against the previous period: relative (in %) by default, or as a difference in percentage
 * points when the values are already percentages.
 *
 * @return null when either value is missing
 */
export const computeDelta = (
  current: number | null | undefined,
  previous: number | null | undefined,
  points = false
): Delta | null => {
  if (
    current === null ||
    current === undefined ||
    previous === null ||
    previous === undefined ||
    Number.isNaN(current) ||
    Number.isNaN(previous)
  )
    return null;
  if (points) return { value: current - previous, points };
  if (previous === 0) return { value: current === 0 ? 0 : null, points };
  return { value: ((current - previous) * 100) / Math.abs(previous), points };
};
