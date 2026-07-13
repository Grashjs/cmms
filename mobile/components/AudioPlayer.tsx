import { View } from 'react-native';
import { Audio } from 'expo-av';
import React, { useState } from 'react';
import { IconButton, useTheme } from 'react-native-paper';

export function AudioPlayer({ url }: { url: string }) {
  const [sound, setSound] = useState<Audio.Sound>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const theme = useTheme();
  const playRecording = async () => {
    try {
      const { sound } = await Audio.Sound.createAsync({ uri: url });
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
    <View>
      {url && !isPlaying && (
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
