import { Button, Card, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import useAuth from '../../../hooks/useAuth';
import { Link as RouterLink } from 'react-router-dom';
import { isCloudVersion } from '../../../config';
import { getLocalizedHomeUrl } from '../../../utils/urlPaths';

export default function FeatureErrorMessage({ message }: { message: string }) {
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
  const { user } = useAuth();
  return (
    <Card
      sx={{
        height: 500,
        p: 4,
        m: 4,
        justifyContent: 'center',
        display: 'flex',
        alignItems: 'center'
      }}
    >
      <Stack spacing={4}>
        <Typography variant="h4">{t(message)}</Typography>
        {user.ownsCompany && (
          <Button
            component={isCloudVersion ? RouterLink : 'a'}
            {...(isCloudVersion
              ? { to: '/app/subscription/plans' }
              : {
                  href: getLocalizedHomeUrl(
                    'pricing?type=selfhosted',
                    i18n.language
                  ),
                  target: '_blank',
                  rel: 'noopener noreferrer'
                })}
            variant="contained"
            size="large"
          >
            {t('upgrade_now')}
          </Button>
        )}
      </Stack>
    </Card>
  );
}
