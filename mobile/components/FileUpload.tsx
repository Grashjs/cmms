import { View } from './Themed';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import * as React from 'react';
import { useContext, useState } from 'react';
import * as FileSystem from 'expo-file-system';
import {
  Alert,
  Image,
  ScrollView,
  Text,
  TouchableOpacity
} from 'react-native';
import { IconButton, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import mime from 'mime';
import { SheetManager } from 'react-native-actions-sheet';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import { IFile } from '../models/file';
import {
  DocumentPickerOptions,
  DocumentPickerResult
} from 'expo-document-picker';
import {
  openCameraWithPermission,
  openLibraryWithPermission
} from '../utils/mediaPermissions';

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
    console.warn('[ImageUpload] Tap -> camera');
    try {
      const result = await openCameraWithPermission('ImageUpload', {
        allowsEditing: true,
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsMultipleSelection: multiple,
        selectionLimit: 10,
        quality: 1
      });

      if (!result || result.canceled) {
        console.warn('[ImageUpload] Camera canceled or unavailable');
        return;
      }

      await onImagePicked(result);
    } catch (e) {
      console.error('Error taking photo:', e);
      Alert.alert('Error', 'Failed to take photo. Please try again.');
    }
  };
  const pickImage = async () => {
    console.warn('[ImageUpload] Tap -> library');
    try {
      const result = await openLibraryWithPermission('ImageUpload', {
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsMultipleSelection: multiple,
        selectionLimit: multiple ? 10 : 1,
        quality: 1
      });

      if (!result || result.canceled) {
        console.warn('[ImageUpload] Library Image picker canceled or unavailable');
        return;
      }
      await onImagePicked(result);
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
      onChangeInternal(
        [...images,
        ...result.assets.map((asset) => {
          const fileName =
            asset.uri.split('/')[asset.uri.split('/').length - 1];
          return {
            uri: asset.uri,
            name: fileName,
            type: mime.getType(fileName)
          };
        })],
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
        <View
          style={{
            flexDirection: 'row',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}
        >
          <Text style={{color:'black'}}>{title}</Text>
          <IconButton iconColor={theme.colors.primary} icon={'plus-circle'} />
        </View>
      </TouchableOpacity>
      <View>
        {type === 'image' &&
          !!images.length &&
          images.map((image) => (
            <View key={image.uri} style={{margin: 3}}>
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
      </View>
    </View>
  );
}
