import { Box, CircularProgress, Typography, useTheme } from '@mui/material';
import Logo from 'src/components/LogoSign';

function AppInit() {
  const theme = useTheme();

  return (
    <Box
      sx={{
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
        right: 0,
        width: '100%',
        height: '100%',
        backgroundColor: theme.palette.background.default,
        zIndex: 99999,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center'
      }}
    >
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 2.5
        }}
      >
        <Logo height={44} />
        <CircularProgress
          size={28}
          thickness={4}
          sx={{
            color: theme.palette.primary.main,
            mt: 1
          }}
        />
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            fontWeight: 500,
            letterSpacing: '0.04em',
            fontSize: '0.8125rem'
          }}
        >
          Loading Webkorps CMMS...
        </Typography>
      </Box>
    </Box>
  );
}

export default AppInit;
