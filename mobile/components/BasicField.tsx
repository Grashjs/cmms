import { View } from './Themed';
import { Divider, Text, TouchableRipple } from 'react-native-paper';
import * as React from 'react';
import { Linking } from 'react-native';

export default function BasicField({
  label,
  value,
  isLink
}: {
  label: string;
  value: string | number;
  isLink?: boolean;
}) {
  if (!value) return null;

  const handlePress = () => {
    if (isLink) {
      const href = value.toString().startsWith('http')
        ? value.toString()
        : `https://${value}`;
      Linking.openURL(href).catch((err) =>
        console.error('Failed to open link:', err)
      );
    }
  };

  return (
    <View>
      <View
        style={{
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-between',
          padding: 20
        }}
      >
        <Text style={{ marginRight: 5 }}>{label}</Text>
        {isLink ? (
          <TouchableRipple onPress={handlePress}>
            <Text
              style={{ fontWeight: 'bold', flexShrink: 1, color: '#1976d2' }}
            >
              {value}
            </Text>
          </TouchableRipple>
        ) : (
          <Text style={{ fontWeight: 'bold', flexShrink: 1 }}>{value}</Text>
        )}
      </View>
      <Divider />
    </View>
  );
}
