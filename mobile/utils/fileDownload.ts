import * as FileSystem from 'expo-file-system';
import { Linking, Platform, Alert, PermissionsAndroid } from 'react-native';

export const downloadFile = async (
  uri: string,
  fileName: string
): Promise<void> => {
  const directoryUri =
    FileSystem.documentDirectory ?? FileSystem.cacheDirectory;
  if (!directoryUri) {
    throw new Error('Missing download directory path');
  }
  const fileUri = `${directoryUri}${fileName}`;
  const res = await FileSystem.downloadAsync(uri, fileUri);

  if (res && res.status === 200) {
    try {
      await Linking.openURL(res.uri);
    } catch (error) {
      console.error(
        'Failed to open local file, falling back to remote URL',
        error
      );
      await Linking.openURL(uri);
    }
  } else {
    throw new Error('Unable to download work order report');
  }
};
