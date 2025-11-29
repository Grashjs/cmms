import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import { Audio } from 'expo-av';
import { IFile } from '../../models/file';
import { IconButton, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';

export default function AudioRecorder({
  title,
  onChange
}: {
  title: string;
  onChange: (audio: IFile) => void;
}) {
  const [recording, setRecording] = useState<Audio.Recording | null>(null);
  const [recordingURI, setRecordingURI] = useState<string | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [sound, setSound] = useState<Audio.Sound | null>(null);
  const [playbackStatus, setPlaybackStatus] = useState<'idle' | 'playing' | 'paused'>(
    'idle'
  );
  const theme = useTheme();
  const { t } = useTranslation();
  const startRecording = async () => {
    try {
      const permissionResponse = await Audio.requestPermissionsAsync();

      if (permissionResponse.status !== 'granted') {
        Alert.alert(
          'Permission Required',
          'Permission to access microphone is required!'
        );
        return;
      }

      // Set audio mode
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: true,
        playsInSilentModeIOS: true
      });
      if (sound) {
        await sound.unloadAsync();
        setSound(null);
      }
      setPlaybackStatus('idle');
      setRecordingURI(null);

      const recording = new Audio.Recording();
      await recording.prepareToRecordAsync({
        web: undefined,
        android: {
          extension: '.m4a',
          outputFormat: Audio.AndroidOutputFormat.MPEG_4,
          audioEncoder: Audio.AndroidAudioEncoder.AAC,
          sampleRate: 44100,
          numberOfChannels: 2,
          bitRate: 128000
        },
        ios: {
          extension: '.m4a',
          outputFormat: Audio.IOSOutputFormat.MPEG4AAC,
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
    if (!recording) return;

    try {
      setIsRecording(false);
      await recording.stopAndUnloadAsync();
      const uri = recording.getURI();
      if (uri) {
        setRecordingURI(uri);
        const sanitizedTitle = title
          ? title.replace(/[^a-z0-9]/gi, '_').toLowerCase()
          : 'audio_recording';
        onChange({
          uri,
          name: `${sanitizedTitle}.m4a`,
          type: 'audio/m4a'
        });
      }
    } catch (error) {
      console.error('Failed to stop recording', error);
    } finally {
      setRecording(null);
    }
  };

  const playRecording = async () => {
    try {
      if (!recordingURI) return;

      let playbackSound = sound;
      if (!playbackSound) {
        const createdSound = await Audio.Sound.createAsync({ uri: recordingURI });
        playbackSound = createdSound.sound;
        playbackSound.setOnPlaybackStatusUpdate(async (status) => {
          if (!status.isLoaded) return;
          if (status.didJustFinish) {
            setPlaybackStatus('idle');
            try {
              await playbackSound.setPositionAsync(0);
            } catch (err) {
              console.error('Failed to reset audio position', err);
            }
          } else if (!status.isPlaying && status.positionMillis > 0) {
            setPlaybackStatus('paused');
          }
        });
        setSound(playbackSound);
      }

      if (!playbackSound) return;

      await playbackSound.playAsync();
      setPlaybackStatus('playing');
    } catch (error) {
      console.error('Failed to play recording', error);
    }
  };

  const pausePlayback = async () => {
    if (!sound) return;

    try {
      await sound.pauseAsync();
      setPlaybackStatus('paused');
    } catch (error) {
      console.error('Failed to pause playback', error);
    }
  };

  const stopPlayback = async () => {
    if (!sound) return;

    try {
      await sound.stopAsync();
      await sound.setPositionAsync(0);
      setPlaybackStatus('idle');
    } catch (error) {
      console.error('Failed to stop playback', error);
    }
  };

  useEffect(() => {
    return () => {
      if (sound) {
        sound.unloadAsync().catch((error) =>
          console.error('Failed to unload sound', error)
        );
      }
    };
  }, [sound]);

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

      {recordingURI && (
        <View
          style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}
        >
          {playbackStatus !== 'playing' && (
            <IconButton
              icon="play"
              iconColor={theme.colors.primary}
              onPress={playRecording}
            />
          )}
          {playbackStatus === 'playing' && (
            <IconButton icon="pause" onPress={pausePlayback} />
          )}
          {(playbackStatus === 'playing' || playbackStatus === 'paused') && (
            <IconButton
              icon="stop"
              iconColor={theme.colors.error}
              onPress={stopPlayback}
            />
          )}
        </View>
      )}
    </View>
  );
}
