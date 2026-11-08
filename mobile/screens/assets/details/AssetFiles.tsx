import { openAttachment } from '../../../utils/attachment';
import * as React from 'react';
import { useState } from 'react';
import { useDispatch } from '../../../store';
import { useTranslation } from 'react-i18next';
import { AssetDTO } from '../../../models/asset';
import { useNavigation } from '@react-navigation/native';
import { editAsset } from '../../../slices/asset';
import { ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import {
  Button,
  Dialog,
  Divider,
  IconButton,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import { View } from '../../../components/Themed';

export default function AssetFiles({ asset }: { asset: AssetDTO }) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const navigation = useNavigation();
  const theme = useTheme();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const [currentFileId, setCurrentFileId] = useState<number>();

  const handleDelete = (id: number) => {
    dispatch(
      editAsset(asset.id, {
        ...asset,
        files: asset.files.filter((file) => file.id !== id)
      })
    ).finally(() => setOpenDelete(false));
  };
  const renderConfirmDialog = () => {
    return (
      <Portal>
        <Dialog visible={openDelete} onDismiss={() => setOpenDelete(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">{t('confirm_delete_file_asset')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={() => handleDelete(currentFileId)}>
              {t('to_delete')}
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {renderConfirmDialog()}
      {asset.files.map((file) => (
        <TouchableOpacity
          key={file.id}
          onPress={async () => {
            await openAttachment(file.url);
          }}
        >
          <View
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              padding: 20,
              alignItems: 'center'
            }}
          >
            <Text style={{ fontWeight: 'bold' }}>{file.name}</Text>
            <IconButton
              icon={'delete-outline'}
              iconColor={theme.colors.error}
              onPress={() => {
                setCurrentFileId(file.id);
                setOpenDelete(true);
              }}
            />
          </View>
          <Divider />
        </TouchableOpacity>
      ))}
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
