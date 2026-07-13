import {
  Box,
  Button,
  Card,
  Container,
  Stack,
  styled,
  Typography
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import Logo from 'src/components/LogoSign';
import LanguageSwitcher from 'src/layouts/ExtendedSidebarLayout/Header/Buttons/LanguageSwitcher';

const HeaderWrapper = styled(Card)(
  ({ theme }) => `
    width: 100%;
    display: flex;
    align-items: center;
    height: ${theme.spacing(10)};
`
);

function DeletionPolicy() {
  const { t }: { t: any } = useTranslation();

  return (
    <Box>
      <Helmet>
        <title>{t('account_deletion')}</title>
      </Helmet>
      <HeaderWrapper>
        <Container maxWidth="lg">
          <Stack direction="row" alignItems="center">
            <Logo />
            <Stack
              direction="row"
              alignItems="center"
              justifyContent="space-between"
              flex={1}
            >
              <Box />
              <Stack direction="row" spacing={{ xs: 1, md: 2 }}>
                <Box
                  sx={{
                    display: {
                      xs: 'none',
                      sm: 'block'
                    }
                  }}
                >
                  <LanguageSwitcher />
                </Box>
                <Button
                  component={RouterLink}
                  to="/app/work-orders"
                  variant="outlined"
                  sx={{
                    ml: 2,
                    size: { xs: 'small', md: 'medium' }
                  }}
                >
                  {t('login')}
                </Button>
                <Button
                  component={RouterLink}
                  to="/account/register"
                  variant="contained"
                  sx={{
                    ml: 2,
                    size: { xs: 'small', md: 'medium' }
                  }}
                >
                  {t('register')}
                </Button>
              </Stack>
            </Stack>
          </Stack>
        </Container>
      </HeaderWrapper>
      <Box sx={{ mx: 10, padding: 2 }}>
        <Typography variant={'h1'}>{t('account_deletion')}</Typography>
      </Box>
      <Card sx={{ mx: 10, padding: 2 }}>
        <Typography>
          {
            'You are free to delete your Account at any time. To do that, you need to login go to >Settings>General>Delete account'
          }
        </Typography>
      </Card>
    </Box>
  );
}

export default DeletionPolicy;
