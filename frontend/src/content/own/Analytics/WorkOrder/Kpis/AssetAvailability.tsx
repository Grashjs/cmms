import {
  Link,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import AnalyticsCard from '../../AnalyticsCard';
import Loading from '../../Loading';
import { useSelector } from '../../../../../store';
import { roundDecimals } from '../../../../../utils/formatters';

// availabilities below this percentage are highlighted
export const AVAILABILITY_THRESHOLD = 95;

function AssetAvailability() {
  const { t }: { t: any } = useTranslation();
  const { availability, loading } = useSelector((state) => state.kpiAnalytics);

  return (
    <AnalyticsCard
      title={t('asset_availability')}
      description="asset_availability_description"
    >
      {loading.availability ? (
        <Loading />
      ) : availability.length === 0 ? (
        <Typography sx={{ py: 2 }}>{t('no_data')}</Typography>
      ) : (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('asset')}</TableCell>
                <TableCell align="right">{t('availability')}</TableCell>
                <TableCell align="right">{t('downtime_hours')}</TableCell>
                <TableCell align="right">{t('downtime_events')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {availability.map((asset) => {
                const low = asset.availability < AVAILABILITY_THRESHOLD;
                return (
                  <TableRow key={asset.id}>
                    <TableCell>
                      <Link
                        component={RouterLink}
                        to={`/app/assets/${asset.id}/details`}
                      >
                        {asset.name}
                      </Link>
                    </TableCell>
                    <TableCell align="right">
                      <Typography
                        component="span"
                        fontWeight={low ? 'bold' : 'normal'}
                        color={low ? 'error' : 'inherit'}
                      >
                        {`${roundDecimals(asset.availability)}%`}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      {roundDecimals(asset.downtime / 3600)}
                    </TableCell>
                    <TableCell align="right">{asset.downtimeEvents}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </AnalyticsCard>
  );
}

export default AssetAvailability;
