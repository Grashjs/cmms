'use client';
import {
  Box,
  styled,
} from '@mui/material';
import { useRouter } from 'next/navigation';
import { useTranslation } from 'react-i18next';
import Hero from '../components/overview/Hero';
import Highlights from '../components/overview/Highlights';
import NavBar from '../../../src/components/NavBar';
import { useEffect } from 'react';
import { isCloudVersion } from '../../../src/config';
import { useBrand } from '../../../src/hooks/useBrand';
import { Footer } from '../../../src/components/Footer';

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

  useEffect(() => {
    if (!isCloudVersion) router.push('/account/login');
  }, [isCloudVersion, router]);

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