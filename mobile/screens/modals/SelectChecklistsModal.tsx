import {
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { Fragment, useContext, useEffect, useState } from 'react';
import { AssetMiniDTO } from '../../models/asset';
import {
  Avatar,
  Button,
  Checkbox,
  Divider,
  IconButton,
  List,
  RadioButton,
  Searchbar,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { TaskType } from '../../models/tasks';
import { UserMiniDTO } from '../../models/user';
import { randomInt } from '../../utils/generators';
import { getTaskFromTaskBase } from '../../utils/formatters';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { getTaskTypes } from '../../utils/displayers';
import { MeterMiniDTO } from '../../models/meter';
import { getTeamsMini } from '../../slices/team';
import { getChecklists } from '../../slices/checklist';
import { useDispatch, useSelector } from '../../store';

export default function SelectChecklistsModal({
  navigation,
  route
}: RootStackScreenProps<'SelectChecklists'>) {
  const { onChange, selected } = route.params;
  const theme = useTheme();
  const { loadingGet, checklists } = useSelector((state) => state.checklists);
  const { t }: { t: any } = useTranslation();
  const [searchQuery, setSearchQuery] = useState<string>('');
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getChecklists());
  }, []);
  return (
    <View
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <Searchbar
        placeholder={t('search')}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <ScrollView
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={() => dispatch(getChecklists())}
          />
        }
      >
        {checklists
          .filter((mini) =>
            mini.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
          )
          .map((checklist) => (
            <TouchableOpacity
              onPress={() => {
                onChange([
                  ...selected,
                  ...checklist.taskBases.map((taskBase) =>
                    getTaskFromTaskBase(taskBase)
                  )
                ]);
                navigation.pop(2);
              }}
              key={checklist.id}
            >
              <View style={styles.card}>
                <View style={styles.cardRow}>
                  <Avatar.Icon
                    size={50}
                    icon="format-list-checks"
                    style={{ backgroundColor: theme.colors.primaryContainer }}
                  />
                  <View style={{ flex: 1 }}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text variant="titleMedium" style={styles.cardTitle}>
                          {checklist.name}
                        </Text>
                        <Text
                          variant={'bodySmall'}
                          style={{ color: 'grey' }}
                        >{`#${checklist.id}`}</Text>
                      </View>
                    </View>
                  </View>
                </View>
              </View>
            </TouchableOpacity>
          ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  card: {
    backgroundColor: 'white',
    marginBottom: 1,
    padding: 10
  },
  cardRow: {
    display: 'flex',
    flexDirection: 'row',
    gap: 6,
    alignItems: 'center'
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  cardTitle: {
    fontWeight: 'bold',
    flexShrink: 1
  }
});
