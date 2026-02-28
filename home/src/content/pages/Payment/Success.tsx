"use client";
import { Box, Card, Container, styled, Typography } from '@mui/material';

import { useTranslations } from 'next-intl';
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

function PaymentSuccess() {
  const t = useTranslations();
  return (
    <>
      <MainContent>
        <Container maxWidth="sm">
          <Logo />
          <Card
            sx={{
              mt: 3,
              p: 4
            }}
          >
            <Box>
              <Typography
                variant="h2"
                sx={{
                  mb: 1
                }}
              >
                {t('Payment Successful!')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 3
                }}
              >
                {t('Your payment was processed successfully. Please check your email for your license details.')}
              </Typography>
            </Box>
          </Card>
        </Container>
      </MainContent>
    </>
  );
}

export default PaymentSuccess;

