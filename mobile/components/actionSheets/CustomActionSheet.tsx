import { View } from 'react-native';
import { Divider, List, useTheme } from 'react-native-paper';
import * as React from 'react';
import { useRef } from 'react';
import { IconSource } from 'react-native-paper/lib/typescript/components/Icon';
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
  const theme = useTheme();

  return (
    <ActionSheet
      ref={actionSheetRef}
      containerStyle={{ backgroundColor: theme.colors.surface }}
    >
      <View
        style={{
          paddingHorizontal: 5,
          paddingVertical: 15,
          backgroundColor: theme.colors.surface
        }}
      >
        <Divider />
        <List.Section>
          {options
            .filter((option) => option.visible)
            .map((entity, index) => (
              <List.Item
                key={index}
                style={{
                  paddingHorizontal: 15,
                  backgroundColor: theme.colors.surface
                }}
                titleStyle={{
                  color: entity.color ?? theme.colors.onSurface
                }}
                title={entity.title}
                left={() => (
                  <List.Icon
                    icon={entity.icon}
                    color={entity.color ?? theme.colors.onSurface}
                  />
                )}
                onPress={async () => {
                  await actionSheetRef.current?.hide();
                  setTimeout(() => {
                    entity.onPress();
                  }, 250);
                }}
              />
            ))}
        </List.Section>
      </View>
    </ActionSheet>
  );
}
