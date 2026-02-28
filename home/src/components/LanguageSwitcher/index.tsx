"use client";
import { useState } from "react";

import {
  Box,
  IconButton,
  List,
  ListItem,
  ListItemText,
  ListItemButton,
  Popover,
  styled,
  Tooltip,
  Typography,
} from "@mui/material";
import { supportedLanguages } from "src/i18n/i18n";
import { useTranslations, useLocale } from "next-intl";
import { useRouter, usePathname } from "src/i18n/routing";

const SectionHeading = styled(Typography)(
  ({ theme }) => `
        font-weight: ${theme.typography.fontWeightBold};
        color: ${theme.palette.secondary.main};
        display: block;
        padding: ${theme.spacing(2, 2, 0)};
`,
);

const IconButtonWrapper = styled(IconButton)(
  ({ theme }) => `
        width: ${theme.spacing(6)};
        height: ${theme.spacing(6)};

        svg {
          width: 28px;
        }
`,
);

function LanguageSwitcher({ onSwitch }: { onSwitch?: () => void }) {
  const t = useTranslations();
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();

  function switchLanguage(locale: string) {
    router.replace(pathname, { locale: locale.replace("_", "-") });
  }
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const isOpen = Boolean(anchorEl);

  const handleOpen = (event: React.MouseEvent<HTMLElement>): void => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = (): void => {
    setAnchorEl(null);
  };

  const currentSupportedLanguage = supportedLanguages.find(
    (supportedLanguage) => supportedLanguage.code.replace("_", "-") === locale,
  );

  return (
    <>
      <Tooltip arrow title={t("Language Switcher")}>
        <IconButtonWrapper color="secondary" onClick={handleOpen}>
          {currentSupportedLanguage && <currentSupportedLanguage.Icon title={currentSupportedLanguage.label} />}
        </IconButtonWrapper>
      </Tooltip>
      <Popover
        disableScrollLock
        anchorEl={anchorEl}
        onClose={handleClose}
        open={isOpen}
        anchorOrigin={{
          vertical: "top",
          horizontal: "right",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "right",
        }}
      >
        <Box
          sx={{
            maxWidth: 240,
          }}
        >
          <SectionHeading variant="body2" color="text.primary">
            {t("Language Switcher")}
          </SectionHeading>
          <List
            sx={{
              p: 2,
              svg: {
                width: 26,
                mr: 1,
              },
            }}
            component="nav"
          >
            {supportedLanguages.map(({ code, label, Icon }) => (
              <ListItem disablePadding key={code}>
                <ListItemButton
                  selected={locale === code.replace("_", "-")}
                  onClick={() => {
                    switchLanguage(code);
                    onSwitch?.();
                    handleClose();
                  }}
                >
                  <Icon title={label} />
                  <ListItemText sx={{ pl: 1 }} primary={label} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Box>
      </Popover>
    </>
  );
}

export default LanguageSwitcher;
