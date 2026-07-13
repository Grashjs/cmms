import {
  Pressable, RefreshControl,
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
  Button, Checkbox,
  Divider,
  IconButton,
  List,
  RadioButton, Searchbar,
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
  const { loadingGet, checklists } = useSelector(state => state.checklists);
  const { t }: { t: any } = useTranslation();
  const [searchQuery, setSearchQuery] = useState<string>('');
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getChecklists());
  }, []);
  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
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
          />}
      >
        {checklists.filter(mini => mini.name.toLowerCase().includes(searchQuery.toLowerCase().trim())).map((checklist) => (
          <TouchableOpacity
            onPress={() => {
              onChange([
                ...selected,
                ...checklist.taskBases.map(taskBase => getTaskFromTaskBase(taskBase))
              ]);
              navigation.pop(2);
            }}
            key={checklist.id}
            style={{
              borderRadius: 5,
              padding: 15,
              backgroundColor: 'white',
              display: 'flex',
              flexDirection: 'row',
              elevation: 2,
              alignItems: 'center'
            }}
          >
            <Text style={{ flexShrink: 1 }} variant={'titleMedium'}>{checklist.name}</Text>
            <Divider />
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
