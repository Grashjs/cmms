import { useRef, useState } from 'react';

import {
  Box,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Popover,
  styled,
  Tooltip,
  Typography
} from '@mui/material';
import internationalization, { supportedLanguages } from 'src/i18n/i18n';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';

const SectionHeading = styled(Typography)(
  ({ theme }) => `
        font-weight: ${theme.typography.fontWeightBold};
        color: ${theme.palette.secondary.main};
        display: block;
        padding: ${theme.spacing(2, 2, 0)};
`
);

const IconButtonWrapper = styled(IconButton)(
  ({ theme }) => `
        width: ${theme.spacing(6)};
        height: ${theme.spacing(6)};

        svg {
          width: 28px;
        }
`
);

const landingPaths = [
  '/',
  '/free-cmms',
  '/pricing',
  '/privacy',
  '/deletion-policy',
  '/terms-of-service',
  '/overview'
];

function getLocalizedPath(currentPath: string, lng: string): string {
  const pathParts = currentPath.split('/');
  const firstPart = pathParts[1];
  const isPrefixed = supportedLanguages.some((l) => l.code === firstPart);

  if (isPrefixed) {
    if (lng === 'en') {
      pathParts.splice(1, 1);
      return pathParts.join('/') || '/';
    } else {
      pathParts[1] = lng;
      return pathParts.join('/');
    }
  } else {
    const isLandingPath =
      landingPaths.includes(currentPath) ||
      currentPath.startsWith('/industries/') ||
      currentPath.startsWith('/features/');

    if (isLandingPath && lng !== 'en') {
      return `/${lng}${currentPath === '/' ? '' : currentPath}`;
    }
  }

  return currentPath;
}

function LanguageSwitcher({ onSwitch }: { onSwitch?: () => void }) {
  const { i18n } = useTranslation();
  const { t }: { t: any } = useTranslation();
  const getLanguage = i18n.language;
  const location = useLocation();

  const switchLanguage = (lng: string) => {
    internationalization.changeLanguage(lng);
  };

  const ref = useRef<any>(null);
  const [isOpen, setOpen] = useState<boolean>(false);

  const handleOpen = (): void => {
    setOpen(true);
  };

  const handleClose = (): void => {
    setOpen(false);
  };

  const currentSupportedLanguage = supportedLanguages.find(
    (supportedLanguage) =>
      supportedLanguage.code === getLanguage ||
      supportedLanguage.code.split('_')[0] === getLanguage
  );

  return (
    <>
      <Tooltip arrow title={t('Language Switcher')}>
        <IconButtonWrapper color="secondary" ref={ref} onClick={handleOpen}>
          {currentSupportedLanguage && (
            <currentSupportedLanguage.Icon
              title={currentSupportedLanguage.label}
            />
          )}
        </IconButtonWrapper>
      </Tooltip>
      <Popover
        disableScrollLock
        anchorEl={ref.current}
        onClose={handleClose}
        open={isOpen}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
      >
        <Box sx={{ maxWidth: 240 }}>
          <SectionHeading variant="body2" color="text.primary">
            {t('Language Switcher')}
          </SectionHeading>
          <List
            sx={{
              p: 2,
              svg: {
                width: 26,
                mr: 1
              }
            }}
            component="nav"
          >
            {supportedLanguages.map(({ code, label, Icon }) => {
              const href =
                getLocalizedPath(location.pathname, code) + location.search;

              return (
                <ListItem
                  key={code}
                  className={
                    getLanguage === code ||
                    (code === 'en' && getLanguage === 'en-US')
                      ? 'active'
                      : ''
                  }
                  button
                  component={Link}
                  to={href}
                  onClick={() => {
                    switchLanguage(code);
                    onSwitch?.();
                    handleClose();
                  }}
                >
                  <Icon title={label} />
                  <ListItemText sx={{ pl: 1 }} primary={label} />
                </ListItem>
              );
            })}
          </List>
        </Box>
      </Popover>
    </>
  );
}

export default LanguageSwitcher;
