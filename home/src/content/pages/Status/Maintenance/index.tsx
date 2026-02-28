"use client";
import {
  Box,
  Container,
  Divider,
  IconButton,
  styled,
  Tooltip,
  Typography
} from '@mui/material';
import Logo from 'src/components/LogoSign';

import { useTranslations } from 'next-intl';
import FacebookIcon from '@mui/icons-material/Facebook';
import TwitterIcon from '@mui/icons-material/Twitter';
import InstagramIcon from '@mui/icons-material/Instagram';

const MainContent = styled(Box)(
  () => `
    height: 100%;
    display: flex;
    flex: 1;
    overflow: auto;
    flex-direction: column;
    align-items: center;
    justify-content: center;
`
);

function StatusMaintenance() {
  const t = useTranslations();

  return (
    <>
      <MainContent>
        <Container maxWidth="md">
          <Logo />
          <Box textAlign="center">
            <Container maxWidth="xs">
              <Typography
                variant="h2"
                sx={{
                  mt: 4,
                  mb: 2
                }}
              >
                {t('The site is currently down for maintenance')}
              </Typography>
              <Typography
                variant="h3"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 4
                }}
              >
                {t('We apologize for any inconveniences caused')}
              </Typography>
            </Container>
            <img
              alt="Maintenance"
              height={250}
              src="/static/images/status/maintenance.svg"
            />
          </Box>
          <Divider
            sx={{
              my: 4
            }}
          />
          <Box
            display="flex"
            alignItems="center"
            justifyContent="space-between"
          >
            <Box>
              <Typography component="span" variant="subtitle1">
                {t('phone')}:{' '}
              </Typography>
              <Typography
                component="span"
                variant="subtitle1"
                color="text.primary"
              >
                + 00 1 888 555 444
              </Typography>
            </Box>
            <Box>
              <Tooltip arrow placement="top" title="Facebook">
                <IconButton color="primary">
                  <FacebookIcon />
                </IconButton>
              </Tooltip>
              <Tooltip arrow placement="top" title="Twitter">
                <IconButton color="primary">
                  <TwitterIcon />
                </IconButton>
              </Tooltip>
              <Tooltip arrow placement="top" title="Instagram">
                <IconButton color="primary">
                  <InstagramIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
        </Container>
      </MainContent>
    </>
  );
}

export default StatusMaintenance;

