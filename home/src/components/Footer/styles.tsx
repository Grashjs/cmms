"use client";
import { Box, styled, Typography } from "@mui/material";
import { Link } from "src/i18n/routing";

export const FooterWrapper = styled(Box)(
  ({ theme }) => `
    background: ${theme.colors.alpha.black[100]};
    color: ${theme.colors.alpha.white[70]};
    padding: ${theme.spacing(4)} 0;
`,
);

export const FooterLink = styled(Link)(
  ({ theme }) => `
    color: ${theme.colors.alpha.white[70]};
    text-decoration: none;

    &:hover {
      color: ${theme.colors.alpha.white[100]};
      text-decoration: underline;
    }
`,
);

export const FooterAnchor = styled("a")(
  ({ theme }) => `
    color: ${theme.colors.alpha.white[70]};
    text-decoration: none;

    &:hover {
      color: ${theme.colors.alpha.white[100]};
      text-decoration: underline;
    }
`,
);

export const SectionHeading = styled(Typography)(
  ({ theme }) => `
    font-weight: ${theme.typography.fontWeightBold};
    color: ${theme.colors.alpha.white[100]};
    margin-bottom: ${theme.spacing(2)};
`,
);
