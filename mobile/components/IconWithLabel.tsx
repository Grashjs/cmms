import { IconSource } from 'react-native-paper/src/components/Icon';
import { StyleSheet, View } from 'react-native';
import { IconButton, Text } from 'react-native-paper';
import * as React from 'react';

export function IconWithLabel({
                                icon,
                                label,
                                color
                              }: {
  icon: IconSource;
  label: string;
  color?: string;
}) {
  return (
    <View style={{ ...styles.row, justifyContent: 'flex-start' }}>
      <IconButton icon={icon} size={20} iconColor={color} />
      <Text style={{ color, flexShrink: 1 }} variant={'bodyMedium'}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  }
});
