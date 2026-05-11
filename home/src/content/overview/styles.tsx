"use client";

import { Box, styled } from "@mui/material";

export const OverviewWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: #ffffff;
    flex: 1;
    overflow-x: hidden;
`,
);
