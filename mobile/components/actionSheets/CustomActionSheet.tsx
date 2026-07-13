import { View } from 'react-native';
import { Divider, List } from 'react-native-paper';
import * as React from 'react';
import { useRef } from 'react';
import { IconSource } from 'react-native-paper/src/components/Icon';
import ActionSheet, { ActionSheetRef } from 'react-native-actions-sheet';

export interface CustomActionSheetOption {
  title: string;
  icon: IconSource;
  onPress: () => void;
  color?: string;
  visible: boolean;
}

interface CustomActionSheetProps {
  options: CustomActionSheetOption[];
}

export default function CustomActionSheet({ options }: CustomActionSheetProps) {
  const actionSheetRef = useRef<ActionSheetRef>(null);

  return (
    <ActionSheet ref={actionSheetRef}>
      <View style={{ paddingHorizontal: 5, paddingVertical: 15 }}>
        <Divider />
        <List.Section>
          {options
            .filter((option) => option.visible)
            .map((entity, index) => (
              <List.Item
                key={index}
                style={{ paddingHorizontal: 15 }}
                titleStyle={{ color: entity.color }}
                title={entity.title}
                left={() => (
                  <List.Icon icon={entity.icon} color={entity.color} />
                )}
                onPress={() => {
                  actionSheetRef.current.hide();
                  entity.onPress();
                }}
              />
            ))}
        </List.Section>
      </View>
    </ActionSheet>
  );
}
