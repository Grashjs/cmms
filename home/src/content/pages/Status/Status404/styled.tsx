"use client";
import { Box, Button, OutlinedInput, styled } from "@mui/material";

export const MainContent = styled(Box)(
  () => `
    min-height: 100vh; 
    display: flex;
    flex: 1;
    overflow: auto;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background-color: #F2F5F9;
`,
);

export const OutlinedInputWrapper = styled(OutlinedInput)(
  ({ theme }) => `
    background-color: ${theme.colors.alpha.white[100]};
`,
);

export const ButtonSearch = styled(Button)(
  ({ theme }) => `
    margin-right: -${theme.spacing(1)};
`,
);
