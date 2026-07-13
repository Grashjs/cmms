import { ScrollView, StyleSheet } from 'react-native';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { Card, IconButton, Text, useTheme } from 'react-native-paper';
import { IconSource } from 'react-native-paper/src/components/Icon';

export default function SelectTasksOrChecklistModal({
                                                      navigation,
                                                      route
                                                    }: RootStackScreenProps<'SelectTasksOrChecklist'>) {
  const { onChange, selected } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const options: { label: string; screen: 'SelectTasks' | 'SelectChecklists', icon: IconSource }[] = [{
    label: t('task'),
    screen: 'SelectTasks',
    icon: 'checkbox-outline'
  }, {
    label: t('checklist'),
    icon: 'format-list-bulleted',
    screen: 'SelectChecklists'
  }];

  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {options.map(option => <Card style={{ margin: 30 }}
                                   onPress={() => navigation.navigate(option.screen, {
                                     onChange, selected
                                   })}>
        <Card.Content style={{ justifyContent: 'center', alignItems: 'center' }}>
          <IconButton icon={option.icon} iconColor={theme.colors.primary} size={50} />
          <Text style={{ fontSize: 50 }}>{option.label}</Text>
        </Card.Content>
      </Card>)}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
