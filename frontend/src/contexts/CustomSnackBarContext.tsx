import { createContext, FC, useEffect } from 'react';
import { Zoom } from '@mui/material';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';
import { setConflictErrorHandler } from '../utils/api';

type CustomSnackBarContext = {
  showSnackBar: (
    message: string,
    type: 'error' | 'warning' | 'success'
  ) => void;
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const CustomSnackBarContext = createContext<CustomSnackBarContext>(
  {} as CustomSnackBarContext
);

export const CustomSnackBarProvider: FC = ({ children }) => {
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();
  const { t } = useTranslation();
  const showSnackBar = (
    message: string,
    type: 'error' | 'warning' | 'success'
  ) => {
    const key = enqueueSnackbar(message, {
      variant: type,
      anchorOrigin: {
        vertical: 'top',
        horizontal: 'right'
      },
      TransitionComponent: Zoom,
      // notistack's 5 s default kept the top-right action buttons covered;
      // errors and warnings stay a bit longer so they can be read.
      autoHideDuration: type === 'success' ? 2000 : 4000,
      // Let users dismiss it right away to reach the controls underneath.
      onClick: () => closeSnackbar(key)
    });
  };

  useEffect(() => {
    return setConflictErrorHandler(() => {
      showSnackBar(t('conflict_error'), 'error');
    });
  }, [t]);

  return (
    <CustomSnackBarContext.Provider value={{ showSnackBar }}>
      {children}
    </CustomSnackBarContext.Provider>
  );
};
