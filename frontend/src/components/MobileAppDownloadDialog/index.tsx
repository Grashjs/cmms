import {
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
  Box,
  Button,
  Divider
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useEffect, useState } from 'react';

interface MobileAppDownloadDialogProps {
  open: boolean;
  onClose: () => void;
}

const PLAY_STORE_URL =
  'https://play.google.com/store/apps/details?id=com.atlas.cmms';
const APP_STORE_URL = 'https://apps.apple.com/us/app/atlas-cmms/id6751547284';

export default function MobileAppDownloadDialog({
  open,
  onClose
}: MobileAppDownloadDialogProps) {
  const { t }: { t: any } = useTranslation();
  const [isIOS, setIsIOS] = useState(false);

  useEffect(() => {
    const userAgent = navigator.userAgent || navigator.vendor;
    const isIosDevice =
      /iPad|iPhone|iPod/.test(userAgent) ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

    setIsIOS(isIosDevice);
  }, []);

  const handleDownloadClick = () => {
    const url = isIOS ? APP_STORE_URL : PLAY_STORE_URL;
    window.open(url, '_blank');
    onClose();
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('Download Mobile App')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="body1">
            {t(
              'Enhance your experience with our mobile app. Get instant notifications and manage your work orders on the go.'
            )}
          </Typography>

          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              gap: 2,
              mt: 2
            }}
          >
            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={handleDownloadClick}
              sx={{
                py: 1.5,
                fontSize: '1rem'
              }}
            >
              {isIOS
                ? t('Download on the App Store')
                : t('Get it on Google Play')}
            </Button>

            <Divider sx={{ my: 1 }} />

            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
              <Button
                variant="outlined"
                size="medium"
                onClick={() => {
                  window.open(APP_STORE_URL, '_blank');
                  onClose();
                }}
                sx={{
                  flex: 1
                }}
              >
                <Box
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center'
                  }}
                >
                  <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                    {t('Download on the')}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {t('App Store')}
                  </Typography>
                </Box>
              </Button>

              <Button
                variant="outlined"
                size="medium"
                onClick={() => {
                  window.open(PLAY_STORE_URL, '_blank');
                  onClose();
                }}
                sx={{
                  flex: 1
                }}
              >
                <Box
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center'
                  }}
                >
                  <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                    {t('GET IT ON')}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {t('Google Play')}
                  </Typography>
                </Box>
              </Button>
            </Box>
          </Box>
        </Box>
      </DialogContent>
    </Dialog>
  );
}
