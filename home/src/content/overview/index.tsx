"use client";

import {Box, styled} from '@mui/material';
import {useRouter} from 'next/navigation';
import {useTranslation} from 'react-i18next';
import Hero from './Hero';
import Highlights from './Highlights';
import NavBar from 'src/components/NavBar';
import {useBrand} from 'src/hooks/useBrand';
import {Footer} from 'src/components/Footer';

const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);
function Overview() {
  const { t }: { t: any } = useTranslation();
  const router = useRouter();
  const brandConfig = useBrand();

  return (
    <OverviewWrapper>
      <NavBar />
      <Hero />
      <Highlights />
      <Footer />
    </OverviewWrapper>
  );
}

export default Overview;
