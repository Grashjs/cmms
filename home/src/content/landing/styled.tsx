"use client";
import { Box, Grid, styled, Typography } from "@mui/material";

export const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`,
);

export const FeatureRow = styled(Grid)(
  ({ theme }) => `
    padding: ${theme.spacing(10, 0)};
    align-items: center;
`,
);

export const ImageWrapper = styled(Box)(
  ({ theme }) => `
    img {
      border-radius: ${theme.general.borderRadiusLg};
      box-shadow: 0 20px 50px rgba(0,0,0,0.1);
    }
`,
);

export const FeatureContent = styled(Box)(
  ({ theme }) => `
    padding: ${theme.spacing(0, 4)};
    @media (max-width: ${theme.breakpoints.values.md}px) {
      padding: ${theme.spacing(4, 0, 0)};
      text-align: center;
    }
`,
);

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

export const HeroImgWrapper = styled(Box)(
  ({ theme }) => `
    position: relative;
    z-index: 5;
    width: 100%;
    overflow: hidden;
    border-radius: ${theme.general.borderRadiusLg};
    box-shadow: 0 0rem 14rem 0 rgb(255 255 255 / 20%), 0 0.8rem 2.3rem rgb(111 130 156 / 3%), 0 0.2rem 0.7rem rgb(17 29 57 / 15%);

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

    img {
      display: block;
      width: 100%;
    }
  `,
);
