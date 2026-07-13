import { useContext, useEffect } from 'react';

import { Helmet } from 'react-helmet-async';

import { Box, Grid } from '@mui/material';
import CompanyCover from './CompanyCover';
import CompanyDetails from './CompanyDetails';
import CompanyPlan from './CompanyPlan';
import { TitleContext } from '../../../contexts/TitleContext';
import { useTranslation } from 'react-i18next';
import useAuth from '../../../hooks/useAuth';
import { isCloudVersion } from '../../../config';

function CompanyProfile() {
  const { t }: { t: any } = useTranslation();
  const { company, user } = useAuth();
  const { setTitle } = useContext(TitleContext);

  useEffect(() => {
    setTitle(t('company'));
  }, []);
  if (!company) {
    return null;
  }
  return (
    <>
      <Helmet>
        <title>{company.name} - Company Details</title>
      </Helmet>
      <Box
        sx={{
          mt: 3
        }}
      >
        <Grid
          sx={{
            px: 4
          }}
          container
          direction="row"
          justifyContent="center"
          alignItems="stretch"
          spacing={1}
        >
          <Grid item xs={12}>
            <CompanyCover image={company.logo?.url} />
          </Grid>
          <Grid item xs={12}>
            <CompanyDetails company={company} />
          </Grid>
          {isCloudVersion && user.ownsCompany && (
            <Grid item xs={12}>
              <CompanyPlan plan={company.subscription.subscriptionPlan} />
            </Grid>
          )}
        </Grid>
      </Box>
    </>
  );
}

export default CompanyProfile;
