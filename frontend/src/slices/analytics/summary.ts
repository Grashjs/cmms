import { format } from 'date-fns';
import type { FilterField } from 'src/models/owns/page';
import api from '../../utils/api';

const companyQuery = (companyId?: number) =>
  companyId ? `?companyId=${companyId}` : '';

/**
 * Downloads the maintenance summary of the period as a PDF: the main figures compared with the previous period of
 * the same length, with the selected filters.
 */
export const downloadMaintenanceSummary = async (
  start: Date,
  end: Date,
  companyId?: number,
  filterFields: FilterField[] = []
): Promise<void> => {
  const response = await api.post<Response>(
    `analytics/summary/pdf${companyQuery(companyId)}`,
    { start, end, filterFields },
    { raw: true }
  );
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  // Content-Disposition is not exposed to cross-origin requests
  link.download = `maintenance-summary-${format(start, 'yyyy-MM-dd')}.pdf`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};
