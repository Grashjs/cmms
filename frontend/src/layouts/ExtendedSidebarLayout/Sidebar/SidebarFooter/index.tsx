import {
  alpha,
  Box,
  IconButton,
  styled,
  Tooltip,
  tooltipClasses,
  TooltipProps,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import EventTwoToneIcon from '@mui/icons-material/EventTwoTone';
import PowerSettingsNewTwoToneIcon from '@mui/icons-material/PowerSettingsNewTwoTone';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import useAuth from 'src/hooks/useAuth';
import UpgradeTwoToneIcon from '@mui/icons-material/UpgradeTwoTone';
import QuestionMarkTwoToneIcon from '@mui/icons-material/QuestionMarkTwoTone';
import { isCloudVersion } from '../../../../config';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';

const LightTooltip = styled(({ className, ...props }: TooltipProps) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
  [`& .${tooltipClasses.tooltip}`]: {
    backgroundColor: theme.colors.alpha.trueWhite[100],
    color: theme.palette.getContrastText(theme.colors.alpha.trueWhite[100]),
    boxShadow: theme.shadows[24],
    fontWeight: 'bold',
    fontSize: theme.typography.pxToRem(12)
  },
  [`& .${tooltipClasses.arrow}`]: {
    color: theme.colors.alpha.trueWhite[100]
  }
}));

function SidebarFooter() {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { logout, user } = useAuth();
  const { requestSubscriptionChange } = useContext(CompanySettingsContext);
  const navigate = useNavigate();

  const handleLogout = async (): Promise<void> => {
    try {
      await logout();
      navigate('/');
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <Box
      sx={{
        height: 60
      }}
      display="flex"
      alignItems="center"
      justifyContent="center"
    >
      {isCloudVersion && user.ownsCompany && (
        <LightTooltip placement="top" arrow title={t('upgrade_now')}>
          <IconButton
            sx={{
              background: `${theme.colors.alpha.trueWhite[10]}`,
              color: `${theme.colors.alpha.trueWhite[70]}`,
              transition: `${theme.transitions.create(['all'])}`,

              '&:hover': {
                background: `${alpha(theme.colors.alpha.trueWhite[100], 0.2)}`,
                color: `${theme.colors.alpha.trueWhite[100]}`
              }
            }}
            // onClick={requestSubscriptionChange}
            to="/app/subscription/plans"
            component={RouterLink}
          >
            <UpgradeTwoToneIcon fontSize="small" />
          </IconButton>
        </LightTooltip>
      )}
      <LightTooltip placement="top" arrow title={t('documentation')}>
        <IconButton
          sx={{
            background: `${theme.colors.alpha.trueWhite[10]}`,
            color: `${theme.colors.alpha.trueWhite[70]}`,
            transition: `${theme.transitions.create(['all'])}`,

            '&:hover': {
              background: `${alpha(theme.colors.alpha.trueWhite[100], 0.2)}`,
              color: `${theme.colors.alpha.trueWhite[100]}`
            }
          }}
          onClick={() => window.open('https://grashjs.github.io/user-guide')}
        >
          <QuestionMarkTwoToneIcon fontSize="small" />
        </IconButton>
      </LightTooltip>
      <LightTooltip placement="top" arrow title={t('wo_calendar')}>
        <IconButton
          sx={{
            background: `${theme.colors.alpha.trueWhite[10]}`,
            color: `${theme.colors.alpha.trueWhite[70]}`,
            transition: `${theme.transitions.create(['all'])}`,

            '&:hover': {
              background: `${alpha(theme.colors.alpha.trueWhite[100], 0.2)}`,
              color: `${theme.colors.alpha.trueWhite[100]}`
            }
          }}
          to="/app/work-orders?view=calendar"
          component={RouterLink}
        >
          <EventTwoToneIcon fontSize="small" />
        </IconButton>
      </LightTooltip>
      <LightTooltip placement="top" arrow title={t('Logout')}>
        <IconButton
          sx={{
            background: `${theme.colors.alpha.trueWhite[10]}`,
            color: `${theme.colors.alpha.trueWhite[70]}`,
            transition: `${theme.transitions.create(['all'])}`,

            '&:hover': {
              background: `${alpha(theme.colors.alpha.trueWhite[100], 0.2)}`,
              color: `${theme.colors.alpha.trueWhite[100]}`
            }
          }}
          onClick={handleLogout}
        >
          <PowerSettingsNewTwoToneIcon fontSize="small" />
        </IconButton>
      </LightTooltip>
    </Box>
  );
}

export default SidebarFooter;
