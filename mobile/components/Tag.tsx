import { View } from 'react-native';
import { Text } from 'react-native-paper';
import * as React from 'react';

export default function Tag({
  text,
  backgroundColor,
  color
}: {
  text: string;
  color: string;
  backgroundColor: string;
}) {
  return (
    <View
      style={{
        backgroundColor,
        paddingHorizontal: 8,
        paddingVertical: 5,
        borderRadius: 5
      }}
    >
      <Text variant={'bodySmall'} style={{ color }}>
        {text}
      </Text>
    </View>
  );
}
