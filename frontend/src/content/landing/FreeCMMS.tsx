import { Box, styled } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import FeaturesAlternating from './FeaturesAlternating';
import NavBar from '../../components/NavBar';
import HeroFree from './HeroFree';
import { Footer } from '../../components/Footer';
import React from 'react';

export const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);

function FreeCMMSPage() {
  return (
    <OverviewWrapper>
      <Helmet>
        <title>Atlas Free CMMS Software | No credit card required</title>
        <meta
          name="description"
          content="Free CMMS software — not a trial. Manage maintenance, assets and work orders instantly. No credit card required."
        />
        <meta
          name="keywords"
          content="free cmms, maintenance management software, free work order software, asset tracking, preventive maintenance"
        />
        <link rel={'canonical'} href={'https://atlas-cmms.com/free-cmms'} />
      </Helmet>
      <NavBar />
      <HeroFree />
      <FeaturesAlternating />
      <Footer />
    </OverviewWrapper>
  );
}

export default FreeCMMSPage;
