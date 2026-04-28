import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  TextField,
  Typography,
  useTheme
} from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import CloseIcon from '@mui/icons-material/Close';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import QrCodeIcon from '@mui/icons-material/QrCode';
import DownloadIcon from '@mui/icons-material/Download';
import PrintIcon from '@mui/icons-material/Print';
import { QRCodeSVG } from 'qrcode.react';
import { RequestPortal } from '../../../../../../models/owns/requestPortal';

interface SharePortalModalProps {
  open: boolean;
  onClose: () => void;
  portal: RequestPortal | null;
}

export default function SharePortalModal({
  open,
  onClose,
  portal
}: SharePortalModalProps) {
  const { t } = useTranslation();
  const theme = useTheme();
  const [copied, setCopied] = useState(false);

  const portalUrl = portal
    ? `${window.location.origin}/request-portal/${portal.uuid}`
    : '';

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(portalUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const handleDownloadQR = () => {
    const svg = document.getElementById(`qr-code-${portal?.uuid}`);
    if (svg) {
      const svgData = new XMLSerializer().serializeToString(svg);
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      const img = new Image();

      img.onload = () => {
        canvas.width = img.width;
        canvas.height = img.height;
        ctx?.drawImage(img, 0, 0);
        const pngFile = canvas.toDataURL('image/png');

        const downloadLink = document.createElement('a');
        downloadLink.download = `portal-qr-${portal?.uuid}.png`;
        downloadLink.href = pngFile;
        downloadLink.click();
      };

      img.src = 'data:image/svg+xml;base64,' + btoa(svgData);
    }
  };

  const handlePrintQR = () => {
    const svg = document.getElementById(`qr-code-${portal?.uuid}`);
    if (svg) {
      const svgData = new XMLSerializer().serializeToString(svg);
      const printWindow = window.open('', '_blank');
      printWindow?.document.write(`
        <html>
          <head>
            <title>QR Code - ${portal?.title}</title>
            <style>
              body { display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
              img { max-width: 100%; }
            </style>
          </head>
          <body>
            <img src="data:image/svg+xml;base64,${btoa(svgData)}" />
          </body>
        </html>
      `);
      printWindow?.document.close();
      printWindow?.focus();
      setTimeout(() => {
        printWindow?.print();
        printWindow?.close();
      }, 500);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: { borderRadius: 2 }
      }}
    >
      <DialogTitle
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          px: 3,
          pt: 3,
          pb: 2
        }}
      >
        <Typography variant="h5" fontWeight={700}>
          {t('share_portal')}
        </Typography>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ px: 3, pb: 3 }}>
        <Stack spacing={3}>
          {/* Portal link section */}
          <Box>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              {t('get_portal_link')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {t('portal_link_description')}
            </Typography>
            <Stack alignItems={'center'} spacing={1}>
              <TextField
                fullWidth
                value={portalUrl}
                InputProps={{ readOnly: true }}
                size="small"
              />
              <Button
                variant="outlined"
                onClick={handleCopyLink}
                startIcon={<ContentCopyIcon />}
                sx={{ minWidth: 100 }}
              >
                {copied ? t('copied') : t('copy')}
              </Button>
            </Stack>
          </Box>

          {/* QR Code section */}
          <Box>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              {t('print_qr_code')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {t('qr_code_description')}
            </Typography>
            <Box
              display="flex"
              flexDirection="column"
              alignItems="center"
              p={3}
              sx={{
                bgcolor: theme.palette.grey[50],
                borderRadius: 2
              }}
            >
              <Box
                sx={{
                  bgcolor: 'white',
                  p: 2,
                  borderRadius: 1,
                  boxShadow: 1
                }}
              >
                <QRCodeSVG
                  id={`qr-code-${portal?.uuid}`}
                  value={portalUrl}
                  size={200}
                  level="H"
                  includeMargin={true}
                />
              </Box>
            </Box>

            {/* QR Code actions */}
            <Box display="flex" gap={1} justifyContent="center" sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                onClick={handlePrintQR}
                startIcon={<PrintIcon />}
              >
                {t('print')}
              </Button>
              <Button
                variant="outlined"
                onClick={handleDownloadQR}
                startIcon={<DownloadIcon />}
              >
                {t('download')}
              </Button>
            </Box>
          </Box>
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
