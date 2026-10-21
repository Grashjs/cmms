import { Box, styled, useMediaQuery, useTheme, SxProps, Theme } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useBrand } from '../../hooks/useBrand';

export const WEBKORPS_ICON = '/static/images/logo/webkorps-icon.png';
export const WEBKORPS_ICON_WHITE = '/static/images/logo/webkorps-icon-white.png';

const LogoWrapper = styled(RouterLink)(({ theme }) => ({
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  textDecoration: 'none',
  borderRadius: theme.general.borderRadiusSm,
  transition: theme.transitions.create('opacity', {
    duration: theme.transitions.duration.shorter
  }),
  '&:hover': {
    opacity: 0.88
  },
  '&:focus-visible': {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: 3
  }
}));

interface OwnProps {
  white?: boolean;
  iconOnly?: boolean;
  height?: number | string;
  width?: number | string;
  sx?: SxProps<Theme>;
}

/**
 * Webkorps brand mark. `iconOnly` renders the circular "W" icon,
 * otherwise the full wordmark (or a licensed custom logo via useBrand).
 * Aspect ratio is always preserved.
 */
function Logo({ white, iconOnly, height, width, sx }: OwnProps) {
  const theme = useTheme();
  const mobile = useMediaQuery(theme.breakpoints.down('sm'));
  const { logo, name: brandName } = useBrand();

  const logoSrc = iconOnly
    ? white
      ? WEBKORPS_ICON_WHITE
      : WEBKORPS_ICON
    : white
    ? logo.white
    : logo.dark;

  const defaultHeight = iconOnly ? (mobile ? 32 : 36) : mobile ? 30 : 36;
  const defaultMaxWidth = iconOnly ? (mobile ? 32 : 36) : mobile ? 140 : 175;

  return (
    <LogoWrapper to="/" aria-label={brandName || 'Webkorps'} sx={sx}>
      <Box
        component="img"
        src={logoSrc}
        alt={brandName || 'Webkorps'}
        sx={{
          display: 'block',
          height: height || defaultHeight,
          maxWidth: width || defaultMaxWidth,
          width: iconOnly ? height || defaultHeight : 'auto',
          objectFit: 'contain'
        }}
      />
    </LogoWrapper>
  );
}

export default Logo;
