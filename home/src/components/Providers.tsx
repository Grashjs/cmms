"use client";

import React, {ReactNode} from 'react';
import {Provider} from 'react-redux';
import {HelmetProvider} from 'react-helmet-async';
import {SnackbarProvider} from 'notistack';

import store from 'src/store';
import {SidebarProvider} from 'src/contexts/SidebarContext';
import {TitleProvider} from 'src/contexts/TitleContext';
import {CustomSnackBarProvider} from 'src/contexts/CustomSnackBarContext';
import ThemeProvider from 'src/theme/ThemeProvider';
import {UtmTrackerProvider} from '@nik0di3m/utm-tracker-hook';

interface ProvidersProps {
  children: ReactNode;
}

export default function Providers({ children }: ProvidersProps) {
  return (
    <HelmetProvider>
      <Provider store={store}>
        <UtmTrackerProvider customParams={['msclkid']}>
          <SidebarProvider>
            <TitleProvider>
              <ThemeProvider>
                  <SnackbarProvider
                    maxSnack={6}
                    anchorOrigin={{
                      vertical: 'bottom',
                      horizontal: 'right'
                    }}
                  >
                    <CustomSnackBarProvider>
                      {children}
                    </CustomSnackBarProvider>
                  </SnackbarProvider>
              </ThemeProvider>
            </TitleProvider>
          </SidebarProvider>
        </UtmTrackerProvider>
      </Provider>
    </HelmetProvider>
  );
}

