import { View } from './Themed';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import * as React from 'react';
import { useContext, useRef, useState } from 'react';
import * as FileSystem from 'expo-file-system';
import {
  Alert,
  Image,
  Linking,
  PermissionsAndroid,
  Platform,
  ScrollView,
  Text,
  TouchableOpacity
} from 'react-native';
import { IconButton, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import mime from 'mime';
import { ActionSheetRef, SheetManager } from 'react-native-actions-sheet';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import { IFile } from '../models/file';
import {
  DocumentPickerOptions,
  DocumentPickerResult
} from 'expo-document-picker';

interface OwnProps {
  title: string;
  type: 'image' | 'file' | 'spreadsheet';
  multiple: boolean;
  description: string;
  onChange: (files: IFile[]) => void;
  files?: IFile[];
}

export default function FileUpload({
  title,
  type,
  multiple,
  onChange,
  files: defaultFiles
}: OwnProps) {
  const theme = useTheme();
  const actionSheetRef = useRef<ActionSheetRef>(null);
  const [images, setImages] = useState<IFile[]>(defaultFiles || []);
  const [files, setFiles] = useState<IFile[]>(defaultFiles || []);
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const maxFileSize: number = 7;

  const onChangeInternal = (files: IFile[], type: 'file' | 'image') => {
    if (type === 'file') {
      setFiles(files);
    } else {
      setImages(files);
    }
    onChange(files);
  };
  const getFileInfo = async (fileURI: string) => {
    const fileInfo = await FileSystem.getInfoAsync(fileURI);
    return fileInfo;
  };
  const isMoreThanTheMB = (fileSize: number, limit: number) => {
    return fileSize / 1024 / 1024 > limit;
  };
  const takePhoto = async () => {
    try {
      // Request camera permissions using modern API directly from ImagePicker
      const { status } = await ImagePicker.requestCameraPermissionsAsync();

      if (status === 'granted') {
        try {
          const result = await ImagePicker.launchCameraAsync({
            allowsEditing: true,
            mediaTypes: ImagePicker.MediaTypeOptions.Images,
            allowsMultipleSelection: multiple,
            selectionLimit: 10,
            quality: 1
          });

          // Handle both new and old API response formats
          // New versions use 'canceled', old versions use 'cancelled'
          if (result.canceled === true) {
            console.log('Camera was canceled');
            return;
          }

          await onImagePicked(result);
        } catch (e) {
          console.error('Error taking photo:', e);
          Alert.alert('Error', 'Failed to take photo. Please try again.');
        }
      } else {
        // Check if we can ask again
        const { canAskAgain } = await ImagePicker.getCameraPermissionsAsync();

        if (!canAskAgain) {
          // User selected "Don't ask again" or "Deny" on Android
          Alert.alert(
            'Permission Required',
            'Camera access is needed to take photos. Please enable it in app settings.',
            [
              { text: 'Cancel', style: 'cancel' },
              {
                text: 'Open Settings',
                onPress: () => Linking.openSettings()
              }
            ]
          );
        } else {
          // First-time denial
          Alert.alert(
            'Permission Denied',
            'Camera access is needed to take photos.',
            [{ text: 'OK' }]
          );
        }
      }
    } catch (error) {
      console.error('Error requesting camera permission:', error);
    }
  };
  const pickImage = async () => {
    try {
      const permissionResult =
        await ImagePicker.requestMediaLibraryPermissionsAsync();

      if (permissionResult.granted === false) {
        // Permission denied
        if (permissionResult.canAskAgain === false) {
          // User selected "Don't ask again" or "Deny" on Android
          Alert.alert(
            'Permission Required',
            'Media library access is needed to select images. Please enable it in app settings.',
            [
              { text: 'Cancel', style: 'cancel' },
              {
                text: 'Open Settings',
                onPress: () => Linking.openSettings()
              }
            ]
          );
        } else {
          // First-time denial
          Alert.alert(
            'Permission Denied',
            'Media library access is needed to select images.',
            [{ text: 'OK' }]
          );
        }
        return;
      }

      // Launch image picker
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsMultipleSelection: multiple,
        selectionLimit: multiple ? 10 : 1,
        quality: 1
      });

      // ImagePicker.launchImageLibraryAsync now returns { canceled: boolean } in newer versions
      // and { cancelled: boolean } in older versions, so handle both cases
      if (result.canceled === true) {
        console.log('Image picker was canceled');
        return;
      }

      // Process selected images
      await onImagePicked(result);

      return result;
    } catch (error) {
      console.error('Error picking image:', error);
      Alert.alert('Error', 'Failed to pick image. Please try again.');
    }
  };
  const checkSize = async (uri: string) => {
    const fileInfo = await getFileInfo(uri);
    if (!('size' in fileInfo)) return;
    if (!fileInfo?.size) {
      Alert.alert("Can't select this file as the size is unknown.");
      throw new Error();
    }
    if (isMoreThanTheMB(fileInfo.size, maxFileSize)) {
      showSnackBar(t('max_file_size_error', { size: maxFileSize }), 'error');
      throw new Error(t('max_file_size_error', { size: maxFileSize }));
    }
  };
  const onImagePicked = async (result: ImagePicker.ImagePickerResult) => {
    if (!result.canceled) {
      for (const asset of result.assets) {
        const { uri } = asset;
        await checkSize(uri);
      }
      const newImages = result.assets.map((asset) => {
        const fileName = asset.uri.split('/')[asset.uri.split('/').length - 1];
        return {
          uri: asset.uri,
          name: fileName,
          type: mime.getType(fileName)
        };
      });
      onChangeInternal(
        multiple ? [...images, ...newImages] : newImages,
        'image'
      );
    }
  };
  const pickFile = async () => {
    try {
      // Pass the 'multiple' prop to enable multi-file selection if needed
      const options: DocumentPickerOptions = {
        type:
          type === 'spreadsheet'
            ? [
                'application/vnd.ms-excel',
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                'text/csv'
              ]
            : '*/*', // Default to all file types
        copyToCacheDirectory: true,
        multiple
      };

      const result: DocumentPickerResult =
        await DocumentPicker.getDocumentAsync(options);
      if (
        result.canceled === true ||
        !result.assets ||
        result.assets.length === 0
      ) {
        console.log('Document picker was canceled or no file selected');
        return;
      }

      // Process selected files (currently only handles the first asset due to existing component logic)
      const selectedAssets = result.assets;
      const filesToUpload: IFile[] = [];

      // Loop through selected assets to perform size checks
      for (const asset of selectedAssets) {
        await checkSize(asset.uri);

        filesToUpload.push({
          uri: asset.uri,
          name: asset.name,
          type:
            mime.getType(asset.name) ||
            asset.mimeType ||
            'application/octet-stream' // Use mime or the asset's mimeType
        });

        // Break the loop if 'multiple' is false, as the current display logic only shows one file.
        if (!multiple) break;
      }

      // Pass the selected file(s) to the internal change handler
      onChangeInternal(
        multiple ? [...files, ...filesToUpload] : filesToUpload,
        'file'
      );
    } catch (error) {
      console.error('Error picking document:', error);
    }
  };
  const onPress = () => {
    if (type === 'image')
      SheetManager.show('upload-file-sheet', {
        payload: {
          onPickImage: pickImage,
          onTakePhoto: takePhoto
        }
      });
    else pickFile();
  };

  return (
    <View style={{ display: 'flex', flexDirection: 'column' }}>
      <TouchableOpacity onPress={onPress}>
        <Text>{title}</Text>
      </TouchableOpacity>
      <ScrollView>
        {type === 'image' &&
          !!images.length &&
          images.map((image) => (
            <View key={image.uri}>
              <Image source={{ uri: image.uri }} style={{ height: 200 }} />
              <IconButton
                style={{ position: 'absolute', top: 10, right: 10 }}
                onPress={() => {
                  onChangeInternal(
                    images.filter((item) => item.uri !== image.uri),
                    'image'
                  );
                }}
                icon={'close-circle'}
                iconColor={theme.colors.error}
              />
            </View>
          ))}
        {type !== 'image' && // Covers 'file' and 'spreadsheet' types
          !!files.length &&
          files.map((file, index) => (
            <View
              key={file.uri} // <--- Use a unique key
              style={{
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                justifyContent: 'space-between',
                paddingVertical: 1 // Optional: Add padding for separation
              }}
            >
              <Text style={{ color: theme.colors.primary, flexShrink: 1 }}>
                {file.name}
              </Text>
              <IconButton
                onPress={() => {
                  onChangeInternal(
                    files.filter((_, i) => i !== index),
                    'file'
                  );
                }}
                icon={'close-circle'}
                iconColor={theme.colors.error}
              />
            </View>
          ))}
      </ScrollView>
    </View>
  );
}
