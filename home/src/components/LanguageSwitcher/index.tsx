"use client";
import { useState } from 'react';

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

function LanguageSwitcher({ onSwitch }: { onSwitch?: () => void }) {
  const { i18n } = useTranslation();
  const { t }: { t: any } = useTranslation();
  const getLanguage = i18n.language;

  const switchLanguage = ({ lng }: { lng: any }) => {
    internationalization.changeLanguage(lng);
  };

  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const isOpen = Boolean(anchorEl);

  const handleOpen = (event: React.MouseEvent<HTMLElement>): void => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = (): void => {
    setAnchorEl(null);
  };

  const currentSupportedLanguage = supportedLanguages.find(
      (supportedLanguage) => supportedLanguage.code === getLanguage
  );

  return (
      <>
        <Tooltip arrow title={t('Language Switcher')}>
          <IconButtonWrapper color="secondary" onClick={handleOpen}>
            {currentSupportedLanguage && (
                <currentSupportedLanguage.Icon
                    title={currentSupportedLanguage.label}
                />
            )}
          </IconButtonWrapper>
        </Tooltip>
        <Popover
            disableScrollLock
            anchorEl={anchorEl}
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
          <Box
              sx={{
                maxWidth: 240
              }}
          >
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
              {supportedLanguages.map(({ code, label, Icon }) => (
                  <ListItem
                      key={code}
                      className={
                        getLanguage === code ||
                        (code === 'en' && getLanguage === 'en-US')
                            ? 'active'
                            : ''
                      }
                      sx={{ cursor: 'pointer' }}
                      onClick={() => {
                        switchLanguage({ lng: code });
                        onSwitch?.();
                        handleClose();
                      }}
                  >
                    <Icon title={label} />
                    <ListItemText sx={{ pl: 1 }} primary={label} />
                  </ListItem>
              ))}
            </List>
          </Box>
        </Popover>
      </>
  );
}

export default LanguageSwitcher;