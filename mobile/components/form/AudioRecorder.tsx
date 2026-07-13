import React, { useState, useEffect } from 'react';
import { Button, View, Text, TouchableOpacity } from 'react-native';
import { Audio } from 'expo-av';
import * as Permissions from 'expo-permissions';
import { IFile } from '../../models/file';
import { IconButton, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';

export default function AudioRecordingExample({
  title,
  onChange
}: {
  title: string;
  onChange: (audio: IFile) => void;
}) {
  const [recording, setRecording] = useState(null);
  const [recordingURI, setRecordingURI] = useState(null);
  const [isRecording, setIsRecording] = useState(false);
  const [sound, setSound] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const theme = useTheme();
  const { t } = useTranslation();
  const startRecording = async () => {
    try {
      const response = await Permissions.askAsync(Permissions.AUDIO_RECORDING);
      if (response.status !== 'granted') {
        alert('Permission to access microphone is required!');
      }
      await Audio.requestPermissionsAsync();
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: true,
        playsInSilentModeIOS: true
      });
      const recording = new Audio.Recording();
      await recording.prepareToRecordAsync({
        web: undefined,
        android: {
          extension: '.m4a',
          outputFormat: Audio.AndroidOutputFormat.AMR_NB,
          audioEncoder: Audio.AndroidAudioEncoder.AMR_NB,
          sampleRate: 44100,
          numberOfChannels: 2,
          bitRate: 128000
        },
        ios: {
          extension: '.caf',
          audioQuality: Audio.IOSAudioQuality.MEDIUM,
          sampleRate: 44100,
          numberOfChannels: 2,
          bitRate: 128000
        }
      });
      await recording.startAsync();
      setRecording(recording);
      setIsRecording(true);
    } catch (error) {
      console.error('Failed to start recording', error);
    }
  };

  const stopRecording = async () => {
    setIsRecording(false);
    await recording.stopAndUnloadAsync();
    const uri = recording.getURI();
    setRecordingURI(uri);
    onChange({ uri, name: title, type: 'audio/mp4' });
    setRecording(null);
  };

  const playRecording = async () => {
    try {
      const { sound } = await Audio.Sound.createAsync({ uri: recordingURI });
      setSound(sound);
      await sound.playAsync();
      setIsPlaying(true);
      sound.setOnPlaybackStatusUpdate((status) => {
        if ('isPlaying' in status && !status.isPlaying) {
          setIsPlaying(false);
        }
      });
    } catch (error) {
      console.error('Failed to play recording', error);
    }
  };

  const pausePlayback = async () => {
    if (sound) {
      await sound.pauseAsync();
      setIsPlaying(false);
    }
  };

  const stopPlayback = async () => {
    if (sound) {
      await sound.stopAsync();
      setIsPlaying(false);
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>{title}</Text>
      <TouchableOpacity
        onPress={isRecording ? stopRecording : startRecording}
        style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}
      >
        <IconButton
          iconColor={isRecording ? theme.colors.error : theme.colors.primary}
          style={{ height: 40, width: 40 }}
          icon={'microphone'}
        />
        <Text>{isRecording ? t('stop_recording') : t('start_recording')}</Text>
      </TouchableOpacity>

      {recordingURI && !isPlaying && (
        <IconButton
          icon="play"
          iconColor={theme.colors.primary}
          onPress={playRecording}
        />
      )}
      {isPlaying && (
        <View style={{ display: 'flex', flexDirection: 'row' }}>
          <IconButton icon="pause" onPress={pausePlayback} />
          <IconButton
            icon="stop"
            iconColor={theme.colors.error}
            onPress={stopPlayback}
          />
        </View>
      )}
    </View>
  );
}
