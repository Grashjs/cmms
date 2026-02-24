import { Box, styled } from '@mui/material';
import FeaturesAlternating from './FeaturesAlternating';
import NavBar from '../../components/NavBar';
import HeroFree from './HeroFree';
import { Footer } from '../../components/Footer';
import React from 'react';
import { useTranslation } from 'react-i18next';
import SharedHelmet from './components/SharedHelmet';

export const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);

function FreeCMMSPage() {
  const { t } = useTranslation();
  return (
    <OverviewWrapper>
      <SharedHelmet
        path="free-cmms"
        title={t('free_cmms.title')}
        description={t('free_cmms.description')}
        keywords={t('free_cmms.keywords')}
      />
      <NavBar />
      <HeroFree />
      <FeaturesAlternating />
      <Footer />
    </OverviewWrapper>
  );
}

export default FreeCMMSPage;
