'use client';
import { Box, styled } from '@mui/material';
import Highlights from '../../../../src/content/overview/Highlights';
import NavBar from '../../../../src/components/NavBar';
import HeroFree from '../../../components/HeroFree';
import { Footer } from '../../../../src/components/Footer';
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
      <NavBar />
      <HeroFree />
      <Highlights hidePricing />
      <Footer />
    </OverviewWrapper>
  );
}

export default FreeCMMSPage;
