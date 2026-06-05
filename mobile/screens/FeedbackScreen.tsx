import { View, StyleSheet } from 'react-native';
import { Text, Button } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../types';

type FeedbackNavProp = NativeStackNavigationProp<RootStackParamList, 'Feedback'>;

export default function FeedbackScreen() {
  const { t } = useTranslation();
  const navigation = useNavigation<FeedbackNavProp>();

  return (
    <View style={styles.container}>
      <Text variant="headlineSmall" style={styles.title}>
        {t('feedback_title')}
      </Text>
      <Text variant="bodyLarge" style={styles.description}>
        {t('feedback_description')}
      </Text>
      <Button
        mode="contained"
        onPress={() => navigation.goBack()}
        style={styles.button}
      >
        {t('close')}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  title: {
    marginBottom: 16,
  },
  description: {
    textAlign: 'center',
    marginBottom: 32,
  },
  button: {
    minWidth: 120,
  },
});
