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

  useEffect(() => {
    if (!isCloudVersion) navigate('/account/login');
  }, [isCloudVersion]);

  return (
    <OverviewWrapper>
      <Helmet>
        <title>{IS_ORIGINAL_CLOUD ? t('main.title') : brandConfig.name}</title>
        <meta
          name="description"
          content="Atlas CMMS is a free, open-source CMMS to manage work orders, preventive maintenance, assets, and facilities. Streamline maintenance operations today."
        />
        <meta
          name="keywords"
          content="CMMS, computerized maintenance management system, EAM, enterprise asset management, open source CMMS, free maintenance software, work order management, preventive maintenance, asset tracking, facility management, maintenance tracking software, equipment maintenance, Atlas CMMS"
        />
        <link rel="canonical" href="https://atlas-cmms.com/" />
        <script type="application/ld+json">
          {`
            {
              "@context": "https://schema.org",
              "@type": "SoftwareApplication",
              "name": "Atlas CMMS",
              "description": "Atlas CMMS is a free, open-source CMMS to manage work orders, preventive maintenance, assets, and facilities. Streamline maintenance operations today.",
              "applicationCategory": "BusinessApplication",
              "operatingSystem": "Web",
              "url": "https://atlas-cmms.com/",
              "keywords": "CMMS, computerized maintenance management system, EAM, enterprise asset management, open source CMMS, free maintenance software, work order management, preventive maintenance, asset tracking, facility management, maintenance tracking software, equipment maintenance, Atlas CMMS",
              "publisher": {
                "@type": "Organization",
                "name": "Atlas CMMS"
              },
              "offers": {
                "@type": "Offer",
                "price": "0",
                "priceCurrency": "USD"
              }
            }
          `}
        </script>
      </Helmet>
      <NavBar />
      <Hero />
      <Highlights />
      <Footer />
    </OverviewWrapper>
  );
}

export default Overview;
