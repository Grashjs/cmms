import { View, StyleSheet } from 'react-native';
import { Text, Button, TextInput } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useNavigation } from '@react-navigation/native';
import { useState } from 'react';
import api from '../utils/api';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../types';

type FeedbackNavProp = NativeStackNavigationProp<
  RootStackParamList,
  'Feedback'
>;

export default function FeedbackScreen() {
  const { t } = useTranslation();
  const navigation = useNavigation<FeedbackNavProp>();
  const [feedback, setFeedback] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!feedback.trim()) return;
    setSubmitting(true);
    try {
      await api.post('reviews/feedback', { value: feedback.trim() });
    } catch {
      // silently fail
    }
    setSubmitting(false);
    navigation.goBack();
  };

  return (
    <View style={styles.container}>
      <Text variant="headlineSmall" style={styles.title}>
        {t('feedback_title')}
      </Text>
      <Text variant="bodyLarge" style={styles.description}>
        {t('feedback_description1')}
      </Text>
      <TextInput
        mode="outlined"
        multiline
        numberOfLines={4}
        value={feedback}
        onChangeText={setFeedback}
        style={styles.input}
        placeholder={t('feedback_placeholder')}
      />
      <Button
        mode="contained"
        onPress={handleSubmit}
        loading={submitting}
        disabled={!feedback.trim() || submitting}
        style={styles.button}
      >
        {t('send')}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24
  },
  title: {
    marginBottom: 16
  },
  description: {
    textAlign: 'center',
    marginBottom: 32
  },
  input: {
    width: '100%',
    marginBottom: 24
  },
  button: {
    minWidth: 120
  }
});
