import { Link as RouterLink } from 'react-router-dom';
import { Box, Card, Container, Link, styled, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import JWTLogin from '../LoginJWT';

import { useTranslation } from 'react-i18next';

const Content = styled(Box)(
  () => `
    display: flex;
    flex: 1;
    width: 100%;
`
);

function LoginCover() {
  const { t }: { t: any } = useTranslation();

  return (
    <>
      <Helmet>
        <title>{t('Login')}</title>
      </Helmet>
      <Content>
        <Container
          sx={{
            display: 'flex',
            alignItems: 'center',
            flexDirection: 'column'
          }}
          maxWidth="sm"
        >
          <Card
            sx={{
              p: 4,
              my: 4
            }}
          >
            <Box textAlign="center">
              <Typography
                variant="h2"
                sx={{
                  mb: 1
                }}
              >
                {t('login')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 3
                }}
              >
                {t('login_description')}
              </Typography>
            </Box>
            <JWTLogin />
            <Box my={4}>
              <Typography
                component="span"
                variant="subtitle2"
                color="text.primary"
                fontWeight="bold"
              >
                {t('no_account_yet')}
              </Typography>{' '}
              <Box display={{ xs: 'block', md: 'inline-block' }}>
                <Link component={RouterLink} to="/account/register">
                  <b>{t('signup_here')}</b>
                </Link>
              </Box>
            </Box>
          </Card>
        </Container>
      </Content>
    </>
  );
}

export default LoginCover;
