import { useEffect, useState } from 'react';
import { FilterField } from '../../../models/owns/page';
import { getPreviousPeriod } from '../../../utils/periodComparison';

type Fetcher<T> = (
  start: Date,
  end: Date,
  companyId?: number,
  filterFields?: FilterField[]
) => Promise<T>;

export interface PreviousPeriod<T> {
  start: Date;
  end: Date;
  // null while loading or if the request failed
  data: T | null;
}

/**
 * Fetches the same report for the previous period of equal length, to show the change on the summary cards.
 * The result is kept in the component: the redux state keeps holding the current period only.
 */
export default function usePreviousPeriod<T>(
  fetcher: Fetcher<T>,
  start: Date,
  end: Date,
  companyId?: number,
  filterFields?: FilterField[]
): PreviousPeriod<T> {
  const period = getPreviousPeriod(start, end);
  const [data, setData] = useState<T | null>(null);

  useEffect(() => {
    let active = true;
    setData(null);
    fetcher(period.start, period.end, companyId, filterFields)
      .then((result) => {
        if (active) setData(result);
      })
      .catch(() => {
        if (active) setData(null);
      });
    return () => {
      active = false;
    };
  }, [start, end, companyId, filterFields]);

  return { ...period, data };
}
