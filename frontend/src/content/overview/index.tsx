import {
  Box,
  Container,
  Grid,
  Link,
  styled,
  Typography,
  Stack
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import Hero from './Hero';
import Highlights from './Highlights';
import NavBar from '../../components/NavBar';
import { useEffect } from 'react';
import { IS_ORIGINAL_CLOUD, isCloudVersion } from '../../config';
import { useBrand } from '../../hooks/useBrand';
import { useSelector } from '../../store';
import {
  Facebook,
  Twitter,
  Instagram,
  Phone,
  Mail,
  Sms,
  LinkedIn
} from '@mui/icons-material';
import { Footer } from 'src/components/Footer';

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
  const brandConfig = useBrand();

  return (
    <OverviewWrapper>
      <Helmet>
        <title>
          {brandConfig.name}
          {IS_ORIGINAL_CLOUD
            ? ' - Open-Source Maintenance Management Software | Free EAM Solution'
            : ''}
        </title>
      </Helmet>
      <NavBar />
      <Hero />
      <Highlights />
      <Footer />
    </OverviewWrapper>
  );
}

export default Overview;
