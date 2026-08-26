import {
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
import { homeUrl, isCloudVersion } from '../../../../config';
import { getLocalizedHomeUrl } from '../../../../utils/urlPaths';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';

const LightTooltip = styled(({ className, ...props }: TooltipProps) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
  [`& .${tooltipClasses.tooltip}`]: {
    backgroundColor: theme.colors.alpha.black[100],
    color: theme.palette.getContrastText(theme.colors.alpha.black[100]),
    boxShadow: theme.shadows[24],
    fontWeight: 'bold',
    fontSize: theme.typography.pxToRem(12)
  },
  [`& .${tooltipClasses.arrow}`]: {
    color: theme.colors.alpha.black[100]
  }
}));

function SidebarFooter() {
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
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
      {user.ownsCompany && user.superAccountRelations.length === 0 && (
        <LightTooltip placement="top" arrow title={t('upgrade_now')}>
          <IconButton
            sx={{
              background: `${theme.sidebar.menuItemBgActive}`,
              color: `${theme.sidebar.menuItemIconColor}`,
              transition: `${theme.transitions.create(['all'])}`,

              '&:hover': {
                background: `${theme.colors.alpha.black[10]}`,
                color: `${theme.sidebar.menuItemIconColorActive}`
              }
            }}
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
          >
            <UpgradeTwoToneIcon fontSize="small" />
          </IconButton>
        </LightTooltip>
      )}
      <LightTooltip placement="top" arrow title={t('documentation')}>
        <IconButton
          sx={{
            background: `${theme.sidebar.menuItemBgActive}`,
            color: `${theme.sidebar.menuItemIconColor}`,
            transition: `${theme.transitions.create(['all'])}`,

            '&:hover': {
              background: `${theme.colors.alpha.black[10]}`,
              color: `${theme.sidebar.menuItemIconColorActive}`
            }
          }}
          onClick={() => window.open('https://grashjs.github.io/user-guide')}
        >
          <QuestionMarkTwoToneIcon fontSize="small" />
        </IconButton>
      </LightTooltip>
      {user.superAccountRelations.length === 0 && (
        <LightTooltip placement="top" arrow title={t('wo_calendar')}>
          <IconButton
            sx={{
              background: `${theme.sidebar.menuItemBgActive}`,
              color: `${theme.sidebar.menuItemIconColor}`,
              transition: `${theme.transitions.create(['all'])}`,

              '&:hover': {
                background: `${theme.colors.alpha.black[10]}`,
                color: `${theme.sidebar.menuItemIconColorActive}`
              }
            }}
            to="/app/work-orders?view=calendar"
            component={RouterLink}
          >
            <EventTwoToneIcon fontSize="small" />
          </IconButton>
        </LightTooltip>
      )}
      <LightTooltip placement="top" arrow title={t('Logout')}>
        <IconButton
          sx={{
            background: `${theme.sidebar.menuItemBgActive}`,
            color: `${theme.sidebar.menuItemIconColor}`,
            transition: `${theme.transitions.create(['all'])}`,

            '&:hover': {
              background: `${theme.colors.alpha.black[10]}`,
              color: `${theme.sidebar.menuItemIconColorActive}`
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
