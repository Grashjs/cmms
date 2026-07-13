import { View } from './Themed';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { DocumentResult } from 'expo-document-picker';
import * as React from 'react';
import * as FileSystem from 'expo-file-system';
import { useContext, useRef, useState } from 'react';
import * as Permissions from 'expo-permissions';
import { Alert, Image, PermissionsAndroid, ScrollView, Text, TouchableOpacity } from 'react-native';
import { Divider, IconButton, List, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import mime from 'mime';
import { IconSource } from 'react-native-paper/src/components/Icon';
import ActionSheet, { ActionSheetRef, SheetManager } from 'react-native-actions-sheet';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import { files, IFile } from '../models/file';


interface OwnProps {
  title: string;
  type: 'image' | 'file' | 'spreadsheet';
  multiple: boolean;
  description: string;
  onChange: (files: IFile[]) => void;
}

export default function FileUpload({
                                     title,
                                     type,
                                     multiple,
                                     onChange
                                   }: OwnProps) {
  const theme = useTheme();
  const actionSheetRef = useRef<ActionSheetRef>(null);
  const [images, setImages] = useState<IFile[]>([]);
  const [files, setFiles] = useState<IFile[]>([]);
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const maxFileSize: number = 7;
  const checkPermissions = async () => {
    try {
      const result = await PermissionsAndroid.check(
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE
      );

      if (!result) {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
          {
            title:
              'You need to give storage permission to download and save the file',
            message: 'App needs access to your camera ',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK'
          }
        );
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          console.log('You can use the camera');
          return true;
        } else {
          Alert.alert(t('error'), t('PERMISSION_ACCESS_FILE'));

          console.log('Camera permission denied');
          return false;
        }
      } else {
        return true;
      }
    } catch (err) {
      console.warn(err);
      return false;
    }
  };
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
    const { status } = await Permissions.askAsync(Permissions.CAMERA);
    if (status === 'granted') {
      try {
        const result = await ImagePicker.launchCameraAsync({
          allowsEditing: true,
          mediaTypes: ImagePicker.MediaTypeOptions.Images,
          allowsMultipleSelection: multiple,
          selectionLimit: 10,
          quality: 1
        });
        await onImagePicked(result);
      } catch (e) {
        console.error(e);
      }
    }
  };
  const pickImage = async () => {
    // No permissions request is necessary for launching the image library
    const { status } = await Permissions.askAsync(Permissions.MEDIA_LIBRARY);
    if (status === 'granted') {
      let result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsMultipleSelection: multiple,
        selectionLimit: 10,
        quality: 1
      });
      await onImagePicked(result);
    }
  };
  const checkSize = async (uri: string) => {
    const fileInfo = await getFileInfo(uri);

    if (!fileInfo?.size) {
      Alert.alert('Can\'t select this file as the size is unknown.');
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
      onChangeInternal(result.assets.map((asset) => {
        const fileName =
          asset.uri.split('/')[asset.uri.split('/').length - 1];
        return {
          uri: asset.uri,
          name: fileName,
          type: mime.getType(fileName)
        };
      }), 'image');
    }
  };
  const pickFile = async () => {
    const hasPermissions = await checkPermissions();
    if (hasPermissions) {
      let result = await DocumentPicker.getDocumentAsync({});
      if (result.type !== 'cancel') {
        await checkSize(result.uri);
        onChangeInternal([
          {
            uri: result.uri,
            name: result.name,
            type: mime.getType(result.name)
          }
        ], 'file');
      }
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
            <View>
              <Image source={{ uri: image.uri }} style={{ height: 200 }} />
              <IconButton
                style={{ position: 'absolute', top: 10, right: 10 }}
                onPress={() => {
                  onChangeInternal(images.filter((item) => item.uri !== image.uri), 'image');
                }}
                icon={'close-circle'}
                iconColor={theme.colors.error}
              />
            </View>
          ))}
        {type === 'file' && !!files.length && (
          <View style={{
            display: 'flex',
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <Text style={{ color: theme.colors.primary }}>{files[0].name}</Text>
            <IconButton
              onPress={() => {
                onChangeInternal([], 'file');
              }}
              icon={'close-circle'}
              iconColor={theme.colors.error}
            /></View>
        )}
      </ScrollView>
    </View>
  );
}
