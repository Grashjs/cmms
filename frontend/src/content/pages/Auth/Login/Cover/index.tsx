import { Link as RouterLink } from 'react-router-dom';
import { Box, Card, Container, Link, styled, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import JWTLogin from '../LoginJWT';

import { useTranslation } from 'react-i18next';
import Logo from 'src/components/LogoSign';
import { ldapEnabled } from '../../../../../config';

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
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>
      <Content
        sx={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          py: 4,
          background: (theme) =>
            theme.palette.mode === 'dark'
              ? 'linear-gradient(135deg, #0b1120 0%, #0f172a 100%)'
              : 'linear-gradient(135deg, #f0f7fc 0%, #e8f2f9 50%, #f8fafc 100%)'
        }}
      >
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
              p: { xs: 3.5, sm: 5 },
              width: '100%',
              borderRadius: 3,
              boxShadow: '0 20px 45px -15px rgba(26, 135, 200, 0.15), 0 0 1px 1px rgba(0, 0, 0, 0.05)',
              border: '1px solid rgba(226, 232, 240, 0.8)'
            }}
          >
            <Box textAlign="center" mb={2}>
              <Logo height={42} sx={{ mb: 3 }} />
              <Typography
                variant="h2"
                sx={{
                  mb: 1,
                  fontWeight: 700,
                  letterSpacing: '-0.02em',
                  fontSize: { xs: '1.5rem', sm: '1.75rem' }
                }}
              >
                {t('login')}
              </Typography>
              <Typography
                variant="body1"
                color="text.secondary"
                sx={{
                  mb: 3,
                  fontSize: '0.9375rem',
                  lineHeight: 1.5
                }}
              >
                {t('login_description')}
              </Typography>
            </Box>
            <JWTLogin />
            {!ldapEnabled && (
              <Box mt={3.5} pt={2.5} textAlign="center" borderTop="1px solid rgba(226, 232, 240, 0.6)">
                <Typography
                  component="span"
                  variant="body2"
                  color="text.secondary"
                >
                  {t('no_account_yet')}
                </Typography>{' '}
                <Link
                  component={RouterLink}
                  to="/account/register"
                  sx={{
                    fontWeight: 600,
                    color: 'primary.main',
                    textDecoration: 'none',
                    '&:hover': { textDecoration: 'underline' }
                  }}
                >
                  {t('signup_here')}
                </Link>
              </Box>
            )}
          </Card>
        </Container>
      </Content>
    </>
  );
}

export default LoginCover;
