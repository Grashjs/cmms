import { Box, styled } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import FeaturesAlternating from './FeaturesAlternating';
import NavBar from '../../components/NavBar';
import HeroFree from './HeroFree';
import { Footer } from '../../components/Footer';
import React from 'react';
import { useTranslation } from 'react-i18next';

export const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);

function FreeCMMSPage() {
  const { t }: { t: any } = useTranslation();
  return (
    <OverviewWrapper>
      <Helmet>
        <title>{t('free_cmms.title')}</title>
        <meta
          name="description"
          content={t('free_cmms.description')}
        />
        <meta
          name="keywords"
          content={t('free_cmms.keywords')}
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
