"use client";

import { Box, styled, Typography } from "@mui/material";

export const TypographyH1 = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(50)};
`,
);

export const TypographyH2 = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(17)};
`,
);

export const ImgWrapper = styled(Box)(
  ({ theme }) => `
    position: relative;
    z-index: 5;
    width: 100%;
    overflow: hidden;
    border-radius: ${theme.general.borderRadiusLg};
    box-shadow: 0 0rem 14rem 0 rgb(255 255 255 / 20%), 0 0.8rem 2.3rem rgb(111 130 156 / 3%), 0 0.2rem 0.7rem rgb(17 29 57 / 15%);
    aspect-ratio: 1920 / 922;

    img {
      display: block;
      width: 100%;
    }
  `,
);

export const BoxAccent = styled(Box)(
  ({ theme }) => `
    border-radius: ${theme.general.borderRadiusLg};
    background: ${theme.palette.background.default};
    width: 100%;
    height: 100%;
    position: absolute;
    left: -40px;
    bottom: -40px;
    display: block;
    z-index: 4;
  `,
);

export const BoxContent = styled(Box)(
  () => `
  width: 150%;
  position: relative;
`,
);

export const MobileImgWrapper = styled(Box)(
  ({ theme }) => `
    position: absolute;
    z-index: 6;
    width: 15%;
    left: -14%;
    bottom: -25%;
         ${theme.breakpoints.down("md")} {
    left: 45%;
    bottom: -50%;
  }
    transform: translateY(-50%);
    overflow: hidden;
    border-radius: ${theme.general.borderRadiusLg};
    box-shadow: 0 0rem 14rem 0 rgb(0 0 0 / 20%), 0 0.8rem 2.3rem rgb(0 0 0 / 3%), 0 0.2rem 0.7rem rgb(0 0 0 / 15%);
    aspect-ratio: 720 / 1600;

    img {
      display: block;
      width: 100%;
    }
  `,
);
