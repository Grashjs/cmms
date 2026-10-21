import { alpha, Box, IconButton, Tooltip, useTheme } from '@mui/material';
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

function SidebarFooter({ collapsed = false }: { collapsed?: boolean }) {
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
  const theme = useTheme();
  const { logout, user } = useAuth();
  const { requestSubscriptionChange } = useContext(CompanySettingsContext);
  const navigate = useNavigate();

  const footerButtonSx = {
    width: 36,
    height: 36,
    color: alpha(theme.colors.alpha.trueWhite[100], 0.65),
    transition: theme.transitions.create(['background-color', 'color']),

    '&:hover': {
      background: alpha(theme.colors.alpha.trueWhite[100], 0.08),
      color: theme.colors.alpha.trueWhite[100]
    }
  };

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
        flexShrink: 0,
        display: 'flex',
        flexDirection: collapsed ? 'column' : 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 0.5,
        py: collapsed ? 1.5 : 1.25,
        px: 1.5,
        borderTop: `1px solid ${alpha(theme.colors.alpha.trueWhite[100], 0.08)}`
      }}
    >
      {user.ownsCompany && user.superAccountRelations.length === 0 && (
        <Tooltip placement={collapsed ? 'right' : 'top'} arrow title={t('upgrade_now')}>
          <IconButton
            sx={footerButtonSx}
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
        </Tooltip>
      )}
      <Tooltip placement={collapsed ? 'right' : 'top'} arrow title={t('documentation')}>
        <IconButton
          sx={footerButtonSx}
          onClick={() => window.open('https://grashjs.github.io/user-guide')}
        >
          <QuestionMarkTwoToneIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      {user.superAccountRelations.length === 0 && (
        <Tooltip placement={collapsed ? 'right' : 'top'} arrow title={t('wo_calendar')}>
          <IconButton
            sx={footerButtonSx}
            to="/app/work-orders?view=calendar"
            component={RouterLink}
          >
            <EventTwoToneIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
      <Tooltip placement={collapsed ? 'right' : 'top'} arrow title={t('Logout')}>
        <IconButton
          sx={footerButtonSx}
          onClick={handleLogout}
        >
          <PowerSettingsNewTwoToneIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  );
}

export default SidebarFooter;
