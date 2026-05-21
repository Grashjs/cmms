"use client";

import React, { ReactNode } from "react";
import { SnackbarProvider } from "notistack";
import { CustomSnackBarProvider } from "src/contexts/CustomSnackBarContext";
import ThemeProvider from "src/theme/ThemeProvider";

interface ProvidersProps {
  children: ReactNode;
}

export default function Providers({ children }: ProvidersProps) {
  return (
    <ThemeProvider>
      <SnackbarProvider
        maxSnack={6}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "right",
        }}
      >
        <CustomSnackBarProvider>{children}</CustomSnackBarProvider>
      </SnackbarProvider>
    </ThemeProvider>
  );
}
