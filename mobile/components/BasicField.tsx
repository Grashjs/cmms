import { View } from './Themed';
import { Divider, Text } from 'react-native-paper';
import * as React from 'react';

export default function BasicField({
                                     label,
                                     value
                                   }: {
  label: string;
  value: string | number;
}) {
  if (value)
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
          <Text
            style={{ fontWeight: 'bold', flexShrink: 1 }}>{value}</Text>

        </View>
        <Divider />
      </View>
    );
  else return null;
}