import {
  ActivityIndicator,
  Dialog,
  Portal,
  Provider,
  Text,
  useTheme
} from 'react-native-paper';
import * as React from 'react';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

export default function LoadingDialog({ visible }: { visible: boolean }) {
  const { t } = useTranslation();
  const theme = useTheme();
  return (
    <Provider theme={theme}>
      <Portal>
        <Dialog
          visible={visible}
          style={{ borderRadius: 5, backgroundColor: theme.colors.surface }}
        >
          <Dialog.Title>{t('loading')}</Dialog.Title>
          <Dialog.Content>
            <View
              style={{
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center'
              }}
            >
              <ActivityIndicator
                animating={true}
                color={theme.colors.primary}
              />
              <Text style={{ marginLeft: 20 }} variant="bodyMedium">
                {t('please_wait')}
              </Text>
            </View>
          </Dialog.Content>
        </Dialog>
      </Portal>
    </Provider>
  );
}
