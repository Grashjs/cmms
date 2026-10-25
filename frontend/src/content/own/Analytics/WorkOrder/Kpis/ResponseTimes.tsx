import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { useSelector } from '../../../../../store';
import { roundDecimals } from '../../../../../utils/formatters';

const hours = (value: number | null) =>
  value === null || value === undefined ? '—' : roundDecimals(value);

function ResponseTimes() {
  const { t }: { t: any } = useTranslation();
  const { kpis, loading } = useSelector((state) => state.kpiAnalytics);

  return (
    <AnalyticsCard
      title={t('response_times_by_priority')}
      description="response_times_by_priority_description"
    >
      {loading.kpis ? (
        <Loading />
      ) : (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('priority')}</TableCell>
                <TableCell align="right">{t('reactive_work_orders')}</TableCell>
                <TableCell align="right">{t('completed')}</TableCell>
                <TableCell align="right">{t('response_time_hours')}</TableCell>
                <TableCell align="right">
                  {t('resolution_time_hours')}
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {kpis.byPriority.map((row) => (
                <TableRow key={row.priority}>
                  <TableCell>{t(row.priority)}</TableCell>
                  <TableCell align="right">{row.count}</TableCell>
                  <TableCell align="right">{row.completed}</TableCell>
                  <TableCell align="right">{hours(row.mtta)}</TableCell>
                  <TableCell align="right">{hours(row.mttr)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </AnalyticsCard>
  );
}

export default ResponseTimes;
