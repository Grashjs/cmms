import { Dialog, Button, Portal, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useReviewPrompt } from '../hooks/useReviewPrompt';

export function ReviewModal() {
  const { t } = useTranslation();
  const { visible, handleYes, handleNo } = useReviewPrompt();

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={handleNo}>
        <Dialog.Content>
          <Text variant="bodyLarge">{t('review_prompt_title')}</Text>
        </Dialog.Content>
        <Dialog.Actions>
          <Button onPress={handleNo}>{t('review_prompt_negative')}</Button>
          <Button onPress={handleYes}>{t('review_prompt_positive')}</Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
}
