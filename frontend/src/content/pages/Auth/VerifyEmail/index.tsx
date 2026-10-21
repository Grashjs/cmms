import { Box, Card, Container, styled, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';

import { useTranslation } from 'react-i18next';
import Logo from 'src/components/LogoSign';

const MainContent = styled(Box)(
  () => `
    height: 100%;
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: center;
`
);

function VerifyEmail() {
  const { t }: { t: any } = useTranslation();
  return (
    <>
      <Helmet>
        <title>{t('verify_email_title')}</title>
      </Helmet>
      <MainContent
        sx={{
          minHeight: '100vh',
          py: 4,
          background: (theme) =>
            theme.palette.mode === 'dark'
              ? 'linear-gradient(135deg, #0b1120 0%, #0f172a 100%)'
              : 'linear-gradient(135deg, #f0f7fc 0%, #e8f2f9 50%, #f8fafc 100%)'
        }}
      >
        <Container
          maxWidth="sm"
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center'
          }}
        >
          <Logo height={42} sx={{ mb: 1 }} />
          <Card
            sx={{
              mt: 2.5,
              p: { xs: 3.5, sm: 5 },
              width: '100%',
              borderRadius: 3,
              boxShadow: '0 20px 45px -15px rgba(26, 135, 200, 0.12), 0 0 1px 1px rgba(0, 0, 0, 0.05)',
              border: '1px solid rgba(226, 232, 240, 0.8)'
            }}
          >
            <Box>
              <Typography
                variant="h2"
                sx={{
                  mb: 1
                }}
              >
                {t('verify_email_title')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 3
                }}
              >
                {t('verify_email_description')}
              </Typography>
            </Box>
          </Card>
        </Container>
      </MainContent>
    </>
  );
}

export default VerifyEmail;
