import {
  Box,
  styled,
  Tooltip,
  tooltipClasses,
  TooltipProps,
  useMediaQuery,
  useTheme
} from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const LogoWrapper = styled(Link)(
  ({ theme }) => `
        color: ${theme.palette.text.primary};
        display: flex;
        text-decoration: none;
        align-items: center;
        width: 53px;
        margin: 0 auto;
        font-weight: ${theme.typography.fontWeightBold};
`
);

const LogoSignWrapper = styled(Box)(
  () => `
        width: 52px;
        height: 52px;
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
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const width = 60;
  const height = 60;
  const mobile = useMediaQuery(theme.breakpoints.down('sm'));
  return (
    <TooltipWrapper title="Atlas" arrow>
      <LogoWrapper to="/overview">
        <LogoSignWrapper>
          <img
            src={`/static/images/logo/logo${white ? '-white' : ''}.png`}
            width={`${width * (mobile ? 0.7 : 1)}px`}
            height={`${height * (mobile ? 0.7 : 1)}px`}
          />
        </LogoSignWrapper>
      </LogoWrapper>
    </TooltipWrapper>
  );
}

export default Logo;
