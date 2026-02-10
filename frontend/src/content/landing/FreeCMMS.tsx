import { Box, styled } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import Highlights from '../overview/Highlights';
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
        <title>Free CMMS - Stop Fighting Fires, Start Preventing Them!</title>
        <meta
          name="description"
          content={
            'Enterprise-grade asset management and work order software with no credit card or payment information required.'
          }
        />
        <link rel={'canonical'} href={'https://atlas-cmms.com/free-cmms'} />
      </Helmet>
      <NavBar />
      <HeroFree />
      <Highlights hidePricing />
      <Footer />
    </OverviewWrapper>
  );
}

export default FreeCMMSPage;
