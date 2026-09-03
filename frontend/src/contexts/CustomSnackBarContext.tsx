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
  const { enqueueSnackbar } = useSnackbar();
  const { t } = useTranslation();
  const showSnackBar = (
    message: string,
    type: 'error' | 'warning' | 'success'
  ) => {
    enqueueSnackbar(message, {
      variant: type,
      anchorOrigin: {
        vertical: 'top',
        horizontal: 'right'
      },
      TransitionComponent: Zoom
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
