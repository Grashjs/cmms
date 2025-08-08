import { Box, styled } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import Hero from './Hero';
import Highlights from './Highlights';
import NavBar from '../../components/NavBar';
import { useEffect } from 'react';
import { isCloudVersion } from '../../config';
import { useBrand } from '../../hooks/useBrand';
import { useSelector } from '../../store';

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
  const navigate = useNavigate();
  const { isLicenseValid } = useSelector((state) => state.license);
  const brandConfig = useBrand();

  useEffect(() => {
    if (
      !isCloudVersion ||
      (isCloudVersion && isLicenseValid != null && !isLicenseValid)
    )
      console.log('license is invalid');
    // navigate('/account/login');
  }, [isCloudVersion, isLicenseValid]);

  return (
    <OverviewWrapper>
      <Helmet>
        <title>{brandConfig.name}</title>
      </Helmet>
      <NavBar />
      <Hero />
      <Highlights />
    </OverviewWrapper>
  );
}

export default Overview;
