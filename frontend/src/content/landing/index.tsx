import { Box, styled } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import Highlights from '../overview/Highlights';
import NavBar from '../../components/NavBar';
import HeroFree from './HeroFree';
import { Footer } from '../../components/Footer';

const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);

function LandingPage() {
  return (
    <OverviewWrapper>
      <Helmet>
        <title>Free CMMS - Stop Fighting Fires, Start Preventing Them!</title>
      </Helmet>
      <NavBar />
      <HeroFree />
      <Highlights hidePricing />
      <Footer />
    </OverviewWrapper>
  );
}

export default LandingPage;
