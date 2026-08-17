import { createContext, FC, ReactNode, useState } from 'react';
import { Snackbar, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { showMessage } from 'react-native-flash-message';

type CustomSnackBarContext = {
  showSnackBar: (
    message: string,
    type?: 'error' | 'success' | 'info',
    action?: { label: string; onPress: () => void }
  ) => void;
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const CustomSnackBarContext = createContext<CustomSnackBarContext>(
  {} as CustomSnackBarContext
);

export const CustomSnackbarProvider: FC<{ children: ReactNode }> = ({
                                                                      children
                                                                    }) => {
  const theme = useTheme();
  const { t } = useTranslation();
  const showSnackBar = (
    text: string,
    type?: 'error' | 'success' | 'info',
    action?: { label: string; onPress: () => void }
  ) => {
    showMessage({
      message: t(type),
      description: text,
      color: getColor(type),
      textStyle: { color: theme.colors.onSurface },
      titleStyle: { color: theme.colors.onSurface },
      type: type === 'error' ? 'danger' : type,
      onPress: action?.onPress
    });
  };

  const getColor = (type: 'error' | 'success' | 'info') => {
    switch (type) {
      case 'info':
        return 'black';
      case 'error':
        return theme.colors.error;
      case 'success':
        // @ts-ignore
        return theme.colors.success;
    }
  };
  return (
    <CustomSnackBarContext.Provider value={{ showSnackBar }}>
      {children}
    </CustomSnackBarContext.Provider>
  );
};
