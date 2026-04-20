import React, { useMemo, useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import { IFile } from '../../models/file';
import { IconButton, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import {
  AudioQuality,
  IOSOutputFormat,
  requestRecordingPermissionsAsync,
  setAudioModeAsync,
  useAudioPlayer,
  useAudioPlayerStatus,
  useAudioRecorder,
  type RecordingOptions
} from 'expo-audio';

export default function AudioRecorder({
  title,
  onChange
}: {
  title: string;
  onChange: (audio: IFile) => void;
}) {
  const [recordingURI, setRecordingURI] = useState<string | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [playbackStatus, setPlaybackStatus] = useState<
    'idle' | 'playing' | 'paused'
  >('idle');
  const theme = useTheme();
  const { t } = useTranslation();
  const recordingOptions = useMemo<RecordingOptions>(
    () => ({
      extension: '.m4a',
      sampleRate: 44100,
      numberOfChannels: 2,
      bitRate: 128000,
      android: {
        extension: '.m4a',
        outputFormat: 'mpeg4',
        audioEncoder: 'aac',
        sampleRate: 44100
      },
      ios: {
        extension: '.m4a',
        outputFormat: IOSOutputFormat.MPEG4AAC,
        audioQuality: AudioQuality.MEDIUM,
        sampleRate: 44100
      },
      web: {}
    }),
    []
  );
  const recorder = useAudioRecorder(recordingOptions);
  const player = useAudioPlayer(recordingURI ? { uri: recordingURI } : null);
  const playerStatus = useAudioPlayerStatus(player);

  useEffect(() => {
    if (recordingURI) {
      player.replace({ uri: recordingURI });
    }
  }, [player, recordingURI]);

  useEffect(() => {
    if (!playerStatus) return;

    if (playerStatus.didJustFinish) {
      player.pause();
      player
        .seekTo(0)
        .catch((error) => console.error('Failed to reset playback', error));
      setPlaybackStatus('idle');
      return;
    }

    if (playerStatus.playing) {
      setPlaybackStatus('playing');
    } else if (playerStatus.currentTime > 0) {
      setPlaybackStatus('paused');
    } else {
      setPlaybackStatus('idle');
    }
  }, [player, playerStatus]);

  const startRecording = async () => {
    try {
      const permissionResponse = await requestRecordingPermissionsAsync();

      if (permissionResponse.status !== 'granted') {
        Alert.alert(
          'Permission Required',
          'Permission to access microphone is required!'
        );
        return;
      }

      // Set audio mode
      await setAudioModeAsync({
        allowsRecording: true,
        playsInSilentMode: true,
        interruptionMode: 'mixWithOthers',
        shouldPlayInBackground: false,
        shouldRouteThroughEarpiece: false
      });
      setPlaybackStatus('idle');
      setRecordingURI(null);

      await recorder.prepareToRecordAsync(recordingOptions);
      recorder.record();
      setIsRecording(true);
    } catch (error) {
      console.error('Failed to start recording', error);
    }
  };

  const stopRecording = async () => {
    try {
      setIsRecording(false);
      await recorder.stop();
      const uri = recorder.uri;
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
    }
  };

  const playRecording = async () => {
    try {
      if (!recordingURI) return;

      player.play();
    } catch (error) {
      console.error('Failed to play recording', error);
    }
  };

  const pausePlayback = async () => {
    try {
      player.pause();
    } catch (error) {
      console.error('Failed to pause playback', error);
    }
  };

  const stopPlayback = async () => {
    try {
      player.pause();
      await player.seekTo(0);
    } catch (error) {
      console.error('Failed to stop playback', error);
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

      {recordingURI && (
        <View
          style={{
            display: 'flex',
            flexDirection: 'row',
            alignItems: 'center'
          }}
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
