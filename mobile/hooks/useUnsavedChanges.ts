import { useEffect } from 'react';
import { Alert } from 'react-native';
import { useTranslation } from 'react-i18next';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

export default function useUnsavedChanges(
  navigation: NativeStackNavigationProp<any>,
  isFormDirty: boolean
) {
  const { t } = useTranslation();

  useEffect(() => {
    return navigation.addListener('beforeRemove', (e) => {
      if (!isFormDirty) {
        return;
      }
      e.preventDefault();
      Alert.alert(t('discard_changes'), t('discard_changes_question'), [
        { text: t('cancel'), style: 'cancel' },
        {
          text: t('discard_changes'),
          style: 'destructive',
          onPress: () => navigation.dispatch(e.data.action)
        }
      ]);
    });
  }, [navigation, isFormDirty]);
}
