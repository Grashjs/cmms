import { Button, Container, Stack, SxProps, Theme } from '@mui/material';
import { TypographyH1Primary } from 'src/content/overview/Highlights';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { demoLink } from '../../../config';

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
      <TypographyH1Primary
        textAlign="center"
        sx={{
          mb: 2
        }}
        variant="h2"
      >
        {t('leading_maintenance')}
      </TypographyH1Primary>
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
            to="/account/register"
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
