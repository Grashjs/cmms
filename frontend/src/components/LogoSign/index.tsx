import {
  Box,
  styled,
  Tooltip,
  tooltipClasses,
  TooltipProps,
  useMediaQuery,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { customLogoPaths, homeUrl } from '../../config';
import { useEffect, useState } from 'react';
import { useBrand } from '../../hooks/useBrand';
import { getLocalizedHomeUrl } from '../../utils/urlPaths';

/**
 * The mark is shared across the product family, so what identifies this application is the
 * caption under it. Deliberately not the brand name from useBrand(): that is "AssetTrace", the
 * family, while this names the tool within it. Sibling applications carry their own caption and
 * differ otherwise only in the favicon.
 */
const APP_CAPTION = 'CMMS Tool';

const LogoWrapper = styled('a')(
  ({ theme }) => `
        color: ${theme.palette.text.primary};
        display: flex;
        text-decoration: none;
        flex-direction: column;
        align-items: center;
        margin: 0 auto;
        font-weight: ${theme.typography.fontWeightBold};
`
);

const LogoSignWrapper = styled(Box)(
  () => `
        display: flex;
        align-items: center;
        justify-content: center;
`
);

const LogoCaption = styled(Box)(
  ({ theme }) => `
        margin-top: ${theme.spacing(1)};
        color: ${theme.sidebar.menuItemColor};
        font-size: ${theme.typography.pxToRem(15)};
        font-weight: ${theme.typography.fontWeightBold};
        line-height: 1.2;
        text-align: center;
        white-space: nowrap;
`
);

const TooltipWrapper = styled(({ className, ...props }: TooltipProps) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
  [`& .${tooltipClasses.tooltip}`]: {
    backgroundColor: theme.colors.alpha.trueWhite[100],
    color: theme.palette.getContrastText(theme.colors.alpha.trueWhite[100]),
    fontSize: theme.typography.pxToRem(12),
    fontWeight: 'bold',
    borderRadius: theme.general.borderRadiusSm,
    boxShadow:
      '0 .2rem .8rem rgba(7,9,25,.18), 0 .08rem .15rem rgba(7,9,25,.15)'
  },
  [`& .${tooltipClasses.arrow}`]: {
    color: theme.colors.alpha.trueWhite[100]
  }
}));
interface OwnProps {
  white?: boolean;
}

function Logo({ white }: OwnProps) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const size = 64;
  const mobile = useMediaQuery(theme.breakpoints.down('sm'));
  const { logo, name: brandName } = useBrand();

  return (
    <TooltipWrapper title={brandName} arrow>
      <LogoWrapper href={getLocalizedHomeUrl('', i18n.language)}>
        <LogoSignWrapper>
          <img
            src={white ? logo.white : logo.dark}
            width={`${size * (mobile ? 0.7 : 1)}px`}
            height={`${size * (mobile ? 0.7 : 1)}px`}
            alt={brandName}
          />
        </LogoSignWrapper>
        <LogoCaption>{APP_CAPTION}</LogoCaption>
      </LogoWrapper>
    </TooltipWrapper>
  );
}

export default Logo;
