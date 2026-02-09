'use client'
import { Button, Container, Stack, SxProps, Theme, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Link from 'next/link';
import { demoLink } from '../../src/config';

export default function TwoCallToActions({
  hidePricing,
  sx
}: {
  hidePricing?: boolean;
  sx?: SxProps<Theme>;
}) {
  const { t } = useTranslation();
  return (
    <Container sx={sx} maxWidth="md">
      <Typography
        textAlign="center"
        sx={{
          mb: 2
        }}
        variant="h2"
      >
        {t('leading_maintenance')}
      </Typography>
      <Container
        sx={{
          mb: 6,
          justifyContent: 'center'
        }}
        maxWidth="sm"
      >
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent={'center'}
          spacing={2}
        >
          <Button
            component={Link}
            size="large"
            href="/account/register"
            variant="contained"
          >
            {hidePricing ? 'Sign Up for Free' : t('register')}
          </Button>
          {!hidePricing && (
            <Button size="large" href={demoLink} variant="outlined">
              {t('book_demo')}
            </Button>
          )}
        </Stack>
      </Container>
    </Container>
  );
}
