import { View } from 'react-native';
import React, { useEffect } from 'react';
import { IconButton, useTheme } from 'react-native-paper';
import { useAudioPlayer, useAudioPlayerStatus } from 'expo-audio';

export function AudioPlayer({ url }: { url: string }) {
  const theme = useTheme();
  const player = useAudioPlayer(url ? { uri: url } : null);
  const status = useAudioPlayerStatus(player);
  const isPlaying = status?.playing ?? false;

  useEffect(() => {
    if (url) {
      player.replace({ uri: url });
    }
  }, [player, url]);

  const playRecording = async () => {
    try {
      if (!url) return;
      player.play();
    } catch (error) {
      console.error('Failed to play recording', error);
    }
  };
  const pausePlayback = async () => {
    player.pause();
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
