import {
  Box,
  Card,
  Divider,
  Grid,
  Link,
  Stack,
  styled,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { AssetDTO } from '../../../../models/owns/asset';
import { UserMiniDTO } from '../../../../models/user';
import { Customer } from '../../../../models/owns/customer';
import { Vendor } from '../../../../models/owns/vendor';
import Team from '../../../../models/owns/team';
import {
  getCustomerUrl,
  getTeamUrl,
  getUserUrl,
  getVendorUrl
} from '../../../../utils/urlPaths';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import AssetStatusTag from '../components/AssetStatusTag';
import Loading from '../../Analytics/Loading';

interface PropsType {
  asset: AssetDTO;
  loading: boolean;
}

const LabelWrapper = styled(Box)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(10)};
    font-weight: bold;
    text-transform: uppercase;
    border-radius: ${theme.general.borderRadiusSm};
    padding: ${theme.spacing(0.9, 1.5, 0.7)};
    line-height: 1;
  `
);
const AssetDetails = ({ asset, loading }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { getFormattedDate, getFormattedCurrency } = useContext(
    CompanySettingsContext
  );
  const informationFields = [
    { label: t('name'), value: asset?.name },
    { label: t('description'), value: asset?.description },
    { label: t('category'), value: asset?.category?.name },
    { label: t('model'), value: asset?.model },
    { label: t('serial_number'), value: asset?.serialNumber },
    { label: t('power'), value: asset?.power },
    { label: t('manufacturer'), value: asset?.manufacturer },
    {
      label: t('acquisition_cost'),
      value: asset?.acquisitionCost
        ? getFormattedCurrency(asset?.acquisitionCost)
        : null
    },
    { label: t('area'), value: asset?.area },
    { label: t('barcode'), value: asset?.barCode }
  ];
  const moreInfosFields = [
    {
      label: t('additional_information'),
      value: asset?.additionalInfos
    },
    {
      label: t('placed_in_service'),
      value: getFormattedDate(asset?.inServiceDate)
    },
    {
      label: t('warranty_expiration'),
      value: getFormattedDate(asset?.warrantyExpirationDate)
    }
  ];
  const BasicField = ({
    label,
    value
  }: {
    label: string | number;
    value: string | number;
  }) => {
    return value ? (
      <Grid item xs={12}>
        <Stack spacing={5} direction="row">
          <Typography variant="h6" fontWeight="bold">
            {label}
          </Typography>
          <Typography variant="h6">{value}</Typography>
        </Stack>
        <Divider sx={{ mt: 1 }} />
      </Grid>
    ) : null;
  };
  const ListField = <T extends { id: number }>({
    values,
    label,
    getHref,
    getValueLabel
  }: {
    values: T[];
    label: string;
    getHref: (value: T) => string;
    getValueLabel: (value: T) => string;
  }) => {
    return (
      !!values?.length && (
        <Grid item xs={12}>
          <Stack spacing={5} direction="row">
            <Typography variant="h6" fontWeight="bold">
              {label}
            </Typography>
            <Stack spacing={1} direction="row">
              {values.map((value, index) => (
                <Stack key={value.id} spacing={1} direction="row">
                  <Link href={getHref(value)} variant="h6">
                    {getValueLabel(value)}
                  </Link>
                  {index !== values.length - 1 && (
                    <Typography variant="h6">,</Typography>
                  )}
                </Stack>
              ))}
            </Stack>
          </Stack>
          <Divider sx={{ mt: 1 }} />
        </Grid>
      )
    );
  };
  if (loading)
    return (
      <Box
        sx={{
          height: '50vh',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center'
        }}
      >
        <Loading />
      </Box>
    );
  return (
    <Box sx={{ px: 4 }}>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Card sx={{ p: 2 }}>
            <Grid container spacing={2} padding={2}>
              {asset?.image && (
                <Grid item xs={12}>
                  <img width="auto" height="300px" src={asset.image.url} />
                </Grid>
              )}
              <Grid item xs={12}>
                <Stack direction={'row'} spacing={2} alignItems={'center'}>
                  <Typography variant="h3">{t('asset_information')}</Typography>
                  {asset && <AssetStatusTag status={asset.status} />}
                </Stack>
              </Grid>
              {informationFields.map((field) => (
                <BasicField
                  key={field.label}
                  label={field.label}
                  value={field.value}
                />
              ))}
              <Grid item xs={12}>
                <Typography variant="h3">{t('more_informations')}</Typography>
              </Grid>
              {moreInfosFields.map((field) => (
                <BasicField
                  key={field.label}
                  label={field.label}
                  value={field.value}
                />
              ))}
              {asset?.primaryUser && (
                <Grid item xs={12}>
                  <Stack spacing={5} direction="row">
                    <Typography variant="h6" fontWeight="bold">
                      {t('primary_worker')}
                    </Typography>
                    <Link
                      key={asset.primaryUser.id}
                      href={`/app/people-teams/people/${asset.primaryUser.id}`}
                      variant="h6"
                    >
                      {`${asset.primaryUser.firstName} ${asset.primaryUser.lastName}`}
                    </Link>
                  </Stack>
                  <Divider sx={{ mt: 1 }} />
                </Grid>
              )}
              {asset?.location && (
                <Grid item xs={12}>
                  <Stack spacing={5} direction="row">
                    <Typography variant="h6" fontWeight="bold">
                      {t('location')}
                    </Typography>
                    <Link
                      href={`/app/locations/${asset.location.id}`}
                      variant="h6"
                    >
                      {asset.location.name}
                    </Link>
                  </Stack>
                  <Divider sx={{ mt: 1 }} />
                </Grid>
              )}
              <ListField
                values={asset?.assignedTo}
                label={t('assigned_to')}
                getHref={(user: UserMiniDTO) => getUserUrl(user.id)}
                getValueLabel={(user: UserMiniDTO) =>
                  `${user.firstName} ${user.lastName}`
                }
              />
              <ListField
                values={asset?.customers}
                label={t('customers')}
                getHref={(customer: Customer) => getCustomerUrl(customer.id)}
                getValueLabel={(customer: Customer) => customer.name}
              />
              <ListField
                values={asset?.vendors}
                label={t('vendors')}
                getHref={(vendor: Vendor) => getVendorUrl(vendor.id)}
                getValueLabel={(vendor: Vendor) => vendor.companyName}
              />
              <ListField
                values={asset?.teams}
                label={t('teams')}
                getHref={(team: Team) => getTeamUrl(team.id)}
                getValueLabel={(team: Team) => team.name}
              />
            </Grid>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AssetDetails;
